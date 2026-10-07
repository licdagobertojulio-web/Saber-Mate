/**
 * SABER MATE · Servidor de resultados en Google Sheets
 * ----------------------------------------------------
 * Recibe en tiempo real las respuestas de los estudiantes que usan la app
 * (APK o versión sin internet) y las guarda en esta hoja de cálculo.
 * También entrega el resumen que ve el docente en el "Panel docente" de la app.
 *
 * 1. Cambia el PIN de abajo por uno que solo tú conozcas.
 * 2. Ejecuta una vez la función "configurar" (botón ▶ Ejecutar) y acepta los permisos.
 * 3. Implementar → Nueva implementación → Aplicación web
 *    · Ejecutar como: Yo   · Quién tiene acceso: Cualquier usuario
 * 4. Copia la URL que termina en /exec y pégala en el archivo config.js de la app.
 */
const PIN_DOCENTE = '2026';          // ← CAMBIA ESTE PIN
const MAX_FILAS_RESUMEN = 6000;      // respuestas recientes que se analizan para el panel

const HOJAS = {
  Respuestas: ['Fecha y hora', 'ID estudiante', 'Nombre', 'Grupo', 'Modo', 'Código pregunta', 'Tema', 'Pensamiento', 'Competencia', 'Resultado', 'ID evento'],
  Rondas: ['Fecha y hora', 'ID estudiante', 'Nombre', 'Grupo', 'Tipo', 'Aciertos', 'Total', 'Porcentaje', 'Tiempo (min)', 'ID evento'],
  Estudiantes: ['ID estudiante', 'Nombre', 'Grupo', 'Primera conexión', 'Última actividad', 'Respondidas', 'Correctas', '% aciertos', 'Rondas', 'Simulacros', 'Mejor simulacro (%)', 'Último simulacro'],
};

function configurar() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  Object.keys(HOJAS).forEach(n => hoja_(ss, n));
  const sobra = ss.getSheetByName('Hoja 1') || ss.getSheetByName('Sheet1');
  if (sobra && ss.getSheets().length > 1) ss.deleteSheet(sobra);
  SpreadsheetApp.getUi && Logger.log('Hojas listas. Ahora implementa el proyecto como aplicación web.');
}

function hoja_(ss, nombre) {
  let h = ss.getSheetByName(nombre);
  if (!h) {
    h = ss.insertSheet(nombre);
    const enc = HOJAS[nombre];
    h.getRange(1, 1, 1, enc.length).setValues([enc]).setFontWeight('bold').setBackground('#2445C2').setFontColor('#FFFFFF');
    h.setFrozenRows(1);
    h.autoResizeColumns(1, enc.length);
  }
  return h;
}

const json_ = o => ContentService.createTextOutput(JSON.stringify(o)).setMimeType(ContentService.MimeType.JSON);

// ---------- Recepción de eventos desde la app ----------
function doPost(e) {
  const lock = LockService.getScriptLock();
  try {
    lock.waitLock(25000);
    const datos = JSON.parse(e.postData.contents || '{}');
    const eventos = Array.isArray(datos.eventos) ? datos.eventos.slice(0, 200) : [];
    if (!eventos.length) return json_({ ok: true, recibidos: 0 });
    // evita duplicados si la app reintenta un envío
    const cache = CacheService.getScriptCache();
    const vistos = cache.getAll(eventos.map(ev => 'e' + ev.id));
    const nuevos = eventos.filter(ev => ev && ev.id && !vistos['e' + ev.id]);
    const ss = SpreadsheetApp.getActiveSpreadsheet();
    const zona = ss.getSpreadsheetTimeZone();
    const fecha = t => Utilities.formatDate(new Date(t || Date.now()), zona, 'yyyy-MM-dd HH:mm:ss');
    const resp = [], rondas = [], porEst = {};
    nuevos.forEach(ev => {
      const est = ev.estudiante || {};
      const id = String(est.id || 'sin-id').slice(0, 60), nombre = limpia_(est.nombre || 'Sin nombre'), grupo = limpia_(est.grupo || '');
      const a = porEst[id] = porEst[id] || { id, nombre, grupo, t: 0, resp: 0, ok: 0, rondas: 0, sims: 0, mejor: null, ultimoSim: '' };
      a.nombre = nombre; a.grupo = grupo; a.t = Math.max(a.t, ev.t || 0);
      if (ev.tipo === 'respuesta') {
        resp.push([fecha(ev.t), id, nombre, grupo, limpia_(ev.modo), ev.codigo, limpia_(ev.tema), limpia_(ev.pensamiento), limpia_(ev.competencia), limpia_(ev.resultado), ev.id]);
        if (ev.resultado !== 'Sin responder') { a.resp++; if (ev.resultado === 'Correcta') a.ok++; }
      } else if (ev.tipo === 'ronda' || ev.tipo === 'simulacro') {
        const pct = ev.total ? Math.round(ev.aciertos * 100 / ev.total) : 0;
        rondas.push([fecha(ev.t), id, nombre, grupo, ev.tipo === 'ronda' ? 'Ronda de práctica' : 'Simulacro', ev.aciertos, ev.total, pct, ev.segundos ? Math.round(ev.segundos / 6) / 10 : '', ev.id]);
        if (ev.tipo === 'ronda') a.rondas++; else { a.sims++; a.mejor = Math.max(a.mejor || 0, pct); a.ultimoSim = ev.aciertos + '/' + ev.total; }
      }
    });
    if (resp.length) { const h = hoja_(ss, 'Respuestas'); h.getRange(h.getLastRow() + 1, 1, resp.length, resp[0].length).setValues(resp); }
    if (rondas.length) { const h = hoja_(ss, 'Rondas'); h.getRange(h.getLastRow() + 1, 1, rondas.length, rondas[0].length).setValues(rondas); }
    actualizaEstudiantes_(ss, porEst, fecha);
    const marcas = {}; nuevos.forEach(ev => marcas['e' + ev.id] = '1');
    if (nuevos.length) cache.putAll(marcas, 21600);
    return json_({ ok: true, recibidos: eventos.length, nuevos: nuevos.length });
  } catch (err) {
    return json_({ ok: false, error: String(err) });
  } finally {
    try { lock.releaseLock(); } catch (e) { }
  }
}

