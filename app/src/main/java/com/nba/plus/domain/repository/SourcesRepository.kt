package com.nba.plus.domain.repository

import com.nba.plus.domain.model.NewsCategory
import com.nba.plus.domain.model.Source
import kotlinx.coroutines.flow.Flow

interface SourcesRepository {

    /** كل المصادر مجمّعة حسب الفئة. */
    suspend fun getSourcesGrouped(): Map<String, List<Source>>

    suspend fun getSource(sourceId: String): Source?

    fun observeFollowedIds(): Flow<Set<String>>

    suspend fun setFollowed(sourceId: String, followed: Boolean)

    suspend fun isFollowed(sourceId: String): Boolean
}

interface CategoriesRepository {

    fun getCategories(): List<NewsCategory>

    fun getCategory(categoryId: String): NewsCategory?

    fun observeFollowedIds(): Flow<Set<String>>

    suspend fun setFollowed(categoryId: String, followed: Boolean)

    /** تعيين مجموعة كاملة دفعة واحدة (لشاشة الترحيب). */
    suspend fun setFollowedAll(categoryIds: Set<String>)
}
