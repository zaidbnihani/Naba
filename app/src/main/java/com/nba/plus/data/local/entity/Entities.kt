package com.nba.plus.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** كاش المقالات (للقراءة دون اتصال). */
@Entity(
    tableName = "articles",
    indices = [
        Index(value = ["publishedAt"]),
        Index(value = ["category"]),
        Index(value = ["sourceId"]),
    ],
)
data class ArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val content: String,
    val url: String,
    val imageUrl: String?,
    val sourceId: String,
    val sourceName: String,
    val sourceIconUrl: String?,
    val category: String,
    val publishedAt: Long,
    val isBreaking: Boolean,
    val popularity: Int,
    val likeCount: Int,
    val commentCount: Int,
    val fetchedAt: Long = System.currentTimeMillis(),
)

/** المقالات المحفوظة/المفضلة. */
@Entity(tableName = "saved_articles", indices = [Index(value = ["savedAt"])])
data class SavedArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val content: String,
    val url: String,
    val imageUrl: String?,
    val sourceId: String,
    val sourceName: String,
    val sourceIconUrl: String?,
    val category: String,
    val publishedAt: Long,
    val isBreaking: Boolean,
    val popularity: Int,
    val likeCount: Int,
    val commentCount: Int,
    val savedAt: Long,
)

/** المقالات التي أعجب بها المستخدم. */
@Entity(tableName = "liked_articles", indices = [Index(value = ["likedAt"])])
data class LikedArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val content: String,
    val url: String,
    val imageUrl: String?,
    val sourceId: String,
    val sourceName: String,
    val sourceIconUrl: String?,
    val category: String,
    val publishedAt: Long,
    val isBreaking: Boolean,
    val popularity: Int,
    val likeCount: Int,
    val commentCount: Int,
    val likedAt: Long,
)

@Entity(tableName = "followed_sources")
data class FollowedSourceEntity(
    @PrimaryKey val sourceId: String,
    val followedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "followed_categories")
data class FollowedCategoryEntity(
    @PrimaryKey val categoryId: String,
    val followedAt: Long = System.currentTimeMillis(),
)

/** سجل ما ظهر سابقًا — لإزالة التكرار عبر الجلسات. */
@Entity(tableName = "seen_articles")
data class SeenArticleEntity(
    @PrimaryKey val articleId: String,
    val normalizedTitle: String,
    val normalizedUrl: String,
    val seenAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis(),
)
