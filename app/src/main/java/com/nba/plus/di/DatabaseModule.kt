package com.nba.plus.di

import android.content.Context
import androidx.room.Room
import com.nba.plus.data.local.NbaDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NbaDatabase =
        Room.databaseBuilder(context, NbaDatabase::class.java, "nba_plus.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideArticleDao(db: NbaDatabase) = db.articleDao()

    @Provides
    fun provideSavedArticlesDao(db: NbaDatabase) = db.savedArticlesDao()

    @Provides
    fun provideLikedArticlesDao(db: NbaDatabase) = db.likedArticlesDao()

    @Provides
    fun provideFollowsDao(db: NbaDatabase) = db.followsDao()

    @Provides
    fun provideSeenArticlesDao(db: NbaDatabase) = db.seenArticlesDao()

    @Provides
    fun provideRecentSearchesDao(db: NbaDatabase) = db.recentSearchesDao()
}
