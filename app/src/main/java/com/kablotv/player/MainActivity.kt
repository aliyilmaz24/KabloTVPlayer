package com.kablotv.player

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var playerView: PlayerView
    private lateinit var osd: TextView
    private lateinit var numberBox: TextView
    private lateinit var channelPanel: View
    private lateinit var categoryTitle: TextView
    private lateinit var channelList: RecyclerView

    private val handler = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null

    // HTTP isteklerine kanal başına Referer/Origin eklemek için kullanılan kaynak fabrikası
    private lateinit var httpFactory: DefaultHttpDataSource.Factory

    private var currentIndex = 0
    private var listOpen = false
    private var category = Category.ALL
    private val digits = StringBuilder()

    // Mevcut kanal için denenecek adresler: 1. ana, 2. yedek, 3. iptv-org güncel link
    private var candidates: List<String> = emptyList()
    private var attempt = 0

    private val adapter = ChannelAdapter { chooseFromList(it) }

    private val hideOsd = Runnable { osd.visibility = View.GONE }
    private val commitDigits = Runnable { commitNumber() }
    private val switchChannel = Runnable { startPlayback() }
    private val retryPlay = Runnable { startPlayback() }

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            attempt++
            if (attempt < candidates.size) {
                showOsd("Kaynak açılamadı, yedek kaynak deneniyor…")
                handler.removeCallbacks(retryPlay)
                handler.postDelayed(retryPlay, 1500)
            } else {
                showOsd("Bu kanal şu an açılamıyor")
            }
        }
    }

    // ---------------------------------------------------------------- Yaşam döngüsü

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PlaylistUpdater.PREFS, MODE_PRIVATE)
        playerView = findViewById(R.id.playerView)
        osd = findViewById(R.id.osd)
        numberBox = findViewById(R.id.numberBox)
        channelPanel = findViewById(R.id.channelPanel)
        categoryTitle = findViewById(R.id.categoryTitle)
        channelList = findViewById(R.id.channelList)

        channelList.layoutManager = LinearLayoutManager(this)
        channelList.adapter = adapter

        httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 7.1.2; Philips TV) AppleWebKit/537.36 Chrome/120.0 Safari/537.36")
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)
            .setAllowCrossProtocolRedirects(true)

        // Son izlenen kanal (yoksa TRT 1)
        val lastNumber = prefs.getInt("last_channel", 22)
        currentIndex = Channels.indexOf(lastNumber).coerceAtLeast(0)

        // Uygulama açılışında iptv-org linklerini arka planda güncelle (UI donmaz)
        PlaylistUpdater.updateAsync(this) { count ->
            if (isFinishing) return@updateAsync
            if (count > 0) {
                showOsd("Yayın listesi güncellendi: $count kanal")
            } else {
                showOsd("Liste güncellenemedi, yerleşik linkler kullanılıyor")
            }
            if (player?.currentMediaItem == null) playChannel(currentIndex)
        }
    }

    override fun onStart() {
        super.onStart()

        // 1080p'ye kadar en yüksek kaliteyi zorla
        val trackSelector = DefaultTrackSelector(this)
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setMaxVideoSize(1920, 1080)
                .setForceHighestSupportedBitrate(true)
                .build()
        )

        val p = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(DefaultDataSource.Factory(this, httpFactory))
            )
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        /* minBufferMs = */ 2500,
                        /* maxBufferMs = */ 8000,
                        /* bufferForPlaybackMs = */ 1000,
                        /* bufferForPlaybackAfterRebufferMs = */ 2000
                    )
                    .build()
            )
            .build()

        p.addListener(playerListener)
        playerView.player = p
        player = p
        playChannel(currentIndex)
    }

    override fun onStop() {
        handler.removeCallbacksAndMessages(null)
        playerView.player = null
        player?.release()
        player = null
        super.onStop()
    }

    // ---------------------------------------------------------------- Oynatma

    /** Kanal için denenecek adresler: yerleşik linkler, ardından iptv-org güncel link. */
    private fun buildCandidates(ch: Channel): List<String> {
        val updated = prefs.getString("url_${ch.number}", null)
        return (ch.urls + listOfNotNull(updated))
            .filter { it.isNotBlank() }
            .distinct()
    }

    /** Kanala özel Referer ve Origin başlıklarını, sonraki isteklere uygular. */
    private fun applyHeaders(referer: String) {
        val uri = Uri.parse(referer)
        val origin = "${uri.scheme}://${uri.host}"
        httpFactory.setDefaultRequestProperties(
            mapOf("Referer" to referer, "Origin" to origin)
        )
    }

    private fun playChannel(index: Int) {
        if (index !in Channels.ALL.indices) return
        currentIndex = index
        val ch = Channels.ALL[index]

        prefs.edit().putInt("last_channel", ch.number).apply()

        // Önceki kanala ait bekleyen işleri iptal et, eski yayını durdur
        handler.removeCallbacks(retryPlay)
        handler.removeCallbacks(switchChannel)
        player?.stop()

        candidates = buildCandidates(ch)
        attempt = 0

        if (candidates.isEmpty()) {
            showOsd("${ch.number}  ${ch.name}  ·  yayın bulunamadı")
            return
        }

        showOsd("${ch.number}  ${ch.name}  ·  ${ch.category.label}")
        // Hızlı geçişlerde yalnızca son seçilen kanal açılsın (0,4 sn bekleme)
        handler.postDelayed(switchChannel, 400)
    }

    /** Mevcut deneme sırasındaki adresi, kanalın Referer başlığıyla oynatıcıya yükler. */
    private fun startPlayback() {
        val p = player ?: return
        if (attempt >= candidates.size) return
        val ch = Channels.ALL[currentIndex]
        applyHeaders(ch.referer)
        p.setMediaItem(MediaItem.fromUri(candidates[attempt]))
        p.prepare()
        p.playWhenReady = true
    }

    private fun showOsd(text: String) {
        osd.text = text
        osd.visibility = View.VISIBLE
        handler.removeCallbacks(hideOsd)
        handler.postDelayed(hideOsd, 3500)
    }

    // ---------------------------------------------------------------- Rakam girişi

    private fun appendDigit(d: Int) {
        digits.append(d)
        numberBox.text = digits.toString()
        numberBox.visibility = View.VISIBLE
        handler.removeCallbacks(commitDigits)
        if (digits.length >= 3) {
            handler.post(commitDigits)
        } else {
            handler.postDelayed(commitDigits, 1300)
        }
    }

    private fun commitNumber() {
        val number = digits.toString().toIntOrNull()
        digits.clear()
        numberBox.visibility = View.GONE

        val index = if (number == null) -1 else Channels.indexOf(number)
        if (index >= 0) {
            playChannel(index)
        } else {
            showOsd("Kanal bulunamadı: $number")
        }
    }

    // ---------------------------------------------------------------- Kanal listesi

    private fun showList() {
        listOpen = true
        channelPanel.visibility = View.VISIBLE
        applyFilter(category)
    }

    private fun hideList() {
        listOpen = false
        channelPanel.visibility = View.GONE
    }

    private fun applyFilter(cat: Category) {
        category = cat
        val filtered = if (cat == Category.ALL) {
            Channels.ALL
        } else {
            Channels.ALL.filter { it.category == cat }
        }
        val playing = Channels.ALL[currentIndex].number
        val selectedPos = filtered.indexOfFirst { it.number == playing }.coerceAtLeast(0)

        adapter.playingNumber = playing
        adapter.setItems(filtered, selectedPos)
        channelList.scrollToPosition(adapter.selected)
        categoryTitle.text = "◀  ${cat.label}  ▶"
    }

    private fun cycleCategory(delta: Int) {
        val all = Category.values()
        val next = all[(category.ordinal + delta + all.size) % all.size]
        applyFilter(next)
    }

    private fun chooseFromList(channel: Channel) {
        hideList()
        playChannel(Channels.indexOf(channel.number))
    }

    // ---------------------------------------------------------------- Kumanda

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (listOpen) {
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    adapter.moveSelection(-1)
                    channelList.scrollToPosition(adapter.selected)
                    true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    adapter.moveSelection(1)
                    channelList.scrollToPosition(adapter.selected)
                    true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    cycleCategory(-1)
                    true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    cycleCategory(1)
                    true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    adapter.selectedChannel()?.let { chooseFromList(it) }
                    true
                }
                KeyEvent.KEYCODE_BACK -> {
                    hideList()
                    true
                }
                else -> super.onKeyDown(keyCode, event)
            }
        }

        if (keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) {
            appendDigit(keyCode - KeyEvent.KEYCODE_0)
            return true
        }

        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                playChannel((currentIndex - 1 + Channels.ALL.size) % Channels.ALL.size)
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                playChannel((currentIndex + 1) % Channels.ALL.size)
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                showList()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}

