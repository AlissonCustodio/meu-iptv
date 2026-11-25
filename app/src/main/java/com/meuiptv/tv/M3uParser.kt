package com.meuiptv.tv

/**
 * Lê playlists M3U/M3U8 no formato usado pelo iptv-org e pelo Free-TV:
 *
 *   #EXTINF:-1 tvg-id="..." tvg-logo="..." group-title="News",Nome do Canal
 *   #EXTVLCOPT:http-user-agent=...
 *   #EXTVLCOPT:http-referrer=...
 *   https://.../stream.m3u8
 */
object M3uParser {
    private val attrRegex = Regex("""([\w-]+)="([^"]*)"""")

    fun parse(text: String): List<Channel> {
        val out = ArrayList<Channel>()
        var info: String? = null
        var ua: String? = null
        var ref: String? = null

        for (raw in text.lineSequence()) {
            val line = raw.trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    info = line; ua = null; ref = null
                }
                line.startsWith("#EXTVLCOPT:http-user-agent=", ignoreCase = true) ->
                    ua = line.substringAfter("=").trim()
                line.startsWith("#EXTVLCOPT:http-referrer=", ignoreCase = true) ->
                    ref = line.substringAfter("=").trim()
                line.startsWith("#") -> Unit
                else -> {
                    val inf = info ?: continue
                    val attrs = attrRegex.findAll(inf)
                        .associate { it.groupValues[1].lowercase() to it.groupValues[2] }

                    // O nome vem depois da vírgula que segue o último atributo.
                    val lastQuote = inf.lastIndexOf('"')
                    val comma = inf.indexOf(',', if (lastQuote >= 0) lastQuote else 0)
                    val name = (if (comma >= 0) inf.substring(comma + 1).trim() else "")
                        .ifBlank { attrs["tvg-name"].orEmpty() }
                        .ifBlank { "Sem nome" }

                    val group = attrs["group-title"].orEmpty()
                        .split(';').first().trim()
                        .let { if (it.isBlank() || it.equals("undefined", true)) "Outros" else it }

                    out += Channel(
                        name = name,
                        url = line,
                        logo = attrs["tvg-logo"]?.ifBlank { null },
                        group = group,
                        userAgent = ua ?: attrs["user-agent"]?.ifBlank { null },
                        referrer = ref ?: attrs["http-referrer"]?.ifBlank { null },
                    )
                    info = null; ua = null; ref = null
                }
            }
        }
        return out
    }
}
