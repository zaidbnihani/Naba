package com.nba.plus.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TitleNormalizerTest {

    @Test
    fun `removes Arabic diacritics and tatweel`() {
        val a = TitleNormalizer.normalize("مُحَمَّد")
        val b = TitleNormalizer.normalize("محـــمد")
        assertEquals(a, b)
        assertFalse(a.contains("ُ"))
    }

    @Test
    fun `unifies alef yaa taa forms`() {
        assertEquals(
            TitleNormalizer.normalize("أحمد يذهب إلى المدرسة"),
            TitleNormalizer.normalize("احمد يذهب الي المدرسه"),
        )
    }

    @Test
    fun `strips punctuation and collapses whitespace`() {
        assertEquals(
            TitleNormalizer.normalize("عاجل: الحكومة — تعتمد الخطة!"),
            TitleNormalizer.normalize("عاجل الحكومة تعتمد الخطة"),
        )
    }

    @Test
    fun `lowercases latin text`() {
        assertEquals(
            TitleNormalizer.normalize("AI Report"),
            TitleNormalizer.normalize("ai report"),
        )
    }

    @Test
    fun `normalizeUrl removes query and fragment`() {
        assertEquals(
            TitleNormalizer.normalizeUrl("https://example.com/news/1?utm=abc#top"),
            TitleNormalizer.normalizeUrl("https://example.com/news/1"),
        )
    }

    @Test
    fun `normalizeUrl handles trailing slash`() {
        assertEquals(
            TitleNormalizer.normalizeUrl("https://Example.com/news/"),
            "https://example.com/news",
        )
    }
}

class DedupDetectorTest {

    private val detector = DedupDetector(similarityThreshold = 0.75)

    private fun seen(id: String, title: String, url: String) = DedupDetector.SeenArticle(
        articleId = id,
        normalizedTitle = TitleNormalizer.normalize(title),
        normalizedUrl = TitleNormalizer.normalizeUrl(url),
    )

    @Test
    fun `jaccard similarity identical sets is one`() {
        assertEquals(1.0, detector.tokenJaccard("خبر عاجل جديد", "خبر عاجل جديد"), 0.0001)
    }

    @Test
    fun `jaccard similarity disjoint sets is zero`() {
        assertEquals(0.0, detector.tokenJaccard("كرة سلة", "بورصة عملات"), 0.0001)
    }

    @Test
    fun `detects duplicate by normalized title`() {
        val first = seen("1", "الحكومة تعتمد الخطة الاقتصادية", "https://a.com/1")
        val duplicateByTitle = seen("2", "الحكومة تعتمد الخطة الاقتصادية!", "https://a.com/2")
        assertTrue(detector.isDuplicate(duplicateByTitle, listOf(first)))
    }

    @Test
    fun `detects duplicate by url`() {
        val first = seen("1", "عنوان أول", "https://a.com/x?utm=9")
        val duplicateByUrl = seen("2", "عنوان مختلف تمامًا", "https://a.com/x")
        assertTrue(detector.isDuplicate(duplicateByUrl, listOf(first)))
    }

    @Test
    fun `different news pass through`() {
        val first = seen("1", "فوز المنتخب في المباراة النهائية", "https://a.com/1")
        val other = seen("2", "ارتفاع أسعار النفط عالميًا", "https://a.com/2")
        assertFalse(detector.isDuplicate(other, listOf(first)))
    }

    @Test
    fun `filter returns only unique and updates seen list`() {
        val seenList = mutableListOf(seen("1", "خبر أول هنا", "https://a.com/1"))
        val articles = listOf(
            article("1", "خبر أول هنا", "https://a.com/1"),     // مكرر بالمعرف والعنوان
            article("2", "خبر أول هنا", "https://a.com/99"),    // مكرر بالعنوان
            article("3", "خبر جديد تمامًا", "https://a.com/3"), // فريد
        )
        val unique = detector.filter(articles, seenList)
        assertEquals(listOf("3"), unique.map { it.id })
        assertEquals(3, seenList.size) // الأصل + الفريد فقط
    }

    private fun article(id: String, title: String, url: String) =
        com.nba.plus.domain.model.Article(
            id = id,
            title = title,
            description = "",
            content = "",
            url = url,
            imageUrl = null,
            sourceId = "s",
            sourceName = "مصدر",
            sourceIconUrl = null,
            category = "politics",
            publishedAt = java.time.Instant.now(),
        )
}
