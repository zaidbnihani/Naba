package com.nba.plus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nba.plus.data.local.dao.ArticleDao
import com.nba.plus.data.local.dao.FollowsDao
import com.nba.plus.data.local.dao.LikedArticlesDao
import com.nba.plus.data.local.dao.RecentSearchesDao
import com.nba.plus.data.local.dao.SavedArticlesDao
import com.nba.plus.data.local.dao.SeenArticlesDao
import com.nba.plus.data.local.entity.ArticleEntity
import com.nba.plus.data.local.entity.FollowedCategoryEntity
import com.nba.plus.data.local.entity.FollowedSourceEntity
import com.nba.plus.data.local.entity.LikedArticleEntity
import com.nba.plus.data.local.entity.RecentSearchEntity
import com.nba.plus.data.local.entity.SavedArticleEntity
import com.nba.plus.data.local.entity.SeenArticleEntity

@Database(
    entities = [
        ArticleEntity::class,
        SavedArticleEntity::class,
        LikedArticleEntity::class,
        FollowedSourceEntity::class,
        FollowedCategoryEntity::class,
        SeenArticleEntity::class,
        RecentSearchEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class NbaDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun savedArticlesDao(): SavedArticlesDao
    abstract fun likedArticlesDao(): LikedArticlesDao
    abstract fun followsDao(): FollowsDao
    abstract fun seenArticlesDao(): SeenArticlesDao
    abstract fun recentSearchesDao(): RecentSearchesDao
}
