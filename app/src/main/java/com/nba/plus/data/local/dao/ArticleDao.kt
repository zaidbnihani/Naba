package com.nba.plus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nba.plus.data.local.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(articles: List<ArticleEntity>)

    @Query("SELECT * FROM articles ORDER BY publishedAt DESC LIMIT :limit")
    fun observeLatest(limit: Int = 60): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE category = :categoryId ORDER BY publishedAt DESC LIMIT :limit")
    fun observeByCategory(categoryId: String, limit: Int = 60): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE sourceId = :sourceId ORDER BY publishedAt DESC LIMIT :limit")
    fun observeBySource(sourceId: String, limit: Int = 60): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ArticleEntity?

    @Query("SELECT * FROM articles WHERE category = :categoryId AND id != :excludeId ORDER BY publishedAt DESC LIMIT :limit")
    suspend fun relatedByCategory(categoryId: String, excludeId: String, limit: Int): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE sourceId = :sourceId AND id != :excludeId ORDER BY publishedAt DESC LIMIT :limit")
    suspend fun relatedBySource(sourceId: String, excludeId: String, limit: Int): List<ArticleEntity>

    @Query("DELETE FROM articles WHERE fetchedAt < :threshold")
    suspend fun pruneOlderThan(threshold: Long)
}
