package com.miaolik.sitehub

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
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
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.backButton)
            .setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val switchSiteButton = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.switchSiteButton)
        val refreshButton = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.refreshButton)
        val clearCacheButton = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.clearCacheButton)
        val clearLoginButton = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.clearLoginButton)
        fun toggleDrawer() {
            val visibility = if (switchSiteButton.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            listOf(switchSiteButton, refreshButton, clearCacheButton, clearLoginButton).forEach { it.visibility = visibility }
        }
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.menuButton)
            .setOnClickListener { toggleDrawer() }
        switchSiteButton
            .setOnClickListener { showSitePicker() }
        refreshButton
            .setOnClickListener { webView.reload() }
        clearCacheButton
            .setOnClickListener { webView.clearCache(true); webView.clearHistory(); webView.reload() }
        clearLoginButton
            .setOnClickListener {
                CookieManager.getInstance().removeAllCookies { webView.reload() }
                CookieManager.getInstance().flush()
            }

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

    private fun showSitePicker() {
        val sites = SiteStore(this).sitesSorted()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("切换网站")
            .setItems(sites.map { if (it.id == site.id) "当前：${it.name}" else it.name }.toTypedArray()) { _, which ->
                if (sites[which].id != site.id) {
                    startActivity(Intent(this, WebActivity::class.java).putExtra(EXTRA_SITE_ID, sites[which].id))
                    finish()
                }
            }
            .setNegativeButton("关闭", null)
            .show()
    }
}
