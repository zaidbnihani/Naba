package com.nba.plus.domain.util

import com.nba.plus.domain.model.Article

/**
 * كاشف التكرار: يرشّح الأخبار المكررة إما لتطابق المعرّف أو الرابط،
 * أو لتطابق العنوان المطبّع، أو لتشابه عالٍ (Jaccard على الكلمات).
 */
class DedupDetector(
    private val similarityThreshold: Double = 0.75,
) {

    data class SeenArticle(
        val articleId: String,
        val normalizedTitle: String,
        val normalizedUrl: String,
    )

    /** يعيد فقط الأخبار غير المكررة ويحدّث قائمة «المرئيّ». */
    fun filter(incoming: List<Article>, seen: MutableList<SeenArticle>): List<Article> {
        val unique = mutableListOf<Article>()
        for (article in incoming) {
            if (!isDuplicate(article, seen)) {
                unique += article
                seen += article.toSeen()
            }
        }
        return unique
    }

    fun isDuplicate(article: Article, seen: List<SeenArticle>): Boolean {
        val normalizedUrl = TitleNormalizer.normalizeUrl(article.url)
        val normalizedTitle = TitleNormalizer.normalize(article.title)
        for (other in seen.asReversed()) {
            if (other.articleId == article.id) return true
            if (other.normalizedUrl == normalizedUrl && normalizedUrl.isNotEmpty()) return true
            if (other.normalizedTitle == normalizedTitle && normalizedTitle.isNotEmpty()) return true
            if (similarityThreshold > 0.0 &&
                tokenJaccard(other.normalizedTitle, normalizedTitle) >= similarityThreshold
            ) return true
        }
        return false
    }

    fun Article.toSeen() = SeenArticle(
        articleId = id,
        normalizedTitle = TitleNormalizer.normalize(title),
        normalizedUrl = TitleNormalizer.normalizeUrl(url),
    )

    /** تشابه Jaccard بين مجموعتي كلمات. */
    fun tokenJaccard(a: String, b: String): Double {
        if (a.isBlank() || b.isBlank()) return 0.0
        val setA = a.split(' ').toSet()
        val setB = b.split(' ').toSet()
        if (setA.isEmpty() || setB.isEmpty()) return 0.0
        val intersection = setA.intersect(setB).size
        val union = setA.union(setB).size
        return intersection.toDouble() / union.toDouble()
    }
}
