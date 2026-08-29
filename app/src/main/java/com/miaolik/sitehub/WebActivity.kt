package com.miaolik.sitehub

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView

class WebActivity : AppCompatActivity() {
    companion object { const val EXTRA_SITE_ID = "site_id" }
    private lateinit var site: Site
    private lateinit var webViewContainer: FrameLayout
    private lateinit var windowTabs: LinearLayout
    private lateinit var windowTabScroller: HorizontalScrollView
    private lateinit var progress: android.widget.ProgressBar
    private val windows = mutableListOf<BrowserWindow>()
    private var activeWindow: BrowserWindow? = null

    private data class BrowserWindow(val webView: WebView, var title: String)

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
            .setOnClickListener { activeWindow?.webView?.reload() }
        clearCacheButton
            .setOnClickListener { activeWindow?.webView?.let { it.clearCache(true); it.clearHistory(); it.reload() } }
        clearLoginButton
            .setOnClickListener {
                CookieManager.getInstance().removeAllCookies { activeWindow?.webView?.reload() }
                CookieManager.getInstance().flush()
            }

        CookieManager.getInstance().setAcceptCookie(true)
        webViewContainer = findViewById(R.id.webViewContainer)
        windowTabs = findViewById(R.id.windowTabs)
        windowTabScroller = findViewById(R.id.windowTabScroller)
        progress = findViewById(R.id.progress)
        createWindow(site.url(), site.name)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activeWindow?.webView?.let { view ->
                    if (view.canGoBack()) view.goBack() else if (windows.size > 1) closeWindow(activeWindow!!)
                    else finish()
                } ?: finish()
            }
        })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWindow(url: String, initialTitle: String): BrowserWindow {
        val view = WebView(this)
        view.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        view.settings.databaseEnabled = true
        view.settings.setSupportZoom(true)
        view.settings.javaScriptCanOpenWindowsAutomatically = true
        view.settings.setSupportMultipleWindows(true)
        val window = BrowserWindow(view, initialTitle)
        view.webViewClient = WebViewClient()
        view.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(webView: WebView, newProgress: Int) {
                if (activeWindow?.webView == webView) progress.progress = newProgress
            }

            override fun onReceivedTitle(webView: WebView, title: String?) {
                if (!title.isNullOrBlank()) {
                    window.title = title
                    renderTabs()
                }
            }

            override fun onCreateWindow(webView: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message): Boolean {
                val child = createWindow("about:blank", "新窗口")
                (resultMsg.obj as WebView.WebViewTransport).webView = child.webView
                resultMsg.sendToTarget()
                return true
            }

            override fun onCloseWindow(webView: WebView) {
                windows.firstOrNull { it.webView == webView }?.let(::closeWindow)
            }
        }
        windows += window
        webViewContainer.addView(view)
        switchTo(window)
        view.loadUrl(url)
        return window
    }

    private fun switchTo(window: BrowserWindow) {
        activeWindow = window
        windows.forEach { it.webView.visibility = if (it == window) View.VISIBLE else View.GONE }
        progress.progress = window.webView.progress
        renderTabs()
    }

    private fun closeWindow(window: BrowserWindow) {
        if (windows.size == 1) {
            finish()
            return
        }
        val index = windows.indexOf(window)
        windows.remove(window)
        webViewContainer.removeView(window.webView)
        window.webView.destroy()
        switchTo(windows[(index - 1).coerceAtLeast(0)])
    }

    private fun renderTabs() {
        windowTabs.removeAllViews()
        windows.forEachIndexed { index, window ->
            val tab = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 6, 0)
                background = getDrawable(if (window == activeWindow) R.drawable.bg_window_tab_active else R.drawable.bg_window_tab)
                setOnClickListener { switchTo(window) }
            }
            val label = TextView(this).apply {
                text = "${index + 1}. ${window.title.take(16)}"
                setTextColor(Color.rgb(35, 51, 72))
                textSize = 12f
                maxLines = 1
            }
            val close = ImageButton(this).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                background = getDrawable(R.drawable.bg_window_close)
                contentDescription = "关闭窗口"
                setPadding(5, 5, 5, 5)
                setOnClickListener { closeWindow(window) }
            }
            tab.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            tab.addView(close, LinearLayout.LayoutParams(28, 28))
            val params = LinearLayout.LayoutParams(156, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = 8 }
            windowTabs.addView(tab, params)
        }
        val activeIndex = windows.indexOf(activeWindow)
        if (activeIndex >= 0) {
            windowTabs.post { windowTabScroller.smoothScrollTo(activeIndex * 164, 0) }
        }
        windowTabScroller.visibility = if (windows.size > 1) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        windows.forEach { it.webView.destroy() }
        windows.clear()
        super.onDestroy()
    }

    private fun showSitePicker() {
        val sites = SiteStore(this).sitesSorted()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("切换网站")
            .setItems(sites.map { if (it.id == site.id) "当前：${it.name}" else it.name }.toTypedArray()) { _, which ->
                if (sites[which].id != site.id) {
                    site = sites[which]
                    createWindow(site.url(), site.name)
                }
            }
            .setNegativeButton("关闭", null)
            .show()
    }
}
