package ru.landilf.ollink.domain.model

import java.net.URI

/** A server root, without API paths, credentials or URL parameters. */
object ServerAddress {
    fun normalize(value: String): String {
        val raw = value.trim().trimEnd('/')
        val uri = try { URI(raw) } catch (_: Exception) {
            throw IllegalArgumentException("Некорректный адрес сервера")
        }
        require(uri.scheme in listOf("http", "https") && !uri.host.isNullOrBlank() &&
            uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null &&
            uri.rawPath.isNullOrEmpty() && (uri.port == -1 || uri.port in 1..65535)) {
            "Введите адрес вида http://192.168.1.10:11434 без пути и параметров"
        }
        return raw
    }
}

