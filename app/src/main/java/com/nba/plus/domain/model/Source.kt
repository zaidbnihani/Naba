package com.nba.plus.domain.model

/** مصدر إخباري. */
data class Source(
    val id: String,
    val name: String,
    val iconUrl: String?,
    val bannerUrl: String?,
    val description: String,
    val category: String,
    val followersCount: Long,
)

/** فئة/مجال أخبار. */
data class NewsCategory(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val imageUrl: String,
)
