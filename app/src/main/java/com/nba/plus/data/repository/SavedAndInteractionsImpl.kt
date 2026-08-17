package com.nba.plus.data.repository

import com.nba.plus.data.local.dao.LikedArticlesDao
import com.nba.plus.data.local.dao.SavedArticlesDao
import com.nba.plus.data.local.toDomain
import com.nba.plus.data.local.toLikedEntity
import com.nba.plus.data.local.toSavedEntity
import com.nba.plus.data.supabase.SupabaseSync
import com.nba.plus.di.IoDispatcher
import com.nba.plus.domain.model.Article
import com.nba.plus.domain.repository.InteractionsRepository
import com.nba.plus.domain.repository.SavedArticlesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedArticlesRepositoryImpl @Inject constructor(
    private val savedArticlesDao: SavedArticlesDao,
    private val supabaseSync: SupabaseSync,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SavedArticlesRepository {

    override fun observeSaved(): Flow<List<Article>> =
        savedArticlesDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeSavedIds(): Flow<Set<String>> =
        savedArticlesDao.observeIds().map { it.toSet() }

    override suspend fun toggleSaved(article: Article): Boolean = withContext(ioDispatcher) {
        val existing = savedArticlesDao.findById(article.id)
        if (existing != null) {
            savedArticlesDao.deleteById(article.id)
            pushRemote(article, saved = false)
            false
        } else {
            savedArticlesDao.upsert(article.toSavedEntity(System.currentTimeMillis()))
            pushRemote(article, saved = true)
            true
        }
    }

    override suspend fun isSaved(articleId: String): Boolean =
        savedArticlesDao.findById(articleId) != null

    private suspend fun pushRemote(article: Article, saved: Boolean) {
        supabaseSync.currentUserId()?.let { uid ->
            supabaseSync.pushSavedArticle(
                userId = uid,
                articleId = article.id,
                title = article.title,
                url = article.url,
                imageUrl = article.imageUrl,
                saved = saved,
            )
        }
    }
}

@Singleton
class InteractionsRepositoryImpl @Inject constructor(
    private val likedArticlesDao: LikedArticlesDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : InteractionsRepository {

    override fun observeLikedArticleIds(): Flow<Set<String>> =
        likedArticlesDao.observeIds().map { it.toSet() }

    override fun observeLiked(): Flow<List<Article>> =
        likedArticlesDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun toggleLike(article: Article): Boolean = withContext(ioDispatcher) {
        val existing = likedArticlesDao.findById(article.id)
        if (existing != null) {
            likedArticlesDao.deleteById(article.id)
            false
        } else {
            likedArticlesDao.upsert(article.toLikedEntity(System.currentTimeMillis()))
            true
        }
    }
}