function limpia_(s) { s = String(s == null ? '' : s).slice(0, 120); return /^[=+\-@]/.test(s) ? "'" + s : s; }

function actualizaEstudiantes_(ss, porEst, fecha) {
  const ids = Object.keys(porEst); if (!ids.length) return;
  const h = hoja_(ss, 'Estudiantes');
  const n = h.getLastRow() - 1;
  const datos = n > 0 ? h.getRange(2, 1, n, 12).getValues() : [];
  const fila = {}; datos.forEach((r, i) => fila[r[0]] = i);
  ids.forEach(id => {
    const a = porEst[id], ahora = fecha(a.t);
    let r = fila[id] != null ? datos[fila[id]] : null;
    if (!r) { r = [id, a.nombre, a.grupo, ahora, ahora, 0, 0, 0, 0, 0, '', '']; fila[id] = datos.length; datos.push(r); }
    r[1] = a.nombre; r[2] = a.grupo; r[4] = ahora;
    r[5] = Number(r[5] || 0) + a.resp; r[6] = Number(r[6] || 0) + a.ok;
    r[7] = r[5] ? Math.round(r[6] * 100 / r[5]) : 0;
    r[8] = Number(r[8] || 0) + a.rondas; r[9] = Number(r[9] || 0) + a.sims;
    if (a.mejor != null) r[10] = Math.max(Number(r[10] || 0), a.mejor);
    if (a.ultimoSim) r[11] = a.ultimoSim;
  });
  if (datos.length) h.getRange(2, 1, datos.length, 12).setValues(datos);
}

// ---------- Resumen para el panel docente ----------
function doGet(e) {
  const p = (e && e.parameter) || {};
  if (p.accion !== 'resumen') return json_({ ok: true, app: 'SABER MATE' });
  if (String(p.pin || '') !== String(PIN_DOCENTE)) return json_({ ok: false, error: 'pin' });
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const he = hoja_(ss, 'Estudiantes'), hr = hoja_(ss, 'Respuestas');
  const ne = he.getLastRow() - 1, nr = hr.getLastRow() - 1;
  const est = ne > 0 ? he.getRange(2, 1, ne, 12).getDisplayValues() : [];
  const desde = Math.max(2, hr.getLastRow() - MAX_FILAS_RESUMEN + 1);
  const resp = nr > 0 ? hr.getRange(desde, 1, hr.getLastRow() - desde + 1, 10).getDisplayValues() : [];
  const temas = {};
  resp.forEach(r => { if (r[9] === 'Sin responder') return; const t = temas[r[6]] = temas[r[6]] || { tema: r[6], pensamiento: r[7], intentos: 0, errores: 0 }; t.intentos++; if (r[9] !== 'Correcta') t.errores++; });
  return json_({
    ok: true,
    actualizado: Utilities.formatDate(new Date(), ss.getSpreadsheetTimeZone(), 'HH:mm:ss'),
    totalRespuestas: Math.max(0, nr),
    estudiantes: est.map(r => ({ id: r[0], nombre: r[1], grupo: r[2], primera: r[3], ultima: r[4], respondidas: +r[5] || 0, correctas: +r[6] || 0, pct: +r[7] || 0, rondas: +r[8] || 0, simulacros: +r[9] || 0, mejor: r[10] === '' ? null : +r[10], ultimoSim: r[11] })),
    recientes: resp.slice(-40).reverse().map(r => ({ hora: r[0], nombre: r[2], grupo: r[3], modo: r[4], codigo: r[5], tema: r[6], resultado: r[9] })),
    temas: Object.values(temas).filter(t => t.intentos >= 2).map(t => ({ ...t, pctError: Math.round(t.errores * 100 / t.intentos) })).sort((a, b) => b.pctError - a.pctError || b.intentos - a.intentos).slice(0, 12),
  });
}
