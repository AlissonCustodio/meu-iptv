package com.meuiptv.tv

import org.json.JSONObject

data class Channel(
    val name: String,
    val url: String,
    val logo: String?,
    val group: String,
    val userAgent: String? = null,
    val referrer: String? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("url", url)
        put("logo", logo ?: "")
        put("group", group)
        put("ua", userAgent ?: "")
        put("ref", referrer ?: "")
    }

    companion object {
        fun fromJson(o: JSONObject) = Channel(
            name = o.optString("name"),
            url = o.optString("url"),
            logo = o.optString("logo").ifBlank { null },
            group = o.optString("group", "Outros"),
            userAgent = o.optString("ua").ifBlank { null },
            referrer = o.optString("ref").ifBlank { null },
        )
    }
}

data class PlaylistSource(
    val title: String,
    val url: String,
    val custom: Boolean = false,
)

object Sources {
    const val FAVORITES = "fav://"
    const val ADD = "add://"
    private const val IPTV_ORG = "https://iptv-org.github.io/iptv"

    val defaults = listOf(
        PlaylistSource("🇧🇷 Brasil", "$IPTV_ORG/countries/br.m3u"),
        PlaylistSource("🇵🇹 Portugal", "$IPTV_ORG/countries/pt.m3u"),
        PlaylistSource("🗣 Em português", "$IPTV_ORG/languages/por.m3u"),
        PlaylistSource("🌍 Free-TV (mundo)", "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8"),
        PlaylistSource("📰 Notícias", "$IPTV_ORG/categories/news.m3u"),
        PlaylistSource("⚽ Esportes", "$IPTV_ORG/categories/sports.m3u"),
        PlaylistSource("🎬 Filmes", "$IPTV_ORG/categories/movies.m3u"),
        PlaylistSource("📺 Séries", "$IPTV_ORG/categories/series.m3u"),
        PlaylistSource("🧸 Infantil", "$IPTV_ORG/categories/kids.m3u"),
        PlaylistSource("🎵 Música", "$IPTV_ORG/categories/music.m3u"),
        PlaylistSource("🔬 Documentários", "$IPTV_ORG/categories/documentary.m3u"),
        PlaylistSource("🌐 Todos (iptv-org)", "$IPTV_ORG/index.m3u"),
    )
}

/** Lista que o player percorre ao trocar de canal (evita passar listas enormes por Intent). */
object ChannelStore {
    var playing: List<Channel> = emptyList()
}
