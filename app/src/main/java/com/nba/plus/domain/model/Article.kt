package com.nba.plus.domain.model

import java.time.Instant

/**
 * نموذج الخبر الأساسي في طبقة المجال.
 */
data class Article(
    val id: String,
    val title: String,
    val description: String,
    val content: String,
    val url: String,
    val imageUrl: String?,
    val sourceId: String,
    val sourceName: String,
    val sourceIconUrl: String?,
    val category: String,
    val publishedAt: Instant,
    val isBreaking: Boolean = false,
    val popularity: Int = 0,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
)

/** مقال محفوظ مع وقت الحفظ. */
data class SavedArticle(
    val article: Article,
    val savedAt: Instant,
)
