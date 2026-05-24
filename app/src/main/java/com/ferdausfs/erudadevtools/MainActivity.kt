package com.ferdausfs.erudadevtools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView

    private var erudaJs: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        webView = findViewById(R.id.webView)
        urlBar = findViewById(R.id.urlBar)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)

        // Load eruda.js from assets
        try {
            erudaJs = assets.open("eruda.js").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setupWebView()
        setupUrlBar()
        setupNavButtons()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val homepage = prefs.getString("homepage", "https://example.com") ?: "https://example.com"
        loadUrl(homepage)
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    view.loadUrl(url)
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = View.VISIBLE
                urlBar.setText(url)
                statusText.text = getString(R.string.loading)
            }

            override fun onPageFinished(view: WebView, url: String) {
                progressBar.visibility = View.GONE
                urlBar.setText(url)
                statusText.text = url
                injectEruda(view)
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    statusText.text = getString(R.string.error_loading)
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                android.util.Log.d("ErudaConsole",
                    "[${consoleMessage.messageLevel()}] ${consoleMessage.message()} " +
                    "(${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})")
                return true
            }

            override fun onJsAlert(view: WebView, url: String, message: String,
                                   result: JsResult): Boolean {
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setMessage(message)
                    .setPositiveButton("OK") { _, _ -> result.confirm() }
                    .setCancelable(false)
                    .show()
                return true
            }
        }
    }

    private fun injectEruda(view: WebView) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val autoInject = prefs.getBoolean("auto_inject_eruda", true)
        if (!autoInject || erudaJs.isEmpty()) return

        val script = """
            (function() {
                if (window.__erudaInjected) return;
                window.__erudaInjected = true;
                try {
                    $erudaJs
                    eruda.init();
                } catch(e) {
                    console.error('Eruda injection failed: ' + e.message);
                }
            })();
        """.trimIndent()

        view.evaluateJavascript(script, null)
    }

    private fun setupUrlBar() {
        urlBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val input = urlBar.text.toString().trim()
                loadUrl(input)
                hideKeyboard()
                true
            } else false
        }
    }

    private fun setupNavButtons() {
        findViewById<View>(R.id.btnBack).setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
        }
        findViewById<View>(R.id.btnForward).setOnClickListener {
            if (webView.canGoForward()) webView.goForward()
        }
        findViewById<View>(R.id.btnRefresh).setOnClickListener {
            webView.reload()
        }
    }

    private fun loadUrl(input: String) {
        val url = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> "https://www.google.com/search?q=${Uri.encode(input)}"
        }
        webView.loadUrl(url)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(urlBar.windowToken, 0)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
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
                webView.clearHistory()
                Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_reload -> {
                webView.reload()
                true
            }
            R.id.action_copy_url -> {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("URL", webView.url))
                Toast.makeText(this, R.string.url_copied, Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_open_browser -> {
                webView.url?.let {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                }
                true
            }
            R.id.action_inject_eruda -> {
                injectEruda(webView)
                Toast.makeText(this, R.string.eruda_injected, Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
