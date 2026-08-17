package com.nba.plus.domain.util

/**
 * يطبّع عناوين الأخبار لأغراض كشف التكرار:
 * إزالة التشكيل والتطويل، توحيد الألف والهمزات والتاء المربوطة،
 * إزالة علامات الترقيم، وضغط المسافات.
 */
object TitleNormalizer {

    private val arabicDiacritics = Regex("[\\u064B-\\u065F\\u0670]")
    private const val tatweel = '\u0640'
    private val nonLetterOrDigit = Regex("[^\\p{L}\\p{N}\\s]")
    private val whitespace = Regex("\\s+")

    fun normalize(title: String): String {
        var t = title
            .lowercase()
            .replace(tatweel, ' ')
            .replace(arabicDiacritics, "")
        t = unifyArabicLetters(t)
        t = nonLetterOrDigit.replace(t, " ")
        return whitespace.replace(t, " ").trim()
    }

    private fun unifyArabicLetters(text: String): String = text
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('ٱ', 'ا')
        .replace('ى', 'ي')
        .replace('ئ', 'ي')
        .replace('ؤ', 'و')
        .replace('ة', 'ه')

    /** تطبيع رابط: إزالة المعاملات الترويجية وأثر التتبع. */
    fun normalizeUrl(url: String): String {
        val noParams = url.substringBefore('?').substringBefore('#')
        return noParams.removeSuffix("/").lowercase()
    }
}
