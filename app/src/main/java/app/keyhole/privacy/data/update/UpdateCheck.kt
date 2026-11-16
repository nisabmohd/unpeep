package app.keyhole.privacy.data.update

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** What GitHub said about the latest release. */
sealed interface UpdateResult {
    data class Latest(val version: String) : UpdateResult
    data class Newer(val version: String, val page: String) : UpdateResult
    data object Failed : UpdateResult
}

/**
 * The github build's update check (CLAUDE.md › Hard rules: "github also asks GitHub for updates, nothing else"):
 * one GET of the latest release's tag, only when the user taps "Check for updates". It sends no data of the user's,
 * only the request itself; play and foss never call it (Play and F-Droid update those).
 */
class UpdateCheck(private val repo: String, private val current: String, private val fetch: (String) -> String? = ::get) {
    suspend fun check(): UpdateResult = withContext(Dispatchers.IO) {
        val api = "https://api.github.com/repos/" + repo.removePrefix("https://github.com/").trim('/') + "/releases/latest"
        val body = fetch(api) ?: return@withContext UpdateResult.Failed
        val release = runCatching { json.decodeFromString<Release>(body) }.getOrNull() ?: return@withContext UpdateResult.Failed
        val latest = release.tag.removePrefix("v")
        if (isNewer(latest, current)) UpdateResult.Newer(latest, release.page) else UpdateResult.Latest(plain(current))
    }

    @Serializable
    private data class Release(@kotlinx.serialization.SerialName("tag_name") val tag: String, @kotlinx.serialization.SerialName("html_url") val page: String)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** "0.1.0-github" → "0.1.0". */
        fun plain(version: String): String = version.substringBefore('-').removePrefix("v")

        /** True when [latest] is a higher version than [current], number by number ("0.10.0" > "0.9.3"). */
        fun isNewer(latest: String, current: String): Boolean {
            val a = plain(latest).split('.').map { it.toIntOrNull() ?: 0 }
            val b = plain(current).split('.').map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }

        private fun get(address: String): String? = try {
            (URL(address).openConnection() as HttpURLConnection).run {
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                setRequestProperty("Accept", "application/vnd.github+json")
                // A generic name: nothing about the user or the phone.
                setRequestProperty("User-Agent", "Keyhole")
                try {
                    if (responseCode == HttpURLConnection.HTTP_OK) inputStream.bufferedReader().use { it.readText().take(MAX_BODY) } else null
                } finally {
                    disconnect()
                }
            }
        } catch (e: IOException) {
            null
        }

        private const val TIMEOUT = 10_000
        private const val MAX_BODY = 256 * 1024
    }
}
