package com.kablotv.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL

/**
 * iptv-org Türkiye listesini arka planda indirir, kanal adlarını eşleştirir
 * ve bulunan yayın linklerini SharedPreferences'a yazar.
 */
object PlaylistUpdater {

    const val PREFS = "kablotv_prefs"
    private const val M3U_URL = "https://iptv-org.github.io/iptv/countries/tr.m3u"

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

    private fun update(context: Context): Int {
        val conn = (URL(M3U_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 20_000
        }

        // normalize edilmiş kanal adı -> yayın URL'si (ilk görülen kazanır)
        val streams = HashMap<String, String>()
        try {
            conn.inputStream.bufferedReader().use { reader ->
                var pendingName: String? = null
                reader.forEachLine { raw ->
                    val line = raw.trim()
                    if (line.startsWith("#EXTINF")) {
                        pendingName = normalizeName(line.substringAfterLast(','))
                    } else if (line.isNotEmpty() && !line.startsWith("#")) {
                        val name = pendingName
                        if (name != null && !streams.containsKey(name)) {
                            streams[name] = line
                        }
                        pendingName = null
                    }
                }
            }
        } finally {
            conn.disconnect()
        }

        var matched = 0
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        for (ch in Channels.ALL) {
            val url = ch.keys.firstNotNullOfOrNull { streams[it] }
            if (url != null) {
                editor.putString("url_${ch.number}", url)
                matched++
            }
        }
        editor.putLong("last_update", System.currentTimeMillis()).apply()
        return matched
    }
}
