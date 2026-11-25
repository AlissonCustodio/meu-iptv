package com.meuiptv.tv

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    /**
     * Carrega todas as playlists de uma fonte em paralelo, junta, remove repetidos e aplica os filtros.
     * Se uma das playlists falhar, usa as outras; só dá erro se todas falharem.
     */
    suspend fun loadSource(
        source: PlaylistSource,
        forceRefresh: Boolean,
        hideGeoBlocked: Boolean,
    ): List<Channel> = coroutineScope {
        val results = source.feeds
            .map { feed -> async { runCatching { loadFeed(feed, forceRefresh) } } }
            .awaitAll()

        if (results.isNotEmpty() && results.all { it.isFailure }) {
            throw results.first().exceptionOrNull() ?: IOException("Falha ao carregar")
        }

        val seen = HashSet<String>()
        val nameFilter = source.nameFilter
        results.flatMap { it.getOrDefault(emptyList()) }
            .filter { seen.add(it.url) }
            .filter { nameFilter == null || nameFilter.containsMatchIn(it.name) }
            .filter { !hideGeoBlocked || !it.name.contains("geo-blocked", ignoreCase = true) }
    }

    private suspend fun loadFeed(feed: Feed, forceRefresh: Boolean): List<Channel> {
        val text = fetch(feed.url, forceRefresh)
        return M3uParser.parse(text)
            .asSequence()
            .filterNot { it.tags.contains("xxx", ignoreCase = true) }
            .filterNot { isYoutube(it.url) } // o player não reproduz páginas do YouTube
            .filter { feed.onlyGroups == null || feed.onlyGroups.containsMatchIn(it.tags) }
            .map { if (feed.relabel != null) it.copy(group = feed.relabel) else it }
            .toList()
    }

    private fun isYoutube(url: String) =
        url.contains("youtube.com", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true)

    /** Baixa a playlist ou usa o cache de até 6 h. Sem internet, usa o cache antigo. */
    private suspend fun fetch(url: String, forceRefresh: Boolean): String =
        withContext(Dispatchers.IO) {
            val cache = File(context.cacheDir, "pl_" + Integer.toHexString(url.hashCode()) + ".m3u")
            val fresh = cache.exists() &&
                System.currentTimeMillis() - cache.lastModified() < CACHE_MS
            if (fresh && !forceRefresh) {
                cache.readText()
            } else {
                try {
                    download(url).also { cache.writeText(it) }
                } catch (e: Exception) {
                    if (cache.exists()) cache.readText() else throw e
                }
            }
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
