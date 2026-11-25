package com.meuiptv.tv

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Favorites(context: Context) {
    private val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    fun all(): List<Channel> = runCatching {
        val arr = JSONArray(prefs.getString("list", "[]"))
        (0 until arr.length()).map { Channel.fromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun urls(): Set<String> = all().map { it.url }.toSet()

    /** Retorna true se o canal passou a ser favorito. */
    fun toggle(channel: Channel): Boolean {
        val list = all().toMutableList()
        val removed = list.removeAll { it.url == channel.url }
        if (!removed) list.add(channel)
        val arr = JSONArray().apply { list.forEach { put(it.toJson()) } }
        prefs.edit().putString("list", arr.toString()).apply()
        return !removed
    }
}

class CustomSources(context: Context) {
    private val prefs = context.getSharedPreferences("sources", Context.MODE_PRIVATE)

    fun all(): List<PlaylistSource> = runCatching {
        val arr = JSONArray(prefs.getString("list", "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            PlaylistSource(o.getString("title"), o.getString("url"), custom = true)
        }
    }.getOrDefault(emptyList())

    fun add(title: String, url: String) = save(all() + PlaylistSource(title, url, true))

    fun remove(url: String) = save(all().filterNot { it.url == url })

    private fun save(list: List<PlaylistSource>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("title", it.title).put("url", it.url)) }
        prefs.edit().putString("list", arr.toString()).apply()
    }
}
