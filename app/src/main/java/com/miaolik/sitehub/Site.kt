package com.miaolik.sitehub

import org.json.JSONObject
import java.util.UUID

data class Site(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val scheme: String = "http://",
    val host: String,
    val port: String = "5200",
    val path: String = "/web/",
    val isDefault: Boolean = false,
) {
    fun url(): String {
        val normalizedScheme = scheme.ifBlank { "http://" }.let {
            if (it.endsWith("://")) it else "$it://"
        }
        val normalizedPath = path.trim().let {
            when {
                it.isBlank() -> "/"
                it.startsWith('/') -> it
                else -> "/$it"
            }
        }
        val portPart = port.trim().takeIf { it.isNotEmpty() }?.let { ":$it" } ?: ""
        return "$normalizedScheme${host.trim()}$portPart$normalizedPath"
    }

    fun toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("scheme", scheme)
        put("host", host)
        put("port", port)
        put("path", path)
        put("isDefault", isDefault)
    }

    companion object {
        fun fromJson(json: JSONObject) = Site(
            id = json.optString("id", UUID.randomUUID().toString()),
            name = json.optString("name"),
            scheme = json.optString("scheme", "http://"),
            host = json.optString("host"),
            port = json.optString("port", "5200"),
            path = json.optString("path", "/web/"),
            isDefault = json.optBoolean("isDefault", false),
        )
    }
}
