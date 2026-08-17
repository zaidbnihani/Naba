package com.nba.plus.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * نماذل نقل البيانات المطابقة لعقد Supabase Edge Function
 * (المستوحى من شكل استجابة NewsData.io). انظر README.
 */
@Serializable
data class NewsResponseDto(
    val status: String = "success",
    val totalResults: Int = 0,
    val results: List<ArticleDto> = emptyList(),
    val nextPage: Int? = null,
)

@Serializable
data class ArticleDto(
    val article_id: String? = null,
    val title: String = "",
    val description: String? = null,
    val content: String? = null,
    val link: String? = null,
    val image_url: String? = null,
    val source_id: String? = null,
    val source_name: String? = null,
    val source_icon: String? = null,
    val category: List<String>? = null,
    /** تاريخ النشر ISO-8601 (مثل 2026-08-17 09:30:00). */
    val pubDate: String? = null,
    val is_breaking: Boolean? = null,
    val popularity: Int? = null,
    val like_count: Int? = null,
    val comment_count: Int? = null,
    /** حقل خاص بالبيانات التجريبية: عمر الخبر بالدقائق. */
    val age_minutes: Long? = null,
)

@Serializable
data class SourcesResponseDto(
    val sources: List<SourceDto> = emptyList(),
)

@Serializable
data class SourceDto(
    val source_id: String = "",
    val name: String = "",
    val icon_url: String? = null,
    val banner_url: String? = null,
    val description: String? = null,
    val category: String? = null,
    val followers: Long? = null,
)
