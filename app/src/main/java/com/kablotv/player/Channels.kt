package com.kablotv.player

enum class Category(val label: String) {
    ALL("TÜMÜ"),
    NATIONAL("ULUSAL"),
    NEWS("HABER"),
    SPORT("SPOR"),
    OTHER("DİĞER")
}

/**
 * number      : Türksat KabloTV kanal numarası
 * keys        : iptv-org listesindeki kanal adıyla eşleşecek normalize edilmiş anahtarlar
 * fallbackUrl : iptv-org eşleşmesi yoksa veya yayın hata verirse kullanılacak resmi yayın adresi
 */
data class Channel(
    val number: Int,
    val name: String,
    val category: Category,
    val keys: List<String>,
    val fallbackUrl: String? = null
)

private val TR_MAP = mapOf(
    'ı' to 'i', 'İ' to 'i', 'î' to 'i',
    'ğ' to 'g', 'Ğ' to 'g',
    'ü' to 'u', 'Ü' to 'u', 'û' to 'u',
    'ş' to 's', 'Ş' to 's',
    'ö' to 'o', 'Ö' to 'o',
    'ç' to 'c', 'Ç' to 'c',
    'â' to 'a'
)

/** Türkçe karakterleri sadeleştirip yalnızca harf/rakamları küçük harfle döndürür. */
fun normalizeName(s: String): String {
    val sb = StringBuilder(s.length)
    for (c in s) {
        val m = TR_MAP[c] ?: c
        if (m.isLetterOrDigit()) sb.append(m.lowercaseChar())
    }
    return sb.toString()
}

object Channels {

    private val N = Category.NATIONAL
    private val H = Category.NEWS
    private val S = Category.SPORT
    private val O = Category.OTHER

    /** Kanal numarası -> resmi varsayılan (fallback) yayın adresi. */
    private val FALLBACK_URLS: Map<Int, String> = mapOf(
        22 to "https://tv-trt1.medya.trt.com.tr/master.m3u8",
        23 to "https://trkvz-live.daioncdn.net/atv/atv.m3u8",
        24 to "https://ciner-live.daioncdn.net/showtv/showtv.m3u8",
        25 to "https://demiroren.daioncdn.net/kanald/kanald.m3u8?app=kanald_web&ce=3",
        26 to "https://dogus-live.daioncdn.net/startv/startv.m3u8",
        27 to "https://kanal7-live.daioncdn.net/kanal7/kanal7.m3u8",
        28 to "https://nowtv-live.daioncdn.net/nowtv/nowtv.m3u8",
        29 to "https://tv8.daioncdn.net/tv8/tv8.m3u8?app=7ddc255a-ef47-4e81-ab14-c0e5f2949788&ce=3",
        30 to "https://beyaztv-live.daioncdn.net/beyaztv/beyaztv.m3u8",
        31 to "https://trkvz-live.daioncdn.net/a2tv/a2tv.m3u8",
        32 to "https://demiroren-live.daioncdn.net/teve2/teve2.m3u8",
        33 to "https://turkmedya-live.ercdn.net/tv360/tv360.m3u8",
        34 to "https://dogus-live.daioncdn.net/tlc/tlc.m3u8",
        35 to "https://dogus-live.daioncdn.net/dmax/dmax.m3u8",
        37 to "https://tv100-live.daioncdn.net/tv100/tv100.m3u8",
        41 to "https://tv8.daioncdn.net/tv8bucuk/tv8bucuk.m3u8",
        48 to "https://turkmedya-live.ercdn.net/tv4/tv4.m3u8",
        50 to "https://tv-trt2.medya.trt.com.tr/master.m3u8",
        55 to "https://tv-trthaber.medya.trt.com.tr/master.m3u8",
        56 to "https://trkvz-live.daioncdn.net/ahaber/ahaber.m3u8",
        57 to "https://dogus-live.daioncdn.net/ntv/ntv.m3u8",
        58 to "https://demiroren-live.daioncdn.net/cnnturk/cnnturk.m3u8",
        59 to "https://turkmedya-live.ercdn.net/tv24/tv24.m3u8",
        60 to "https://canli.tgrthaber.com/tgrt.m3u8",
        61 to "https://ciner-live.daioncdn.net/haberturktv/haberturktv.m3u8",
        62 to "https://ciner-live.daioncdn.net/bloomberght/bloomberght.m3u8",
        63 to "https://ulketv-live.daioncdn.net/ulketv/ulketv.m3u8",
        64 to "https://mn-nl.mncdn.com/tvnet/tvnet/playlist.m3u8",
        67 to "https://tv-trtturk.medya.trt.com.tr/master.m3u8",
        68 to "https://tv-trtavaz.medya.trt.com.tr/master.m3u8",
        69 to "https://tv-trt3.medya.trt.com.tr/master.m3u8",
        70 to "https://tv-trtkurdi.medya.trt.com.tr/master.m3u8",
        71 to "https://ensonhaber-live.ercdn.net/haberglobal/haberglobal.m3u8",
        74 to "https://halktv-live.daioncdn.net/halktv/halktv.m3u8",
        76 to "https://sozcu-live.daioncdn.net/sozcutv/sozcutv.m3u8",
        77 to "https://trkvz-live.daioncdn.net/apara/apara.m3u8",
        88 to "https://tv-trtworld.medya.trt.com.tr/master.m3u8",
        111 to "https://tv-trtbelgesel.medya.trt.com.tr/master.m3u8",
        222 to "https://tv-trtspor1.medya.trt.com.tr/master.m3u8",
        223 to "https://bein-live.daioncdn.net/beinsportshaber/beinsportshaber.m3u8",
        224 to "https://trkvz-live.daioncdn.net/aspor/aspor.m3u8",
        227 to "https://ciner-live.daioncdn.net/htspor/htspor.m3u8",
        244 to "https://tv-trtspor2.medya.trt.com.tr/master.m3u8",
        444 to "https://tv-trtcocuk.medya.trt.com.tr/master.m3u8",
        465 to "https://tv-e-okul01.medya.trt.com.tr/master.m3u8",
        466 to "https://tv-e-okul02.medya.trt.com.tr/master.m3u8",
        467 to "https://tv-e-okul03.medya.trt.com.tr/master.m3u8",
        555 to "https://eustr73.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_5c9e17f59dc09.smil/playlist.m3u8",
        666 to "https://tv-trtmuzik.medya.trt.com.tr/master.m3u8",
        667 to "https://demiroren-live.daioncdn.net/dreamturk/dreamturk.m3u8"
    )

