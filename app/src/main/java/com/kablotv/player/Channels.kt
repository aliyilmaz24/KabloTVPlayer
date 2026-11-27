package com.kablotv.player

enum class Category(val label: String) {
    ALL("TÜMÜ"),
    NATIONAL("ULUSAL"),
    NEWS("HABER"),
    SPORT("SPOR"),
    OTHER("DİĞER")
}

/**
 * number   : Türksat KabloTV kanal numarası
 * keys     : iptv-org listesindeki kanal adıyla eşleşecek normalize edilmiş anahtarlar
 * urls     : yerleşik yayın adresleri (1. ana, 2. yedek)
 * referer  : bu kanalın HTTP isteğinde gönderilecek Referer ve Origin başlığının kaynağı
 */
data class Channel(
    val number: Int,
    val name: String,
    val category: Category,
    val keys: List<String>,
    val urls: List<String>,
    val referer: String
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

    private const val TABII = "https://www.tabii.com/"

    private fun ch(
        number: Int,
        name: String,
        category: Category,
        referer: String,
        urls: List<String>,
        vararg extra: String
    ) = Channel(
        number = number,
        name = name,
        category = category,
        keys = listOf(normalizeName(name)) + extra,
        urls = urls,
        referer = referer
    )

    val ALL: List<Channel> = listOf(
        // Ulusal (22 - 50)
        ch(22, "TRT 1", N, TABII,
            listOf("https://tv-trt1.medya.trt.com.tr/master.m3u8",
                   "https://tv-trt1.medya.trt.com.tr/master_1080.m3u8"), "trt1hd"),
        ch(23, "ATV", N, "https://www.atv.com.tr/",
            listOf("https://trkvz.daioncdn.net/atv/atv.m3u8?app=d5eb593f-39d9-4b01-9cfd-4748e8332cf0&ce=3",
                   "https://trkvz-live.daioncdn.net/atv/atv.m3u8")),
        ch(24, "Show TV", N, "https://www.showtv.com.tr/",
            listOf("https://ciner.daioncdn.net/showtv/showtv.m3u8?app=showtv_web&ce=3",
                   "https://ciner-live.daioncdn.net/showtv/showtv.m3u8")),
        ch(25, "Kanal D", N, "https://www.kanald.com.tr/",
            listOf("https://demiroren.daioncdn.net/kanald/kanald.m3u8?app=kanald_web&ce=3",
                   "https://demiroren-live.daioncdn.net/kanald/kanald.m3u8")),
        ch(26, "Star TV", N, "https://www.startv.com.tr/",
            listOf("https://dogus.daioncdn.net/startv/startv.m3u8?app=startv_web&ce=3",
                   "https://dogus-live.daioncdn.net/startv/startv.m3u8"), "star"),
        ch(27, "Kanal 7", N, "https://www.kanal7.com/",
            listOf("https://kanal7-live.daioncdn.net/kanal7/kanal7.m3u8",
                   "https://yurhnwtpys.turknet.ercdn.net/cvmbjbpmdx/kanal7/kanal7.m3u8")),
        ch(28, "NOW", N, "https://www.nowtv.com.tr/",
            listOf("https://uycyyuuzyh.turknet.ercdn.net/nphindgytw/nowtv/nowtv.m3u8",
                   "https://nowtv-live.daioncdn.net/nowtv/nowtv.m3u8"), "nowtv"),
        ch(29, "TV8", N, "https://www.tv8.com.tr/",
            listOf("https://tv8.daioncdn.net/tv8/tv8.m3u8?app=7ddc255a-ef47-4e81-ab14-c0e5f2949788&ce=3",
                   "https://tv8-live.daioncdn.net/tv8/tv8.m3u8")),
        ch(30, "Beyaz TV", N, "https://www.beyaztv.com.tr/",
            listOf("https://beyaztv-live.daioncdn.net/beyaztv/beyaztv.m3u8")),
        ch(31, "a2", N, "https://www.a2tv.com.tr/",
            listOf("https://trkvz.daioncdn.net/a2tv/a2tv.m3u8?app=59363a60-be96-4f73-9eff-355d0ff2c758&ce=3",
                   "https://trkvz-live.daioncdn.net/a2tv/a2tv.m3u8")),
        ch(32, "Teve2", N, "https://www.teve2.com.tr/",
            listOf("https://demiroren.daioncdn.net/teve2/teve2.m3u8?app=6aab838a-437e-4a1b-bbd0-e30f79cdbbbd&ce=3",
                   "https://demiroren-live.daioncdn.net/teve2/teve2.m3u8")),
        ch(33, "360", N, "https://www.tv360.com.tr/",
            listOf("https://turkmedya-live.ercdn.net/tv360/tv360.m3u8")),
        ch(34, "TLC", N, "https://www.tlctv.com.tr/",
            listOf("https://dogus.daioncdn.net/tlc/tlc.m3u8?app=1aca0aa1-06e3-41ff-a8e5-38d7df475469&ce=3",
                   "https://dogus.daioncdn.net/tlc/tlc.m3u8?app=tlc_web&ce=3")),
        ch(35, "DMAX", N, "https://www.dmax.com.tr/",
            listOf("https://dogus.daioncdn.net/dmax/dmax.m3u8?app=5a02c599-d17e-4982-9b04-090934d51af7&ce=3",
                   "https://dogus.daioncdn.net/dmax/dmax.m3u8?app=dmax_web&ce=3")),
        ch(37, "tv100", N, "https://www.tv100.com/",
            listOf("https://tv100-live.daioncdn.net/tv100/tv100.m3u8")),
        ch(41, "TV 8,5", N, "https://www.tv8bucuk.com/",
            listOf("https://tv8.daioncdn.net/tv8bucuk/tv8bucuk.m3u8")),
        ch(48, "TV4", N, "https://www.tv4.com.tr/",
            listOf("https://turkmedya-live.ercdn.net/tv4/tv4.m3u8")),
        ch(50, "TRT 2", N, TABII,
            listOf("https://tv-trt2.medya.trt.com.tr/master.m3u8")),

        // Haber (55 - 88)
        ch(55, "TRT Haber", H, "https://www.trthaber.com/",
            listOf("https://tv-trthaber.medya.trt.com.tr/master.m3u8")),
        ch(56, "A Haber", H, "https://www.ahaber.com.tr/",
            listOf("https://trkvz.daioncdn.net/ahaber/ahaber.m3u8?app=web&ce=3",
                   "https://trkvz-live.daioncdn.net/ahaber/ahaber.m3u8")),
        ch(57, "NTV", H, "https://www.ntv.com.tr/",
            listOf("https://dogus.daioncdn.net/ntv/ntv.m3u8?app=ntv_web&ce=3",
                   "https://dogus-live.daioncdn.net/ntv/ntv.m3u8")),
        ch(58, "CNN Türk", H, "https://www.cnnturk.com/",
            listOf("https://demiroren-live.daioncdn.net/cnnturk/cnnturk.m3u8"), "cnnturk"),
        ch(59, "24 TV", H, "https://www.yirmidort.tv/",
            listOf("https://turkmedya-live.ercdn.net/tv24/tv24.m3u8")),
        ch(60, "TGRT Haber", H, "https://www.tgrthaber.com/",
            listOf("https://canli.tgrthaber.com/tgrt.m3u8"), "tgrt"),
        ch(61, "Habertürk TV", H, "https://www.haberturk.com/",
            listOf("https://ciner.daioncdn.net/haberturktv/haberturktv.m3u8?app=c98ab0b0-50cc-495b-bb37-778e91f5ff5b&ce=3",
                   "https://ciner-live.daioncdn.net/haberturktv/haberturktv.m3u8"), "haberturk"),
        ch(62, "Bloomberg HT", H, "https://www.bloomberght.com/",
            listOf("https://ciner.daioncdn.net/bloomberght/bloomberght.m3u8?app=201b9556-b11c-4d92-b264-f21fd24e9aaf&ce=3",
                   "https://ciner-live.daioncdn.net/bloomberght/bloomberght.m3u8")),
        ch(63, "Ülke TV", H, "https://www.ulketv.com/",
            listOf("https://ulketv-live.daioncdn.net/ulketv/ulketv.m3u8")),
        ch(64, "TVNET", H, "https://www.tvnet.com.tr/",
            listOf("https://mn-nl.mncdn.com/tvnet/tvnet/playlist.m3u8")),
        ch(67, "TRT Türk", H, TABII,
            listOf("https://tv-trtturk.medya.trt.com.tr/master.m3u8")),
        ch(68, "TRT Avaz", H, TABII,
            listOf("https://tv-trtavaz.medya.trt.com.tr/master.m3u8")),
        ch(69, "TRT 3 Spor / TBMM", H, TABII,
            listOf("https://tv-trt3.medya.trt.com.tr/master.m3u8"), "trt3spor", "tbmm"),
        ch(70, "TRT Kurdî", H, TABII,
            listOf("https://tv-trtkurdi.medya.trt.com.tr/master.m3u8")),
        ch(71, "Haber Global", H, "https://haberglobal.com.tr/",
            listOf("https://ensonhaber-live.ercdn.net/haberglobal/haberglobal.m3u8")),
        ch(74, "Halk TV", H, "https://halktv.com.tr/",
            listOf("https://halktv-live.daioncdn.net/halktv/halktv.m3u8")),
        ch(76, "Sözcü TV", H, "https://www.szctv.com.tr/",
            listOf("https://sozcu-live.daioncdn.net/sozcutv/sozcutv.m3u8"), "sozcu", "szc"),
        ch(77, "A Para", H, "https://www.apara.com.tr/",
            listOf("https://trkvz-live.daioncdn.net/apara/apara.m3u8")),
        ch(88, "TRT World", H, "https://www.trtworld.com/",
            listOf("https://tv-trtworld.medya.trt.com.tr/master.m3u8")),

        // Belgesel, Çocuk, Müzik, Eğitim (DİĞER)
        ch(111, "TRT Belgesel", O, TABII,
            listOf("https://tv-trtbelgesel.medya.trt.com.tr/master.m3u8")),
        ch(444, "TRT Çocuk", O, "https://www.trtcocuk.net.tr/",
            listOf("https://tv-trtcocuk.medya.trt.com.tr/master.m3u8")),
        ch(465, "TRT EBA İlkokul", O, TABII,
            listOf("https://tv-e-okul01.medya.trt.com.tr/master.m3u8")),
        ch(466, "TRT EBA Ortaokul", O, TABII,
            listOf("https://tv-e-okul02.medya.trt.com.tr/master.m3u8")),
        ch(467, "TRT EBA Lise", O, TABII,
            listOf("https://tv-e-okul03.medya.trt.com.tr/master.m3u8")),
        ch(555, "Diyanet TV", O, "https://www.diyanet.tv/",
            listOf("https://eustr73.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_5c9e17f59dc09.smil/playlist.m3u8")),
        ch(666, "TRT Müzik", O, TABII,
            listOf("https://tv-trtmuzik.medya.trt.com.tr/master.m3u8")),
        ch(667, "Dream Türk", O, "https://www.dreamturk.com.tr/",
            listOf("https://demiroren-live.daioncdn.net/dreamturk/dreamturk.m3u8"), "dreamturkhd"),

        // Spor (222 - 244)
        ch(222, "TRT Spor", S, "https://www.trtspor.com.tr/",
            listOf("https://tv-trtspor1.medya.trt.com.tr/master.m3u8"), "trtsporhd"),
        ch(223, "beIN Sports Haber", S, "https://beinsports.com.tr/",
            listOf("https://bein-live.daioncdn.net/beinsportshaber/beinsportshaber.m3u8"), "beinsportshaber"),
        ch(224, "A Spor", S, "https://www.aspor.com.tr/",
            listOf("https://trkvz.daioncdn.net/aspor/aspor.m3u8?app=45f847c4-04e8-419a-a561-2ebf87084765&ce=3",
                   "https://trkvz-live.daioncdn.net/aspor/aspor.m3u8")),
        ch(227, "HT Spor", S, "https://www.htspor.com/",
            listOf("https://ciner.daioncdn.net/ht-spor/ht-spor.m3u8?app=web&ce=3",
                   "https://ciner-live.daioncdn.net/htspor/htspor.m3u8")),
        ch(244, "TRT Spor Yıldız", S, "https://www.trtspor.com.tr/",
            listOf("https://tv-trtspor2.medya.trt.com.tr/master.m3u8"))
    ).sortedBy { it.number }

    private val byNumber: Map<Int, Int> = ALL.withIndex().associate { it.value.number to it.index }

    /** Kanal numarasının listedeki indeksini döndürür; yoksa -1. */
    fun indexOf(number: Int): Int = byNumber[number] ?: -1
}
