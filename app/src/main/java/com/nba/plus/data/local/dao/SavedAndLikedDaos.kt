package com.nba.plus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nba.plus.data.local.entity.LikedArticleEntity
import com.nba.plus.data.local.entity.SavedArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedArticlesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(article: SavedArticleEntity)

    @Query("DELETE FROM saved_articles WHERE id = :articleId")
    suspend fun deleteById(articleId: String)

    @Query("SELECT * FROM saved_articles ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<SavedArticleEntity>>

    @Query("SELECT id FROM saved_articles")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT * FROM saved_articles WHERE id = :articleId LIMIT 1")
    suspend fun findById(articleId: String): SavedArticleEntity?
}

@Dao
interface LikedArticlesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(article: LikedArticleEntity)

    @Query("DELETE FROM liked_articles WHERE id = :articleId")
    suspend fun deleteById(articleId: String)

    @Query("SELECT * FROM liked_articles ORDER BY likedAt DESC")
    fun observeAll(): Flow<List<LikedArticleEntity>>

    @Query("SELECT id FROM liked_articles")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT * FROM liked_articles WHERE id = :articleId LIMIT 1")
    suspend fun findById(articleId: String): LikedArticleEntity?
}