    private fun ch(number: Int, name: String, category: Category, vararg extra: String) =
        Channel(number, name, category, listOf(normalizeName(name)) + extra)

    val ALL: List<Channel> = listOf(
        // Ulusal (22 - 50)
        ch(22, "TRT 1", N, "trt1hd"),
        ch(23, "ATV", N),
        ch(24, "Show TV", N),
        ch(25, "Kanal D", N),
        ch(26, "Star TV", N, "star"),
        ch(27, "Kanal 7", N),
        ch(28, "NOW", N, "nowtv"),
        ch(29, "TV8", N),
        ch(30, "Beyaz TV", N),
        ch(31, "a2", N),
        ch(32, "Teve2", N),
        ch(33, "360", N),
        ch(34, "TLC", N),
        ch(35, "DMAX", N),
        ch(37, "tv100", N),
        ch(41, "TV 8,5", N),
        ch(48, "TV4", N),
        ch(50, "TRT 2", N),

        // Haber (55 - 88)
        ch(55, "TRT Haber", H),
        ch(56, "A Haber", H),
        ch(57, "NTV", H),
        ch(58, "CNN Türk", H),
        ch(59, "24 TV", H),
        ch(60, "TGRT Haber", H, "tgrt"),
        ch(61, "Habertürk TV", H, "haberturk"),
        ch(62, "Bloomberg HT", H),
        ch(63, "Ülke TV", H),
        ch(64, "TVNET", H),
        ch(67, "TRT Türk", H),
        ch(68, "TRT Avaz", H),
        ch(69, "TRT 3 Spor / TBMM", H, "trt3spor", "tbmm"),
        ch(70, "TRT Kurdî", H),
        ch(71, "Haber Global", H),
        ch(74, "Halk TV", H),
        ch(76, "Sözcü TV", H, "sozcu", "szc"),
        ch(77, "A Para", H),
        ch(88, "TRT World", H),

        // Belgesel, Çocuk, Müzik (DİĞER)
        ch(111, "TRT Belgesel", O),
        ch(444, "TRT Çocuk", O),
        ch(465, "TRT EBA İlkokul", O),
        ch(466, "TRT EBA Ortaokul", O),
        ch(467, "TRT EBA Lise", O),
        ch(555, "Diyanet TV", O),
        ch(666, "TRT Müzik", O),
        ch(667, "Dream Türk", O),

        // Spor (222 - 244)
        ch(222, "TRT Spor", S),
        ch(223, "beIN Sports Haber", S, "beinsportshaber"),
        ch(224, "A Spor", S),
        ch(227, "HT Spor", S),
        ch(244, "TRT Spor Yıldız", S)
    ).map { it.copy(fallbackUrl = FALLBACK_URLS[it.number]) }
     .sortedBy { it.number }

    private val byNumber: Map<Int, Int> = ALL.withIndex().associate { it.value.number to it.index }

    /** Kanal numarasının listedeki indeksini döndürür; yoksa -1. */
    fun indexOf(number: Int): Int = byNumber[number] ?: -1
}