/** Kanal listesi adaptörü. Hafif tutuldu: tek tip satır, gölge/animasyon yok. */
class ChannelAdapter(private val onClick: (Channel) -> Unit) :
    RecyclerView.Adapter<ChannelAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val number: TextView = view.findViewById(R.id.itemNumber)
        val name: TextView = view.findViewById(R.id.itemName)
    }

    private var items: List<Channel> = emptyList()

    var selected = 0
        private set

    var playingNumber = -1

    fun setItems(list: List<Channel>, selectedPos: Int) {
        items = list
        selected = selectedPos.coerceIn(0, (list.size - 1).coerceAtLeast(0))
        notifyDataSetChanged()
    }

    fun moveSelection(delta: Int) {
        if (items.isEmpty()) return
        val old = selected
        selected = (selected + delta + items.size) % items.size
        notifyItemChanged(old)
        notifyItemChanged(selected)
    }

    fun selectedChannel(): Channel? = items.getOrNull(selected)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ch = items[position]
        holder.number.text = ch.number.toString()
        holder.name.text = ch.name
        holder.name.setTextColor(
            if (ch.number == playingNumber) 0xFF4FC3F7.toInt() else Color.WHITE
        )
        holder.itemView.setBackgroundColor(
            if (position == selected) 0x553FA9F5.toInt() else Color.TRANSPARENT
        )
        holder.itemView.setOnClickListener { onClick(ch) }
    }
}
