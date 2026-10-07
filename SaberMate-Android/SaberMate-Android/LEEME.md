# SABER MATE · App Android

Aplicación de estudio de Matemáticas Saber 11.º con 500 preguntas. Funciona **con o sin internet**. Cuando hay conexión, cada respuesta, ronda y simulacro llega en tiempo real a una hoja de Google del docente y al **Panel docente** de la app. Sin conexión, los resultados se guardan en el teléfono y se envían solos cuando vuelve el internet.

## Paso 0: recibir resultados en tiempo real (hazlo antes de compilar)

1. Entra a sheets.google.com con tu cuenta de Google y crea una hoja nueva. Llámala, por ejemplo, **SABER MATE · Resultados**.
2. En la hoja, abre **Extensiones → Apps Script**.
3. Borra lo que aparece y pega todo el contenido del archivo `servidor-google/Code.gs`.
4. En la línea `const PIN_DOCENTE = '2026';` cambia `2026` por un PIN que solo tú conozcas. Lo pedirá el Panel docente.
5. Guarda (ícono de disquete). Arriba, elige la función **configurar** y pulsa **▶ Ejecutar**. Google pedirá permisos: elige tu cuenta → *Configuración avanzada* → *Ir a SABER MATE (no seguro)* → **Permitir**. Es tu propio script, por eso Google muestra esa advertencia.
6. Pulsa **Implementar → Nueva implementación**. En el engranaje elige **Aplicación web**:
   - *Ejecutar como:* **Yo**
   - *Quién tiene acceso:* **Cualquier usuario**

   Pulsa **Implementar** y copia la **URL de la aplicación web** (termina en `/exec`).
7. Abre `app/src/main/assets/config.js` y pega la URL entre las comillas, en lugar de `PEGA_AQUI_LA_URL_DE_TU_SCRIPT`. Si subes el proyecto a GitHub, puedes editar el archivo ahí mismo (ícono del lápiz).

Listo: cuando compiles el APK, la app enviará los resultados a tu hoja. En la hoja verás tres pestañas:
- **Respuestas:** cada pregunta respondida, con nombre, grupo, tema, pensamiento, competencia y resultado.
- **Rondas:** los resultados de cada ronda y de cada simulacro.
- **Estudiantes:** el resumen por estudiante (respondidas, % de aciertos, simulacros, última actividad), actualizado solo.

**Panel docente en el celular:** instala la misma app, ve a Inicio → **Soy docente** (abajo) y escribe tu PIN. Verás en vivo a tus estudiantes, la actividad reciente y los temas con más errores. Se actualiza cada 15 segundos.

> Si más adelante cambias el código del script, usa **Implementar → Gestionar implementaciones → Editar → Nueva versión**, para que la URL no cambie.

## Opción 1: obtener el APK con GitHub, sin instalar nada

1. Crea una cuenta gratuita en github.com y un repositorio nuevo, por ejemplo `sabermate`.
2. En el repositorio, elige **Add file → Upload files** y sube **todo el contenido** de esta carpeta, incluida la carpeta oculta `.github`. Si tu computador no muestra esa carpeta, sube el ZIP completo y descomprímelo en GitHub, o usa GitHub Desktop.
3. Abre la pestaña **Actions**. El proceso "Construir APK de SABER MATE" empieza solo y tarda unos 5 minutos. Si no empieza, ábrelo y pulsa **Run workflow**.
4. Cuando aparezca la marca verde, entra al proceso y descarga **SaberMate-apk** en la sección *Artifacts*. Dentro del ZIP está `app-debug.apk`.

## Opción 2: Android Studio

1. Instala Android Studio (gratuito) y elige **File → Open** sobre esta carpeta.
2. Espera a que termine la sincronización de Gradle.
3. Elige **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. El archivo queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Lectura en voz alta y accesibilidad

La app lee en voz alta las preguntas, las opciones y la retroalimentación con la voz del teléfono (motor de texto a voz de Android). Si no suena:
- Revisa en el teléfono **Ajustes → Accesibilidad → Salida de texto a voz** que el motor sea *Google* y que esté instalado el idioma **español**.
- Sube el volumen multimedia.

En la app, el botón de **Accesibilidad** (arriba) permite activar la lectura automática, cambiar la velocidad de la voz, agrandar la letra y ampliar el espaciado.

## Instalar en los teléfonos

Envía `app-debug.apk` por WhatsApp, correo o USB. Al abrirlo, Android pide permitir la instalación de *orígenes desconocidos* para esa aplicación (WhatsApp, Archivos, etc.). Acepta y pulsa **Instalar**. Funciona en Android 5.0 o superior.

## Datos de la app

- Nombre: SABER MATE
- Paquete: `co.sabermate.app`
- Versión: 1.0
- Contenido: `app/src/main/assets/index.html`, `config.js` y la carpeta `fig/`. Para actualizar las preguntas, se reemplazan esos archivos y se vuelve a compilar.
- Permisos: solo internet, para enviar los resultados al docente. La lectura en voz alta usa el motor de voz del teléfono.
