package com.meuiptv.tv

import org.json.JSONObject

data class Channel(
    val name: String,
    val url: String,
    val logo: String?,
    val group: String,
    val userAgent: String? = null,
    val referrer: String? = null,
    /** group-title completo da lista (ex.: "News;Sports"), usado nos filtros. */
    val tags: String = "",
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

/**
 * Uma playlist M3U de onde vêm canais.
 * @param onlyGroups se definido, mantém só canais cujo group-title combine com este filtro.
 * @param relabel se definido, troca a categoria dos canais desta playlist por este nome.
 */
data class Feed(
    val url: String,
    val onlyGroups: Regex? = null,
    val relabel: String? = null,
)

enum class Special { FAVORITES, ADD, TOGGLE_GEO }

/** Um item da coluna "listas": junta uma ou mais playlists e, opcionalmente, filtra pelo nome do canal. */
data class PlaylistSource(
    val title: String,
    val feeds: List<Feed> = emptyList(),
    val custom: Boolean = false,
    val nameFilter: Regex? = null,
    val special: Special? = null,
)

object Sources {
    private const val ORG = "https://iptv-org.github.io/iptv"
    private const val FREE_TV = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8"
    private val IC = setOf(RegexOption.IGNORE_CASE)

    private fun org(path: String, onlyGroups: Regex? = null) = Feed("$ORG/$path", onlyGroups)

    /** Canais de um país dentro da lista Free-TV (ela agrupa por país). */
    private fun freeTv(country: String) =
        Feed(FREE_TV, Regex("^($country)$", IC), relabel = "TV aberta (Free-TV)")

    private val SPORTS = Regex("sports", IC)

    /** Palavras que indicam canal de futebol (ou canal esportivo focado em futebol). */
    private val FOOTBALL = Regex(
        listOf(
            "futebol", "football", "soccer", "f[uú]tbol", "\\bgol\\b", "\\bgoal", "golazo",
            "fifa", "uefa", "conmebol", "premier", "champions", "libertadores", "brasileir",
            "\\bcopa\\b", "\\bliga\\b", "laliga", "bundesliga", "serie a\\b", "\\bfc\\b",
            "flamengo", "palmeiras", "corinthians", "botafogo", "vasco", "fluminense",
            "gr[eê]mio", "cruzeiro", "benfica", "sporting", "real madrid", "bar[cç]a",
            "espn", "bein", "dazn", "sportv", "tnt sports", "sky sports", "fox sports",
            "band ?sports", "tudn", "eleven", "onefootball", "fanatiz",
        ).joinToString("|"),
        IC,
    )

    val defaults = listOf(
        PlaylistSource(
            "🇧🇷 Brasil (todos)",
            listOf(org("countries/br.m3u"), freeTv("brazil|brasil")),
        ),
        PlaylistSource(
            "⚽ Futebol",
            listOf(
                org("categories/sports.m3u"),
                org("countries/br.m3u"), org("countries/uk.m3u"),
                org("countries/us.m3u"), org("countries/pt.m3u"),
                Feed(FREE_TV),
            ),
            nameFilter = FOOTBALL,
        ),
        PlaylistSource(
            "🏆 Esportes BR · UK · EUA",
            listOf(
                org("countries/br.m3u", SPORTS),
                org("countries/uk.m3u", SPORTS),
                org("countries/us.m3u", SPORTS),
            ),
        ),
        PlaylistSource("🌍 Esportes (mundo)", listOf(org("categories/sports.m3u"))),
        PlaylistSource(
            "🇬🇧 Reino Unido",
            listOf(org("countries/uk.m3u"), freeTv("uk|united kingdom")),
        ),
        PlaylistSource(
            "🇺🇸 Estados Unidos",
            listOf(org("countries/us.m3u"), freeTv("usa|united states")),
        ),
        PlaylistSource(
            "🇵🇹 Portugal",
            listOf(org("countries/pt.m3u"), freeTv("portugal")),
        ),
        PlaylistSource("📰 Notícias", listOf(org("categories/news.m3u"))),
        PlaylistSource("🎬 Filmes", listOf(org("categories/movies.m3u"))),
        PlaylistSource("🧸 Infantil", listOf(org("categories/kids.m3u"))),
        PlaylistSource("🎵 Música", listOf(org("categories/music.m3u"))),
        PlaylistSource("🌍 Free-TV (mundo)", listOf(Feed(FREE_TV))),
        PlaylistSource("🌐 Todos (iptv-org)", listOf(org("index.m3u"))),
    )
}

/** Lista que o player percorre ao trocar de canal (evita passar listas enormes por Intent). */
object ChannelStore {
    var playing: List<Channel> = emptyList()
}
