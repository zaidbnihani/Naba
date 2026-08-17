package com.nba.plus.di

import com.nba.plus.data.preferences.RealPersonalizationRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNewsRepository(impl: NewsRepositoryImpl): NewsRepository

    @Binds
    @Singleton
    abstract fun bindSourcesRepository(impl: SourcesRepositoryImpl): SourcesRepository

    @Binds
    @Singleton
    abstract fun bindCategoriesRepository(impl: CategoriesRepositoryImpl): CategoriesRepository

    @Binds
    @Singleton
    abstract fun bindSavedArticlesRepository(impl: SavedArticlesRepositoryImpl): SavedArticlesRepository

    @Binds
    @Singleton
    abstract fun bindInteractionsRepository(impl: InteractionsRepositoryImpl): InteractionsRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesDataStore): UserPreferencesRepository

    /** تنفيذ التخصيص بالذكاء الاصطناعي عبر Gemini 3.7 Flash. */
    @Binds
    @Singleton
    abstract fun bindPersonalizationRepository(impl: RealPersonalizationRepository): PersonalizationRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideDedupDetector(): DedupDetector = DedupDetector(similarityThreshold = 0.75)
}
