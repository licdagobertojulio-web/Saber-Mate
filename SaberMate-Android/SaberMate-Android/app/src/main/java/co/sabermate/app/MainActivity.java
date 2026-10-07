package co.sabermate.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Locale;

/**
 * SABER MATE: muestra la aplicación de estudio (assets/index.html), envía los resultados
 * al docente cuando hay internet y lee en voz alta las preguntas con la voz del teléfono.
 */
public class MainActivity extends Activity {
    private WebView web;
    private TextToSpeech tts;
    private boolean ttsListo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tts = new TextToSpeech(this, estado -> {
            if (estado == TextToSpeech.SUCCESS) {
                int r = tts.setLanguage(new Locale("es", "CO"));
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) tts.setLanguage(new Locale("es"));
                ttsListo = true;
            }
        });

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#F3F5FA"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);      // guarda el progreso (localStorage)
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true); // permite enviar resultados al servidor de Google
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setTextZoom(100);
        web.addJavascriptInterface(new LectorVoz(), "AndroidTTS");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return abrirFuera(request.getUrl());
            }
            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return abrirFuera(Uri.parse(url));
            }
        });
        setContentView(web);
        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl("file:///android_asset/index.html");
    }

    /** Puente para que la página use la voz del teléfono (lectura en voz alta). */
    private class LectorVoz {
        @JavascriptInterface
        public void speak(String texto, float velocidad) {
            if (!ttsListo || texto == null) return;
            tts.setSpeechRate(velocidad <= 0 ? 1f : velocidad);
            tts.stop();
            // se divide en frases cortas para no superar el límite del motor de voz
            String[] partes = texto.split("(?<=[.!?:;])\\s+");
            StringBuilder bloque = new StringBuilder();
            int n = 0;
            for (String p : partes) {
                if (bloque.length() + p.length() > 350) {
                    tts.speak(bloque.toString(), n == 0 ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, null, "sm" + n++);
                    bloque.setLength(0);
                }
                bloque.append(p).append(' ');
            }
            if (bloque.length() > 0) tts.speak(bloque.toString(), n == 0 ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, null, "sm" + n);
        }

        @JavascriptInterface
        public void stop() {
            if (tts != null) tts.stop();
        }

        @JavascriptInterface
        public boolean isSpeaking() {
            return tts != null && tts.isSpeaking();
        }
    }

    /** Los enlaces a internet se abren en el navegador del teléfono. */
    private boolean abrirFuera(Uri uri) {
        String esquema = uri.getScheme();
        if ("http".equals(esquema) || "https".equals(esquema)) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) { }
            return true;
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tts != null) tts.stop();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
