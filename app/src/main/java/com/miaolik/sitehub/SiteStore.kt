package com.miaolik.sitehub

import android.content.Context
import org.json.JSONArray

class SiteStore(context: Context) {
    private val prefs = context.getSharedPreferences("site_hub", Context.MODE_PRIVATE)

    fun sites(): List<Site> {
        val raw = prefs.getString("sites", "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { Site.fromJson(array.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun save(site: Site) {
        val updated = sites().filterNot { it.id == site.id } + site
        persist(updated.map { entry ->
            entry.copy(isDefault = if (site.isDefault) entry.id == site.id else entry.isDefault)
        })
    }

    fun delete(site: Site) = persist(sites().filterNot { it.id == site.id })

    fun defaultSite(): Site? = sites().firstOrNull { it.isDefault }

    private fun persist(sites: List<Site>) {
        val array = JSONArray()
        sites.forEach { array.put(it.toJson()) }
        prefs.edit().putString("sites", array.toString()).apply()
    }
}
