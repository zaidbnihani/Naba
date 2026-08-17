package com.nba.plus.data.repository

import com.nba.plus.data.local.dao.FollowsDao
import com.nba.plus.data.local.entity.FollowedCategoryEntity
import com.nba.plus.data.local.entity.FollowedSourceEntity
import com.nba.plus.data.remote.NewsApiService
import com.nba.plus.data.remote.dto.SourceDto
import com.nba.plus.data.local.toDomain
import com.nba.plus.data.supabase.SupabaseSync
import com.nba.plus.di.IoDispatcher
import com.nba.plus.domain.model.NewsCategory
import com.nba.plus.domain.model.Source
import com.nba.plus.domain.repository.CategoriesRepository
import com.nba.plus.domain.repository.SourcesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourcesRepositoryImpl @Inject constructor(
    private val api: NewsApiService,
    private val followsDao: FollowsDao,
    private val supabaseSync: SupabaseSync,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SourcesRepository {

    @Volatile
    private var cachedSources: List<Source>? = null

    override suspend fun getSourcesGrouped(): Map<String, List<Source>> =
        withContext(ioDispatcher) {
            val sources = cachedSources ?: api.getSources().sources
                .map(SourceDto::toDomain)
                .also { cachedSources = it }
            sources.groupBy { it.category }
        }

    override suspend fun getSource(sourceId: String): Source? = withContext(ioDispatcher) {
        cachedSources?.firstOrNull { it.id == sourceId }
            ?: api.getSources().sources
                .firstOrNull { it.source_id == sourceId }
                ?.toDomain()
            ?.also { source -> cachedSources = (cachedSources.orEmpty() + source).distinctBy { it.id } }
    }

    override fun observeFollowedIds(): Flow<Set<String>> =
        followsDao.observeFollowedSourceIds().map { it.toSet() }

    override suspend fun setFollowed(sourceId: String, followed: Boolean) {
        if (followed) {
            followsDao.followSource(FollowedSourceEntity(sourceId))
        } else {
            followsDao.unfollowSource(sourceId)
        }
        supabaseSync.currentUserId()?.let { uid ->
            supabaseSync.pushFollowedSource(uid, sourceId, followed)
        }
    }

    override suspend fun isFollowed(sourceId: String): Boolean =
        followsDao.isSourceFollowed(sourceId)
}

@Singleton
class CategoriesRepositoryImpl @Inject constructor(
    private val followsDao: FollowsDao,
    private val supabaseSync: SupabaseSync,
) : CategoriesRepository {

    override fun getCategories(): List<NewsCategory> = DefaultCategories.all

    override fun getCategory(categoryId: String): NewsCategory? =
        DefaultCategories.all.firstOrNull { it.id == categoryId }

    override fun observeFollowedIds(): Flow<Set<String>> =
        followsDao.observeFollowedCategoryIds().map { it.toSet() }

    override suspend fun setFollowed(categoryId: String, followed: Boolean) {
        if (followed) {
            followsDao.followCategory(FollowedCategoryEntity(categoryId))
        } else {
            followsDao.unfollowCategory(categoryId)
        }
        supabaseSync.currentUserId()?.let { uid ->
            supabaseSync.pushFollowedCategory(uid, categoryId, followed)
        }
    }

    override suspend fun setFollowedAll(categoryIds: Set<String>) {
        followsDao.replaceFollowedCategories(categoryIds)
        supabaseSync.currentUserId()?.let { uid ->
            categoryIds.forEach { supabaseSync.pushFollowedCategory(uid, it, true) }
        }
    }
}
