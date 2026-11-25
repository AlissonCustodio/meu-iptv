package com.meuiptv.tv

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var repo: PlaylistRepository
    private lateinit var favorites: Favorites
    private lateinit var customSources: CustomSources
    private lateinit var settings: Settings

    private lateinit var sourceAdapter: TextAdapter
    private lateinit var groupAdapter: TextAdapter
    private lateinit var channelAdapter: ChannelAdapter

    private lateinit var status: TextView
    private lateinit var titleView: TextView
    private lateinit var progress: ProgressBar
    private lateinit var search: EditText
    private lateinit var channelList: RecyclerView

    private var sources: List<PlaylistSource> = emptyList()
    private var currentSource = -1
    private var allChannels: List<Channel> = emptyList()
    private var groups: List<String> = emptyList()
    private var currentGroup: String? = null
    private var visible: List<Channel> = emptyList()
    private var loadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repo = PlaylistRepository(this)
        favorites = Favorites(this)
        customSources = CustomSources(this)
        settings = Settings(this)

        status = findViewById(R.id.status)
        titleView = findViewById(R.id.title)
        progress = findViewById(R.id.progress)
        search = findViewById(R.id.search)
        channelList = findViewById(R.id.channelList)

        sourceAdapter = TextAdapter(::onSourceClick, ::onSourceLongClick)
        findViewById<RecyclerView>(R.id.sourceList).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sourceAdapter
        }

        groupAdapter = TextAdapter({ selectGroup(it) })
        findViewById<RecyclerView>(R.id.groupList).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }

        channelAdapter = ChannelAdapter(::openChannel, ::toggleFavorite)
        channelList.layoutManager = GridLayoutManager(this, 4)
        channelList.adapter = channelAdapter

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) = applyFilter()
        })

        rebuildSources()
        onSourceClick(1) // abre "Brasil" ao iniciar
    }

    override fun onResume() {
        super.onResume()
        channelAdapter.favoriteUrls = favorites.urls()
        channelAdapter.notifyDataSetChanged()
    }

    // ---------- Listas (fontes) ----------

    private fun rebuildSources() {
        val geo = getString(if (settings.hideGeoBlocked) R.string.geo_on else R.string.geo_off)
        sources = listOf(PlaylistSource("⭐ Favoritos", special = Special.FAVORITES)) +
            Sources.defaults +
            customSources.all() +
            PlaylistSource("➕ Adicionar lista M3U", special = Special.ADD) +
            PlaylistSource(geo, special = Special.TOGGLE_GEO)
        sourceAdapter.items = sources.map { it.title }
        sourceAdapter.selected = currentSource
    }

    private fun onSourceClick(index: Int) {
        val src = sources[index]
        when (src.special) {
            Special.ADD -> { showAddDialog(); return }
            Special.TOGGLE_GEO -> {
                settings.hideGeoBlocked = !settings.hideGeoBlocked
                rebuildSources()
                // Recarrega a lista aberta já com o novo filtro (usa o cache, é rápido).
                sources.getOrNull(currentSource)?.let { if (it.feeds.isNotEmpty()) load(it, force = false) }
                return
            }
            else -> Unit
        }
        currentSource = index
        sourceAdapter.selected = index
        titleView.text = src.title
        search.setText("")

        loadJob?.cancel()
        if (src.special == Special.FAVORITES) {
            setChannels(favorites.all())
            if (allChannels.isEmpty()) {
                status.text = getString(R.string.no_favorites)
            }
            return
        }
        load(src, force = false)
    }

    /** Clique longo: em lista própria = remover; nas demais = recarregar da internet. */
    private fun onSourceLongClick(index: Int): Boolean {
        val src = sources[index]
        when {
            src.custom -> AlertDialog.Builder(this)
                .setTitle(getString(R.string.remove_list_q, src.title))
                .setPositiveButton(R.string.remove) { _, _ ->
                    customSources.remove(src.feeds.first().url)
                    if (currentSource == index) currentSource = -1
                    rebuildSources()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            src.feeds.isNotEmpty() -> {
                currentSource = index
                sourceAdapter.selected = index
                titleView.text = src.title
                load(src, force = true)
            }
            else -> return false
        }
        return true
    }

    private fun load(src: PlaylistSource, force: Boolean) {
        loadJob?.cancel()
        progress.visibility = View.VISIBLE
        status.text = getString(R.string.loading)
        setChannels(emptyList(), updateStatus = false)
        loadJob = lifecycleScope.launch {
            try {
                val list = repo.loadSource(src, force, settings.hideGeoBlocked)
                setChannels(list)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status.text = getString(R.string.load_error, e.message ?: e.javaClass.simpleName)
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun showAddDialog() {
        val pad = (16 * resources.displayMetrics.density).toInt()
        val nameInput = EditText(this).apply { hint = getString(R.string.list_name) }
        val urlInput = EditText(this).apply {
            hint = "https://.../lista.m3u"
            inputType = InputType.TYPE_TEXT_VARIATION_URI
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, 0)
            addView(nameInput)
            addView(urlInput)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.add_list)
            .setView(box)
            .setPositiveButton(R.string.save) { _, _ ->
                val url = urlInput.text.toString().trim()
                if (!url.startsWith("http")) {
                    Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                val name = nameInput.text.toString().trim().ifBlank { "Minha lista" }
                customSources.add("📁 $name", url)
                rebuildSources()
                onSourceClick(sources.indexOfFirst { it.custom && it.feeds.first().url == url })
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ---------- Grupos e canais ----------

    private fun setChannels(list: List<Channel>, updateStatus: Boolean = true) {
        allChannels = list
        val counts = list.groupingBy { it.group }.eachCount()
        groups = counts.keys.sortedWith(compareBy({ it == "Outros" }, { it.lowercase() }))
        groupAdapter.items = listOf("Todos (${list.size})") + groups.map { "$it (${counts[it]})" }
        currentGroup = null
        groupAdapter.selected = 0
        applyFilter(updateStatus)
    }

    private fun selectGroup(index: Int) {
        currentGroup = if (index == 0) null else groups[index - 1]
        groupAdapter.selected = index
        applyFilter()
        channelList.scrollToPosition(0)
    }

    private fun applyFilter(updateStatus: Boolean = true) {
        val q = search.text.toString().trim()
        visible = allChannels.filter {
            (currentGroup == null || it.group == currentGroup) &&
                (q.isEmpty() || it.name.contains(q, ignoreCase = true))
        }
        channelAdapter.favoriteUrls = favorites.urls()
        channelAdapter.items = visible
        if (updateStatus) status.text = getString(R.string.channel_count, visible.size)
    }

    private fun openChannel(position: Int) {
        ChannelStore.playing = visible
        startActivity(
            Intent(this, PlayerActivity::class.java)
                .putExtra(PlayerActivity.EXTRA_INDEX, position)
        )
    }

    private fun toggleFavorite(position: Int) {
        val ch = visible[position]
        val added = favorites.toggle(ch)
        Toast.makeText(
            this,
            getString(if (added) R.string.fav_added else R.string.fav_removed, ch.name),
            Toast.LENGTH_SHORT
        ).show()
        if (sources.getOrNull(currentSource)?.special == Special.FAVORITES) {
            setChannels(favorites.all())
        } else {
            channelAdapter.favoriteUrls = favorites.urls()
            channelAdapter.notifyItemChanged(position)
        }
    }
}
