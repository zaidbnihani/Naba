package com.nba.plus.domain.repository

import androidx.paging.PagingData
import com.nba.plus.domain.model.Article
import kotlinx.coroutines.flow.Flow

/** مواصفات التغذية المطلوبة من المستودع. */
data class FeedSpec(
    val tab: com.nba.plus.domain.model.FeedTab? = null,
    val categoryId: String? = null,
    val sourceId: String? = null,
    val forYouCategories: Set<String> = emptySet(),
    val forYouSources: Set<String> = emptySet(),
)

interface NewsRepository {

    /** تدفق مُرقّم (Paging 3) للتغذية المطلوبة مع كشف التكرار. */
    fun pagedFeed(spec: FeedSpec): Flow<PagingData<Article>>

    /** جلب صفحة واحدة (تستخدمها مصادر الترقيم). */
    suspend fun fetchPage(spec: FeedSpec, page: Int): List<Article>

    /** بحث فوري عن استعلام. */
    suspend fun search(query: String): List<Article>

    /** مقال واحد بالمعرّف (ذاكرة مؤقتة ثم الشبكة). */
    suspend fun getArticle(id: String): Article?

    /** أخبار ذات صلة بمقال (نفس الفئة أو المصدر). */
    suspend fun getRelated(article: Article, limit: Int = 8): List<Article>

    /** المقالات المخزّنة محليًا (عرض دون اتصال). */
    fun observeCached(spec: FeedSpec = FeedSpec()): Flow<List<Article>>

    /** عمليات البحث الأخيرة. */
    fun observeRecentSearches(): Flow<List<String>>

    suspend fun recordSearch(query: String)

    suspend fun clearRecentSearches()

    /** المواضيع الرائجة. */
    suspend fun getTrendingTopics(): List<String>
}
