package com.nba.plus.data.remote

import android.content.Context
import com.nba.plus.data.remote.dto.ArticleDto
import com.nba.plus.data.remote.dto.NewsResponseDto
import com.nba.plus.data.remote.dto.SourceDto
import com.nba.plus.data.remote.dto.SourcesResponseDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مصدر البيانات التجريبية: يقرأ JSON عربيًا واقعيًا من assets/mock
 * ويحاكي سلوك الخادم (ترقيم + تصفية + بحث).
 */
@Singleton
class MockDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile private var articlesCache: List<ArticleDto>? = null
    @Volatile private var sourcesCache: List<SourceDto>? = null

    private fun loadArticles(): List<ArticleDto> =
        articlesCache ?: readAsset("mock/news_all.json").let { text ->
            json.decodeFromString(NewsResponseDto.serializer(), text).results
                .also { articlesCache = it }
        }

    private fun loadSources(): List<SourceDto> =
        sourcesCache ?: readAsset("mock/sources.json").let { text ->
            json.decodeFromString(SourcesResponseDto.serializer(), text).sources
                .also { sourcesCache = it }
        }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    fun findArticle(id: String): ArticleDto? = loadArticles().firstOrNull { it.article_id == id }

    fun trending(): List<String> = listOf(
        "الأردن", "الذكاء الاصطناعي", "أسعار النفط", "دوري أبطال أوروبا",
        "السيارات الكهربائية", "البورصة", "المناخ", "منتخب الأردن",
    )

    /**
     * يحاكي GET /news مع الترقيم والتصفية.
     */
    fun getNews(
        page: Int,
        category: String?,
        source: String?,
        query: String?,
        pageSize: Int = PAGE_SIZE,
    ): NewsResponseDto {
        val categories = category?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
        val sources = source?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
        val q = query?.trim()

        var items = loadArticles().asSequence()
        if (!categories.isNullOrEmpty()) items = items.filter { a -> a.category.orEmpty().any { it in categories } }
        if (!sources.isNullOrEmpty()) items = items.filter { a -> a.source_id in sources }
        if (!q.isNullOrEmpty()) {
            val needle = q.lowercase()
            items = items.filter { a ->
                a.title.lowercase().contains(needle) ||
                    a.description.orEmpty().lowercase().contains(needle) ||
                    a.source_name.orEmpty().lowercase().contains(needle)
            }
        }
        // الأحدث أولًا: أصغر عمرًا (بالدقائق) في المقدمة
        val sorted = items
            .sortedBy { it.age_minutes ?: Long.MAX_VALUE }
            .toList()
        val fromIndex = (page - 1) * pageSize
        val pageItems = if (fromIndex >= sorted.size) emptyList() else sorted.drop(fromIndex).take(pageSize)
        val hasNext = fromIndex + pageSize < sorted.size
        return NewsResponseDto(
            totalResults = sorted.size,
            results = pageItems,
            nextPage = if (hasNext) page + 1 else null,
        )
    }

    fun getSources(): SourcesResponseDto = SourcesResponseDto(sources = loadSources())

    suspend fun warmUp() {
        withContext(Dispatchers.IO) {
            loadArticles()
            loadSources()
        }
    }

    companion object {
        const val PAGE_SIZE = 10
    }
}
