package com.meuiptv.tv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_INDEX = "index"
    }

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var info: TextView
    private lateinit var error: TextView
    private lateinit var loading: ProgressBar

    private val channels get() = ChannelStore.playing
    private var index = 0
    private var triedAlternate = false

    private val handler = Handler(Looper.getMainLooper())
    private val hideInfo = Runnable { info.visibility = View.GONE }

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            loading.visibility = if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
        }

        override fun onPlayerError(e: PlaybackException) {
            if (!triedAlternate) {
                // Tenta de novo com outro formato (HLS <-> direto) antes de desistir.
                play(index, alternate = true)
                return
            }
            loading.visibility = View.GONE
            error.text = getString(R.string.channel_error, e.errorCodeName)
            error.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        if (channels.isEmpty()) {
            finish(); return
        }
        playerView = findViewById(R.id.playerView)
        info = findViewById(R.id.info)
        error = findViewById(R.id.error)
        loading = findViewById(R.id.loading)
        index = savedInstanceState?.getInt(EXTRA_INDEX)
            ?: intent.getIntExtra(EXTRA_INDEX, 0)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onStart() {
        super.onStart()
        if (channels.isEmpty()) return
        player = ExoPlayer.Builder(this).build().also {
            it.addListener(listener)
            it.playWhenReady = true
            playerView.player = it
        }
        play(index)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(hideInfo)
        player?.release()
        player = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_INDEX, index)
    }

    private fun play(i: Int, alternate: Boolean = false) {
        val p = player ?: return
        index = ((i % channels.size) + channels.size) % channels.size
        triedAlternate = alternate
        val ch = channels[index]
        error.visibility = View.GONE
        if (!alternate) showInfo()

        val headers = buildMap {
            ch.referrer?.let {
                put("Referer", it)
                put("Origin", it.trimEnd('/'))
            }
        }
        val dataSource = DefaultHttpDataSource.Factory()
            .setUserAgent(ch.userAgent ?: DEFAULT_UA)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setDefaultRequestProperties(headers)

        val item = MediaItem.Builder()
            .setUri(ch.url)
            .apply { guessMime(ch.url, alternate)?.let { setMimeType(it) } }
            .build()

        p.setMediaSource(DefaultMediaSourceFactory(dataSource).createMediaSource(item))
        p.prepare()
        p.play()
    }

    /** Descobre o tipo do stream pela URL. Links sem extensão normalmente são HLS. */
    private fun guessMime(url: String, alternate: Boolean): String? {
        val path = url.substringBefore('?').lowercase()
        val primary = when {
            path.endsWith(".mpd") -> MimeTypes.APPLICATION_MPD
            path.endsWith(".m3u8") || path.endsWith(".m3u") -> MimeTypes.APPLICATION_M3U8
            listOf(".mp4", ".mkv", ".webm", ".mp3", ".aac", ".ts", ".flv").any { path.endsWith(it) } -> null
            else -> MimeTypes.APPLICATION_M3U8
        }
        if (!alternate) return primary
        return if (primary == MimeTypes.APPLICATION_M3U8) null else MimeTypes.APPLICATION_M3U8
    }

    private fun showInfo() {
        val ch = channels[index]
        info.text = getString(R.string.channel_info, index + 1, channels.size, ch.name, ch.group)
        info.visibility = View.VISIBLE
        handler.removeCallbacks(hideInfo)
        handler.postDelayed(hideInfo, 4000)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> play(index + 1)
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> play(index - 1)
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_INFO -> showInfo()
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> player?.let { if (it.isPlaying) it.pause() else it.play() }
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }
}
