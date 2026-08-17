package com.nba.plus.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.nba.plus.data.ArticleCache
import com.nba.plus.data.local.dao.ArticleDao
import com.nba.plus.data.local.dao.RecentSearchesDao
import com.nba.plus.data.local.dao.SeenArticlesDao
import com.nba.plus.data.local.entity.RecentSearchEntity
import com.nba.plus.data.local.entity.SeenArticleEntity
import com.nba.plus.data.local.toDomain
import com.nba.plus.data.local.toEntity
import com.nba.plus.data.remote.MockDataSource
import com.nba.plus.data.remote.NewsApiService
import com.nba.plus.data.remote.dto.ArticleDto
import com.nba.plus.di.IoDispatcher
import com.nba.plus.domain.model.Article
import com.nba.plus.domain.model.FeedTab
import com.nba.plus.domain.repository.FeedSpec
import com.nba.plus.domain.repository.NewsRepository
import com.nba.plus.domain.util.DedupDetector
import com.nba.plus.domain.util.TitleNormalizer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.nba.plus.data.local.dao.LikedArticlesDao
import com.nba.plus.data.preferences.UserPreferencesDataStore
import com.nba.plus.data.remote.GeminiPersonalizationEngine
import kotlinx.coroutines.flow.first

@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val api: NewsApiService,
    private val articleDao: ArticleDao,
    private val seenArticlesDao: SeenArticlesDao,
    private val likedArticlesDao: LikedArticlesDao,
    private val recentSearchesDao: RecentSearchesDao,
    private val mockDataSource: MockDataSource,
    private val articleCache: ArticleCache,
    private val dedupDetector: DedupDetector,
    private val geminiPersonalizationEngine: GeminiPersonalizationEngine,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NewsRepository {

    override fun pagedFeed(spec: FeedSpec): Flow<PagingData<Article>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = PAGE_SIZE,
            prefetchDistance = 3,
            enablePlaceholders = false,
            maxSize = 120,
        ),
        pagingSourceFactory = { FeedPagingSource(spec) },
    ).flow

    /** مصدر ترقيم شبكي مع إزالة التكرار قبل العرض. */
    inner class FeedPagingSource(
        private val spec: FeedSpec,
    ) : PagingSource<Int, Article>() {

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Article> = try {
            val page = params.key ?: 1
            val items = fetchPage(spec, page)
            LoadResult.Page(
                data = items,
                prevKey = if (page <= 1) null else page - 1,
                nextKey = if (items.size < PAGE_SIZE) null else page + 1,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }

        override fun getRefreshKey(state: PagingState<Int, Article>): Int? {
            val anchor = state.anchorPosition ?: return null
            val page = state.closestPageToPosition(anchor)
            return page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
        }
    }

    override suspend fun fetchPage(spec: FeedSpec, page: Int): List<Article> =
        withContext(ioDispatcher) {
            val response = api.getNews(
                page = page,
                category = spec.categoryId
                    ?: spec.forYouCategories.joinToString(",").ifEmpty { null },
                source = spec.sourceId
                    ?: spec.forYouSources.joinToString(",").ifEmpty { null },
            )
            val now = Instant.now()
            val articles = applyTab(response.results.map { it.toDomain(now) }, spec)

            // إزالة التكرار مقابل ما ظهر سابقًا (محليًا وفي هذه الجلسة)
            val seen = seenArticlesDao.loadRecent()
                .map { DedupDetector.SeenArticle(it.articleId, it.normalizedTitle, it.normalizedUrl) }
                .toMutableList()
            val unique = dedupDetector.filter(articles, seen)

            seenArticlesDao.insertAll(
                unique.map {
                    SeenArticleEntity(
                        articleId = it.id,
                        normalizedTitle = TitleNormalizer.normalize(it.title),
                        normalizedUrl = TitleNormalizer.normalizeUrl(it.url),
                    )
                }
            )
            articleDao.upsertAll(unique.map { it.toEntity() })
            articleDao.pruneOlderThan(System.currentTimeMillis() - CACHE_TTL_MS)
            unique.forEach(articleCache::put)
            val rankedArticles = if (spec.tab == FeedTab.FOR_YOU && userPreferencesDataStore.preferences.first().aiPersonalizationEnabled && geminiPersonalizationEngine.isAvailable) {
                val likedTitles = runCatching {
                    likedArticlesDao.observeAll().first().map { it.title }
                }.getOrDefault(emptyList())
                geminiPersonalizationEngine.rankArticles(
                    articles = unique,
                    userLikedTitles = likedTitles,
                    followedCategories = spec.forYouCategories,
                    followedSources = spec.forYouSources,
                )
            } else {
                unique
            }

            rankedArticles
        }

    private fun applyTab(articles: List<Article>, spec: FeedSpec): List<Article> =
        when (spec.tab) {
            FeedTab.MOST_READ -> articles.sortedByDescending { it.popularity }
            FeedTab.BREAKING -> articles.filter { it.isBreaking }
                .ifEmpty { articles.sortedByDescending { it.publishedAt } }
            FeedTab.LATEST, FeedTab.FOR_YOU, null -> articles.sortedByDescending { it.publishedAt }
        }

    override suspend fun search(query: String): List<Article> = withContext(ioDispatcher) {
        val all = mutableListOf<ArticleDto>()
        var page = 1
        do {
            val response = api.getNews(page = page, query = query)
            all += response.results
            page = response.nextPage ?: break
        } while (all.size < MAX_SEARCH_RESULTS && page <= MAX_SEARCH_PAGES)

        val now = Instant.now()
        all.map { it.toDomain(now) }
            .sortedByDescending { it.publishedAt }
            .also { it.forEach(articleCache::put) }
    }

    override suspend fun getArticle(id: String): Article? = withContext(ioDispatcher) {
        articleCache.get(id)
            ?: articleDao.findById(id)?.toDomain()
            ?.also { articleCache.put(it) }
            ?: runCatching { mockDataSource.findArticle(id)?.let { dto -> dto.toDomain() } }.getOrNull()
    }

    override suspend fun getRelated(article: Article, limit: Int): List<Article> =
        withContext(ioDispatcher) {
            val byCategory = articleDao.relatedByCategory(article.category, article.id, limit)
            val bySource = articleDao.relatedBySource(article.sourceId, article.id, limit)
            val result = (byCategory + bySource)
                .distinctBy { it.id }
                .take(limit)
                .map { it.toDomain() }
            if (result.isNotEmpty()) {
                result
            } else {
                runCatching {
                    mockDataSource.getNews(1, article.category, article.sourceId, null)
                        .results
                        .map { it.toDomain() }
                        .filter { it.id != article.id }
                        .take(limit)
                }.getOrDefault(emptyList())
            }
        }

    override fun observeCached(spec: FeedSpec): Flow<List<Article>> {
        val flow = when {
            spec.categoryId != null -> articleDao.observeByCategory(spec.categoryId)
            spec.sourceId != null -> articleDao.observeBySource(spec.sourceId)
            else -> articleDao.observeLatest()
        }
        return flow.map { list -> list.map { it.toDomain() } }
    }

    override fun observeRecentSearches(): Flow<List<String>> = recentSearchesDao.observeRecent()

    override suspend fun recordSearch(query: String) {
        recentSearchesDao.upsert(RecentSearchEntity(query.trim()))
    }

    override suspend fun clearRecentSearches() = recentSearchesDao.clearAll()

    override suspend fun getTrendingTopics(): List<String> = mockDataSource.trending()

    companion object {
        const val PAGE_SIZE = MockDataSource.PAGE_SIZE
        const val MAX_SEARCH_PAGES = 5
        const val MAX_SEARCH_RESULTS = 60
        private const val CACHE_TTL_MS = 72L * 24 * 60 * 60 * 1000 // 3 أيام
    }
}
