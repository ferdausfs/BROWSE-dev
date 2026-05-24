package com.ferdausfs.erudadevtools

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlBar: EditText
    private lateinit var backBtn: ImageButton
    private lateinit var forwardBtn: ImageButton
    private lateinit var refreshBtn: ImageButton
    private lateinit var statusBar: TextView
    private lateinit var progressBar: ProgressBar

    private val prefsName = "eruda_prefs"
    private val keyAutoInject = "auto_inject_eruda"
    private val keyDarkMode = "dark_mode_webview"
    private val keyHomepage = "homepage_url"
    private val defaultHomepage = "https://example.com"

    private val erudaInjectionScript = """
        (function() {
          if (window.__erudaInjected) return;
          window.__erudaInjected = true;
          var s = document.createElement('script');
          s.src = 'file:///android_asset/eruda.js';
          document.head.appendChild(s);
          s.onload = function() { eruda.init(); };
        })();
    """.trimIndent()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        webView = findViewById(R.id.webView)
        urlBar = findViewById(R.id.urlBar)
        backBtn = findViewById(R.id.backBtn)
        forwardBtn = findViewById(R.id.forwardBtn)
        refreshBtn = findViewById(R.id.refreshBtn)
        statusBar = findViewById(R.id.statusBar)
        progressBar = findViewById(R.id.progressBar)

        configureWebView()
        setupListeners()
        applyDarkModeIfEnabled()

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            val home = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                .getString(keyHomepage, defaultHomepage) ?: defaultHomepage
            loadUrl(home)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.databaseEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.setSupportZoom(true)
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        // Keep default Android UA — do not override

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                view?.loadUrl(url)
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                statusBar.text = getString(R.string.status_loading, url ?: "")
                urlBar.setText(url ?: "")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                statusBar.text = getString(R.string.status_loaded, url ?: "")
                urlBar.setText(url ?: "")
                updateNavButtons()
                injectErudaIfEnabled()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    Log.d(
                        "ErudaWebViewConsole",
                        "[${it.messageLevel()}] ${it.message()} -- ${it.sourceId()}:${it.lineNumber()}"
                    )
                }
                return true
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                } else {
                    progressBar.visibility = View.GONE
                }
            }

            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: android.webkit.JsResult?
            ): Boolean {
                Log.d("ErudaWebViewJSAlert", "JS Alert: $message")
                Toast.makeText(this@MainActivity, message ?: "", Toast.LENGTH_SHORT).show()
                result?.confirm()
                return true
            }
        }
    }

    private fun setupListeners() {
        urlBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                loadUrl(urlBar.text.toString())
                hideKeyboard()
                true
            } else {
                false
            }
        }

        backBtn.setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
        }
        forwardBtn.setOnClickListener {
            if (webView.canGoForward()) webView.goForward()
        }
        refreshBtn.setOnClickListener {
            webView.reload()
        }
    }

    private fun updateNavButtons() {
        backBtn.isEnabled = webView.canGoBack()
        backBtn.alpha = if (webView.canGoBack()) 1f else 0.4f
        forwardBtn.isEnabled = webView.canGoForward()
        forwardBtn.alpha = if (webView.canGoForward()) 1f else 0.4f
    }

    private fun loadUrl(raw: String) {
        var url = raw.trim()
        if (url.isEmpty()) return
        if (!url.startsWith("http://", ignoreCase = true) &&
            !url.startsWith("https://", ignoreCase = true) &&
            !url.startsWith("file://", ignoreCase = true) &&
            !url.startsWith("about:", ignoreCase = true)
        ) {
            // Smart bar: if contains dot and no spaces, treat as URL; else search Google
            url = if (url.contains(".") && !url.contains(" ")) {
                "https://$url"
            } else {
                "https://www.google.com/search?q=" + Uri.encode(url)
            }
        }
        webView.loadUrl(url)
    }

    private fun injectErudaIfEnabled() {
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val autoInject = prefs.getBoolean(keyAutoInject, true)
        if (!autoInject) return

        webView.evaluateJavascript(erudaInjectionScript, null)
        // Fallback: also try loadUrl javascript: scheme
        webView.post {
            try {
                webView.loadUrl("javascript:$erudaInjectionScript")
            } catch (e: Exception) {
                Log.w("ErudaInject", "Fallback injection failed: ${e.message}")
            }
        }
    }

    private fun applyDarkModeIfEnabled() {
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val dark = prefs.getBoolean(keyDarkMode, false)
        if (dark) {
            webView.setBackgroundColor(Color.parseColor("#121212"))
        } else {
            webView.setBackgroundColor(Color.WHITE)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(urlBar.windowToken, 0)
        urlBar.clearFocus()
    }

    override fun onResume() {
        super.onResume()
        applyDarkModeIfEnabled()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_clear_cache -> {
                webView.clearCache(true)
                Toast.makeText(this, R.string.toast_cache_cleared, Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_reload -> {
                webView.reload()
                true
            }
            R.id.action_copy_url -> {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("url", webView.url ?: ""))
                Toast.makeText(this, R.string.toast_url_copied, Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_open_in_browser -> {
                val current = webView.url
                if (!current.isNullOrEmpty()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(current))
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, R.string.toast_no_browser, Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
            R.id.action_inject_eruda -> {
                webView.evaluateJavascript(erudaInjectionScript, null)
                Toast.makeText(this, R.string.toast_eruda_injected, Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
