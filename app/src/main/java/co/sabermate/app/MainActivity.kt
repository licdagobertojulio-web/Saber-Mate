package co.sabermate.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import co.sabermate.app.tts.TtsManager
import co.sabermate.app.ui.theme.BluePrimary
import co.sabermate.app.ui.theme.SaberMateTheme

class MainActivity : ComponentActivity() {
    private lateinit var ttsManager: TtsManager
    private var webViewInstance: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ttsManager = TtsManager(this)

        setContent {
            SaberMateTheme {
                SaberMateMainScreen(
                    ttsManager = ttsManager,
                    onWebViewCreated = { webViewInstance = it },
                    getWebView = { webViewInstance }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::ttsManager.isInitialized) {
            ttsManager.stop()
        }
    }

    override fun onDestroy() {
        if (::ttsManager.isInitialized) {
            ttsManager.shutdown()
        }
        webViewInstance?.destroy()
        webViewInstance = null
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaberMateMainScreen(
    ttsManager: TtsManager,
    onWebViewCreated: (WebView) -> Unit,
    getWebView: () -> WebView?
) {
    val isSpeaking by ttsManager.isSpeakingFlow.collectAsState()
    var showInfoDialog by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }

    // Intercept back button if webview can go back
    BackHandler(enabled = canGoBack) {
        val wv = getWebView()
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Text(
                            text = "SABER MATE",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { getWebView()?.reload() },
                        modifier = Modifier.testTag("reload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recargar contenido",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.testTag("info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Información Saber Mate",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("top_app_bar")
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isSpeaking,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { ttsManager.stop() },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("stop_tts_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeOff,
                        contentDescription = "Detener lectura en voz alta"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SaberMateWebView(
                ttsManager = ttsManager,
                onWebViewCreated = onWebViewCreated,
                onBackStateChanged = { canGoBack = it }
            )
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text(text = "SABER MATE · Saber 11.º", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Aplicación completa de preparación y entrenamiento para la prueba de Matemáticas Saber 11.º con 500 preguntas oficiales e inéditas, retroalimentación paso a paso, simulacros cronometrados, lectura en voz alta y sincronización docente."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showInfoDialog = false },
                    modifier = Modifier.testTag("close_info_dialog")
                ) {
                    Text("Entendido")
                }
            }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SaberMateWebView(
    ttsManager: TtsManager,
    onWebViewCreated: (WebView) -> Unit,
    onBackStateChanged: (Boolean) -> Unit
) {
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .testTag("saber_mate_webview"),
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(Color.parseColor("#F3F5FA"))
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    mediaPlaybackRequiresUserGesture = false
                    textZoom = 100
                    cacheMode = WebSettings.LOAD_DEFAULT
                }

                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun speak(texto: String?, velocidad: Float) {
                            ttsManager.speak(texto, velocidad)
                        }

                        @JavascriptInterface
                        fun stop() {
                            ttsManager.stop()
                        }

                        @JavascriptInterface
                        fun isSpeaking(): Boolean {
                            return ttsManager.isSpeaking()
                        }
                    },
                    "AndroidTTS"
                )

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        return handleExternalUri(request.url)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                        return handleExternalUri(Uri.parse(url))
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onBackStateChanged(view?.canGoBack() == true)
                    }

                    private fun handleExternalUri(uri: Uri): Boolean {
                        val scheme = uri.scheme
                        if ("http" == scheme || "https" == scheme) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                ctx.startActivity(intent)
                            } catch (_: Exception) { }
                            return true
                        }
                        return false
                    }
                }

                loadUrl("file:///android_asset/index.html")
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            onBackStateChanged(webView.canGoBack())
        }
    )
}
