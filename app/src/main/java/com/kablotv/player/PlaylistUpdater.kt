package com.kablotv.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL

/**
 * iptv-org Türkiye listesini arka planda indirir, kanalları esnek biçimde eşleştirir
 * ve bulunan yayın linklerini SharedPreferences'a yazar.
 */
object PlaylistUpdater {

    const val PREFS = "kablotv_prefs"
    private const val M3U_URL = "https://iptv-org.github.io/iptv/countries/tr.m3u"

    /**
     * Kanal numarası -> listede görülebilecek ek adlar (normalize edilmiş, Türkçe karaktersiz).
     * Bir kanal açılmazsa buraya listedeki adını ekleyebilirsin.
     */
    private val ALIASES: Map<Int, List<String>> = mapOf(
        22 to listOf("trt1", "trtbir"),
        23 to listOf("atvhd"),
        24 to listOf("showtvhd"),
        25 to listOf("kanaldhd", "kanald"),
        26 to listOf("startvhd", "star"),
        27 to listOf("kanal7hd"),
        28 to listOf("now", "nowtv", "nowhd", "nowtvhd"),
        29 to listOf("tv8hd"),
        31 to listOf("a2hd", "a2tv"),
        32 to listOf("teve2hd"),
        33 to listOf("tv360", "360hd"),
        34 to listOf("tlchd", "tlctr", "discoverytlc"),
        35 to listOf("dmaxhd", "dmaxtr", "discoverydmax"),
        37 to listOf("tv100hd"),
        48 to listOf("tv4hd"),
        57 to listOf("ntvhd"),
        58 to listOf("cnnturkhd", "cnnturk"),
        59 to listOf("tv24hd"),
        62 to listOf("bloomberght", "bloombergtv"),
        74 to listOf("halktvhd"),
        76 to listOf("sozcutvhd"),
        222 to listOf("trtsporhd"),
        223 to listOf("beinsportshaberhd", "beinsports"),
        224 to listOf("aspor", "aspor hd"),
        227 to listOf("htspor"),
        444 to listOf("trtcocuk"),
        666 to listOf("trtmuzik"),
        667 to listOf("dreamturk", "dreamturkhd")
    )

    /** Arka planda çalışır; onDone sonucu ANA THREAD'de döndürür (eşleşen kanal sayısı, hata: -1). */
    fun updateAsync(context: Context, onDone: (Int) -> Unit) {
        val app = context.applicationContext
        val main = Handler(Looper.getMainLooper())
        Thread {
            val result = try {
                update(app)
            } catch (e: Exception) {
                -1
            }
            main.post { onDone(result) }
        }.start()
    }

    /** Listedeki bir yayın girdisi: normalize edilmiş adları ve URL'si. */
    private class Entry(val names: List<String>, val url: String)

    private fun parse(url: String): List<Entry> {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 20_000
        }
        val entries = ArrayList<Entry>()
        try {
            conn.inputStream.bufferedReader().use { reader ->
                var pendingNames: List<String>? = null
                reader.forEachLine { raw ->
                    val line = raw.trim()
                    if (line.startsWith("#EXTINF")) {
                        val names = mutableListOf<String>()
                        // tvg-name="..." özniteliği varsa onu da kullan
                        Regex("tvg-name=\"([^\"]*)\"").find(line)?.groupValues?.get(1)
                            ?.takeIf { it.isNotBlank() }
                            ?.let { names.add(normalizeName(it)) }
                        // Virgülden sonraki görünen ad
                        names.add(normalizeName(line.substringAfterLast(',')))
                        pendingNames = names.filter { it.isNotEmpty() }.distinct()
                    } else if (line.isNotEmpty() && !line.startsWith("#")) {
                        val names = pendingNames
                        if (names != null && names.isNotEmpty()) {
                            entries.add(Entry(names, line))
                        }
                        pendingNames = null
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        return entries
    }

    private fun update(context: Context): Int {
        val entries = parse(M3U_URL)
        var matched = 0
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()

        for (ch in Channels.ALL) {
            val keys = (listOf(normalizeName(ch.name)) + ch.keys + (ALIASES[ch.number] ?: emptyList()))
                .map { normalizeName(it) }
                .filter { it.isNotEmpty() }
                .distinct()

            // 1. Tam eşleşme
            var url: String? = entries.firstOrNull { e -> e.names.any { it in keys } }?.url

            // 2. Esnek eşleşme: en az 4 karakterlik anahtar, ad içinde geçiyorsa
            if (url == null) {
                url = entries.firstOrNull { e ->
                    e.names.any { name -> keys.any { k -> k.length >= 4 && name.contains(k) } }
                }?.url
            }

            if (url != null) {
                editor.putString("url_${ch.number}", url)
                matched++
            } else {
                editor.remove("url_${ch.number}")
            }
        }
        editor.putLong("last_update", System.currentTimeMillis()).apply()
        return matched
    }
}
