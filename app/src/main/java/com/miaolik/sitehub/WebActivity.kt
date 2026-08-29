package com.miaolik.sitehub

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
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
    private data class PickerItem(val title: String, val detail: String, val selected: Boolean)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)
        site = SiteStore(this).sites().firstOrNull { it.id == intent.getStringExtra(EXTRA_SITE_ID) } ?: run {
            finish(); return
        }
        val backButton = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.backButton)
        backButton
            .setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val switchSiteButton = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.switchSiteButton)
        val windowButton = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.windowButton)
        val refreshButton = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.refreshButton)
        val clearCacheButton = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.clearCacheButton)
        val clearLoginButton = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.clearLoginButton)
        fun toggleDrawer() {
            val visibility = if (switchSiteButton.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            listOf(backButton, windowButton, switchSiteButton, refreshButton, clearCacheButton, clearLoginButton).forEach { it.visibility = visibility }
        }
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.menuButton)
            .setOnClickListener { toggleDrawer() }
        switchSiteButton
            .setOnClickListener { showSitePicker() }
        windowButton
            .setOnClickListener { showWindowPicker() }
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
        applyWindowSafeAreas()
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

    private fun applyWindowSafeAreas() {
        val actionDrawer = findViewById<View>(R.id.actionDrawer)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()).top
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout()).bottom
            (actionDrawer.layoutParams as ViewGroup.MarginLayoutParams).apply {
                bottomMargin = bottomInset + dp(8)
                actionDrawer.layoutParams = this
            }
            (windowTabScroller.layoutParams as ViewGroup.MarginLayoutParams).apply {
                topMargin = topInset + dp(64)
                windowTabScroller.layoutParams = this
            }
            insets
        }
        ViewCompat.requestApplyInsets(actionDrawer)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWindow(url: String?, initialTitle: String): BrowserWindow {
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
                val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
                val child = createWindow(null, "新窗口")
                transport.webView = child.webView
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
        url?.let(view::loadUrl)
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
            tab.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val params = LinearLayout.LayoutParams(156, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = 8 }
            windowTabs.addView(tab, params)
        }
        val activeIndex = windows.indexOf(activeWindow)
        if (activeIndex >= 0) {
            windowTabs.post { windowTabScroller.smoothScrollTo(activeIndex * 164, 0) }
        }
        windowTabScroller.visibility = View.VISIBLE
    }

    private fun showWindowPicker() {
        showPicker(
            title = "切换窗口",
            subtitle = "选择要继续浏览的页面",
            items = windows.mapIndexed { index, window ->
                PickerItem("窗口 ${index + 1}", window.title, window == activeWindow)
            },
        ) { index -> switchTo(windows[index]) }
    }

    override fun onDestroy() {
        windows.forEach { it.webView.destroy() }
        windows.clear()
        super.onDestroy()
    }

    private fun showSitePicker() {
        val sites = SiteStore(this).sitesSorted()
        showPicker(
            title = "切换网站",
            subtitle = "选择一个站点并在新窗口中打开",
            items = sites.map { candidate ->
                PickerItem(candidate.name, candidate.url(), candidate.id == site.id)
            },
        ) { index ->
            val selectedSite = sites[index]
            if (selectedSite.id != site.id) {
                site = selectedSite
                createWindow(site.url(), site.name)
            }
        }
    }

    private fun showPicker(
        title: String,
        subtitle: String,
        items: List<PickerItem>,
        onSelect: (Int) -> Unit,
    ) {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(16))
            background = roundedBackground("#151A23", "#303B4D", 22)
        }
        val heading = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 20f
        }
        val description = TextView(this).apply {
            text = subtitle
            setTextColor(Color.parseColor("#9EACBF"))
            textSize = 13f
            setPadding(0, dp(4), 0, dp(16))
        }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        lateinit var dialog: androidx.appcompat.app.AlertDialog
        content.addView(heading)
        content.addView(description)
        items.forEachIndexed { index, item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
                background = roundedBackground(
                    if (item.selected) "#29364A" else "#202836",
                    if (item.selected) "#65799B" else "#303B4D",
                    14,
                )
                isClickable = true
                isFocusable = true
            }
            val rowTitle = TextView(this).apply {
                text = if (item.selected) "${item.title}  当前" else item.title
                setTextColor(Color.WHITE)
                textSize = 16f
                maxLines = 1
            }
            val rowDetail = TextView(this).apply {
                text = item.detail
                setTextColor(Color.parseColor("#AAB7C9"))
                textSize = 12f
                maxLines = 1
                setPadding(0, dp(4), 0, 0)
            }
            row.addView(rowTitle)
            row.addView(rowDetail)
            list.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                if (index > 0) topMargin = dp(8)
            })
            row.setOnClickListener {
                dialog.dismiss()
                onSelect(index)
            }
        }
        content.addView(list)
        dialog = androidx.appcompat.app.AlertDialog.Builder(this).setView(content).create()
        dialog.setOnShowListener {
            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                decorView.setPadding(dp(16), 0, dp(16), 0)
            }
        }
        dialog.show()
    }

    private fun roundedBackground(fillColor: String, strokeColor: String, radius: Int) = GradientDrawable().apply {
        setColor(Color.parseColor(fillColor))
        setStroke(dp(1), Color.parseColor(strokeColor))
        cornerRadius = dp(radius).toFloat()
    }
}
