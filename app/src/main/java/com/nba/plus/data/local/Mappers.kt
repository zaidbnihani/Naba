package com.nba.plus.data.local

import com.nba.plus.data.local.entity.ArticleEntity
import com.nba.plus.data.local.entity.LikedArticleEntity
import com.nba.plus.data.local.entity.SavedArticleEntity
import com.nba.plus.data.remote.dto.ArticleDto
import com.nba.plus.data.remote.dto.SourceDto
import com.nba.plus.domain.model.Article
import com.nba.plus.domain.model.Source
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** تحويلات DTO ↔ كيانات Room ↔ نماذج المجال. */

internal fun resolvePublishedAt(pubDate: String?, ageMinutes: Long?, now: Instant): Instant {
    ageMinutes?.let { return now.minusSeconds(it * 60) }
    if (!pubDate.isNullOrBlank()) {
        runCatching { return Instant.parse(pubDate) }
        runCatching { return OffsetDateTime.parse(pubDate).toInstant() }
        runCatching { return LocalDateTime.parse(pubDate).toInstant(ZoneOffset.UTC) }
        runCatching {
            return LocalDateTime.parse(
                pubDate,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            ).toInstant(ZoneOffset.UTC)
        }
    }
    return now
}

fun ArticleDto.toDomain(now: Instant = Instant.now()): Article = Article(
    id = article_id ?: (link ?: title).hashCode().toString(),
    title = title,
    description = description.orEmpty(),
    content = content.orEmpty(),
    url = link.orEmpty(),
    imageUrl = image_url,
    sourceId = source_id.orEmpty(),
    sourceName = source_name.orEmpty(),
    sourceIconUrl = source_icon,
    category = category?.firstOrNull().orEmpty(),
    publishedAt = resolvePublishedAt(pubDate, age_minutes, now),
    isBreaking = is_breaking ?: false,
    popularity = popularity ?: 0,
    likeCount = like_count ?: 0,
    commentCount = comment_count ?: 0,
)

fun ArticleDto.toEntity(now: Instant = Instant.now()): ArticleEntity = ArticleEntity(
    id = article_id ?: (link ?: title).hashCode().toString(),
    title = title,
    description = description.orEmpty(),
    content = content.orEmpty(),
    url = link.orEmpty(),
    imageUrl = image_url,
    sourceId = source_id.orEmpty(),
    sourceName = source_name.orEmpty(),
    sourceIconUrl = source_icon,
    category = category?.firstOrNull().orEmpty(),
    publishedAt = resolvePublishedAt(pubDate, age_minutes, now).toEpochMilli(),
    isBreaking = is_breaking ?: false,
    popularity = popularity ?: 0,
    likeCount = like_count ?: 0,
    commentCount = comment_count ?: 0,
)

fun ArticleEntity.toDomain(): Article = Article(
    id = id,
    title = title,
    description = description,
    content = content,
    url = url,
    imageUrl = imageUrl,
    sourceId = sourceId,
    sourceName = sourceName,
    sourceIconUrl = sourceIconUrl,
    category = category,
    publishedAt = Instant.ofEpochMilli(publishedAt),
    isBreaking = isBreaking,
    popularity = popularity,
    likeCount = likeCount,
    commentCount = commentCount,
)

fun Article.toEntity(): ArticleEntity = ArticleEntity(
    id = id,
    title = title,
    description = description,
    content = content,
    url = url,
    imageUrl = imageUrl,
    sourceId = sourceId,
    sourceName = sourceName,
    sourceIconUrl = sourceIconUrl,
    category = category,
    publishedAt = publishedAt.toEpochMilli(),
    isBreaking = isBreaking,
    popularity = popularity,
    likeCount = likeCount,
    commentCount = commentCount,
)

fun Article.toSavedEntity(savedAt: Long): SavedArticleEntity = SavedArticleEntity(
    id = id, title = title, description = description, content = content, url = url,
    imageUrl = imageUrl, sourceId = sourceId, sourceName = sourceName,
    sourceIconUrl = sourceIconUrl, category = category,
    publishedAt = publishedAt.toEpochMilli(), isBreaking = isBreaking,
    popularity = popularity, likeCount = likeCount, commentCount = commentCount,
    savedAt = savedAt,
)

fun SavedArticleEntity.toDomain(): Article = Article(
    id = id, title = title, description = description, content = content, url = url,
    imageUrl = imageUrl, sourceId = sourceId, sourceName = sourceName,
    sourceIconUrl = sourceIconUrl, category = category,
    publishedAt = Instant.ofEpochMilli(publishedAt), isBreaking = isBreaking,
    popularity = popularity, likeCount = likeCount, commentCount = commentCount,
)

fun Article.toLikedEntity(likedAt: Long): LikedArticleEntity = LikedArticleEntity(
    id = id, title = title, description = description, content = content, url = url,
    imageUrl = imageUrl, sourceId = sourceId, sourceName = sourceName,
    sourceIconUrl = sourceIconUrl, category = category,
    publishedAt = publishedAt.toEpochMilli(), isBreaking = isBreaking,
    popularity = popularity, likeCount = likeCount, commentCount = commentCount,
    likedAt = likedAt,
)

fun LikedArticleEntity.toDomain(): Article = Article(
    id = id, title = title, description = description, content = content, url = url,
    imageUrl = imageUrl, sourceId = sourceId, sourceName = sourceName,
    sourceIconUrl = sourceIconUrl, category = category,
    publishedAt = Instant.ofEpochMilli(publishedAt), isBreaking = isBreaking,
    popularity = popularity, likeCount = likeCount, commentCount = commentCount,
)

fun SourceDto.toDomain(): Source = Source(
    id = source_id,
    name = name,
    iconUrl = icon_url,
    bannerUrl = banner_url,
    description = description.orEmpty(),
    category = category.orEmpty(),
    followersCount = followers ?: 0L,
)
