package com.meuiptv.tv

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

const val DEFAULT_UA =
    "Mozilla/5.0 (Linux; Android 10; Android TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"

class PlaylistRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** Baixa (ou usa o cache de até 6 h) e devolve os canais. Em caso de falha de rede, usa o cache antigo. */
    suspend fun load(url: String, forceRefresh: Boolean = false): List<Channel> =
        withContext(Dispatchers.IO) {
            val cache = File(context.cacheDir, "pl_" + Integer.toHexString(url.hashCode()) + ".m3u")
            val fresh = cache.exists() &&
                System.currentTimeMillis() - cache.lastModified() < CACHE_MS

            val text = if (fresh && !forceRefresh) {
                cache.readText()
            } else {
                try {
                    download(url).also { cache.writeText(it) }
                } catch (e: Exception) {
                    if (cache.exists()) cache.readText() else throw e
                }
            }
            M3uParser.parse(text).filterNot { it.group.equals("XXX", ignoreCase = true) }
        }

    private fun download(url: String): String {
        val request = Request.Builder().url(url).header("User-Agent", DEFAULT_UA).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            return response.body?.string() ?: throw IOException("Resposta vazia")
        }
    }

    companion object {
        private const val CACHE_MS = 6L * 60 * 60 * 1000
    }
}
