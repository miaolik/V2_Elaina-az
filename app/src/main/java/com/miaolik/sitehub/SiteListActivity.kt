package com.miaolik.sitehub

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText

class SiteListActivity : AppCompatActivity() {
    private lateinit var store: SiteStore
    private lateinit var adapter: SiteAdapter
    private var openedDefault = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_site_list)
        openedDefault = savedInstanceState?.getBoolean("opened_default", false) ?: false
        store = SiteStore(this)
        adapter = SiteAdapter(store.sitesSorted(), ::openSite, ::showSiteDialog)
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.siteList).apply {
            layoutManager = LinearLayoutManager(this@SiteListActivity)
            adapter = this@SiteListActivity.adapter
        }
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setOnMenuItemClickListener { false }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.addButton)
            .setOnClickListener { showSiteDialog(null) }

        if (!openedDefault && intent.getBooleanExtra("open_default", true)) {
            openedDefault = true
            store.defaultSite()?.let(::openSite)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("opened_default", openedDefault)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        adapter.submit(store.sitesSorted())
    }

    private fun openSite(site: Site) {
        startActivity(Intent(this, WebActivity::class.java).putExtra(WebActivity.EXTRA_SITE_ID, site.id))
    }

    private fun showSiteDialog(existing: Site?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_site, null)
        val name = view.findViewById<TextInputEditText>(R.id.siteName)
        val scheme = view.findViewById<TextInputEditText>(R.id.scheme)
        val host = view.findViewById<TextInputEditText>(R.id.host)
        val port = view.findViewById<TextInputEditText>(R.id.port)
        val path = view.findViewById<TextInputEditText>(R.id.path)
        val defaultSite = view.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.defaultSite)
        existing?.let {
            name.setText(it.name); scheme.setText(it.scheme); host.setText(it.host)
            port.setText(it.port); path.setText(it.path); defaultSite.isChecked = it.isDefault
        }
        val dialog = AlertDialog.Builder(this).setTitle(if (existing == null) "添加网站" else "编辑网站")
            .setView(view).setNegativeButton("取消", null).setPositiveButton("保存", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (name.text.isNullOrBlank() || host.text.isNullOrBlank()) {
                    host.error = "请填写名称和 IP 或域名"
                    return@setOnClickListener
                }
                store.save(Site(existing?.id ?: java.util.UUID.randomUUID().toString(), name.text.toString().trim(), scheme.text.toString().trim(), host.text.toString().trim(), port.text.toString().trim(), path.text.toString().trim(), defaultSite.isChecked))
                adapter.submit(store.sites())
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}
