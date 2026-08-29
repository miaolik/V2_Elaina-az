package com.miaolik.sitehub

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class WebActivity : AppCompatActivity() {
    companion object { const val EXTRA_SITE_ID = "site_id" }
    private lateinit var webView: WebView
    private lateinit var site: Site

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)
        site = SiteStore(this).sites().firstOrNull { it.id == intent.getStringExtra(EXTRA_SITE_ID) } ?: run {
            finish(); return
        }
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.webToolbar)
        setSupportActionBar(toolbar)
        toolbar.title = site.name
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.navigationContentDescription = "返回"
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        CookieManager.getInstance().setAcceptCookie(true)
        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true
        webView.settings.setSupportZoom(true)
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                findViewById<android.widget.ProgressBar>(R.id.progress).progress = newProgress
            }
        }
        webView.loadUrl(site.url())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add("刷新").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add("清除登录状态")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.title) {
        "刷新" -> { webView.reload(); true }
        "清除登录状态" -> {
            CookieManager.getInstance().removeAllCookies { webView.reload() }
            CookieManager.getInstance().flush()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
