package com.nba.plus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nba.plus.data.local.entity.FollowedCategoryEntity
import com.nba.plus.data.local.entity.FollowedSourceEntity
import com.nba.plus.data.local.entity.RecentSearchEntity
import com.nba.plus.data.local.entity.SeenArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowsDao {

    // --- المصادر ---
    @Query("SELECT sourceId FROM followed_sources")
    fun observeFollowedSourceIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun followSource(entity: FollowedSourceEntity)

    @Query("DELETE FROM followed_sources WHERE sourceId = :sourceId")
    suspend fun unfollowSource(sourceId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM followed_sources WHERE sourceId = :sourceId)")
    suspend fun isSourceFollowed(sourceId: String): Boolean

    @Query("DELETE FROM followed_sources")
    suspend fun clearFollowedSources()

    // --- الفئات ---
    @Query("SELECT categoryId FROM followed_categories")
    fun observeFollowedCategoryIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun followCategory(entity: FollowedCategoryEntity)

    @Query("DELETE FROM followed_categories WHERE categoryId = :categoryId")
    suspend fun unfollowCategory(categoryId: String)

    @Query("DELETE FROM followed_categories")
    suspend fun clearFollowedCategories()

    @Transaction
    suspend fun replaceFollowedCategories(ids: Set<String>) {
        clearFollowedCategories()
        ids.forEach { followCategory(FollowedCategoryEntity(it)) }
    }
}

@Dao
interface SeenArticlesDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<SeenArticleEntity>)

    @Query("SELECT * FROM seen_articles ORDER BY seenAt DESC LIMIT :limit")
    suspend fun loadRecent(limit: Int = 300): List<SeenArticleEntity>
}

@Dao
interface RecentSearchesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecentSearchEntity)

    @Query("SELECT `query` FROM recent_searches ORDER BY searchedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<String>>

    @Query("DELETE FROM recent_searches")
    suspend fun clearAll()
}
