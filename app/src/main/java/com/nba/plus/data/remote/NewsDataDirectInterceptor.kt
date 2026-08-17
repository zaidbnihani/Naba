package com.nba.plus.data.remote

import com.nba.plus.BuildConfig
import com.nba.plus.data.remote.dto.ArticleDto
import com.nba.plus.data.remote.dto.NewsResponseDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * وضع الاتصال المباشر بـ NewsData.io (للتطوير والتجربة فقط).
 *
 * يعمل حصريًا عندما:
 *  1) وُضع مفتاح NewsData في secrets.properties (يصل عبر BuildConfig)، و
 *  2) لم يُستبدل رابط Supabase Edge Function بعد (ApiConfig.isDemoMode).
 */
@Singleton
class NewsDataDirectInterceptor @Inject constructor() : Interceptor {

    private val json = Json { ignoreUnknownKeys = true }
    private val contentType = "application/json; charset=utf-8".toMediaType()

    private val pageTokens = ConcurrentHashMap<String, String>()

    private val upstreamClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val enabled: Boolean
        get() = BuildConfig.NEWSDATA_API_KEY.isNotBlank() && ApiConfig.isDemoMode

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!enabled) return chain.proceed(request)
        if (request.url.pathSegments.lastOrNull() != "news") return chain.proceed(request)

        val page = request.url.queryParameter("page")?.toIntOrNull() ?: 1
        val category = request.url.queryParameter("category")
        val source = request.url.queryParameter("source")
        val query = request.url.queryParameter("q")

        return try {
            val responseDto = fetchNews(page, category, source, query)
            val payload = json.encodeToString(
                NewsResponseDto.serializer(),
                responseDto,
            )
            Response.Builder()
                .request(request)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(200)
                .message("OK (newsdata)")
                .body(payload.toResponseBody(contentType))
                .build()
        } catch (_: Exception) {
            chain.proceed(request)
        }
    }

    private fun fetchNews(
        page: Int,
        category: String?,
        source: String?,
        query: String?,
    ): NewsResponseDto {
        val cacheKey = "${category.orEmpty()}|${source.orEmpty()}|${query.orEmpty()}"

        val urlBuilder = "https://newsdata.io/api/1/news".toHttpUrl().newBuilder()
            .addQueryParameter("apikey", BuildConfig.NEWSDATA_API_KEY)
            .addQueryParameter("language", "ar")
            .addQueryParameter("removeduplicate", "1")

        if (page > 1) {
            val token = pageTokens["$cacheKey-p$page"]
            if (!token.isNullOrBlank()) {
                urlBuilder.addQueryParameter("page", token)
            }
        }

        topicOf(category)?.let { urlBuilder.addQueryParameter("topic", it) }
        domainOf(source)?.let { urlBuilder.addQueryParameter("domain", it) }
        if (!query.isNullOrBlank()) urlBuilder.addQueryParameter("q", query)

        val body = upstreamClient.newCall(Request.Builder().url(urlBuilder.build()).build())
            .execute()
            .use { it.body?.string().orEmpty() }

        val root = json.parseToJsonElement(body).jsonObject
        val resultsArray = root["results"] as? JsonArray
            ?: throw IllegalStateException("NewsData error: ${root["results"]}")

        val articles = resultsArray.mapNotNull { element ->
            val o = element as? JsonObject ?: return@mapNotNull null
            val title = o.str("title").orEmpty()
            if (title.isBlank()) return@mapNotNull null
            val sourceId = o.str("source_id")
            ArticleDto(
                article_id = o.str("article_id"),
                title = title,
                description = o.str("description"),
                content = o.str("content"),
                link = o.str("link"),
                image_url = o.str("image_url"),
                source_id = sourceId,
                source_name = sourceId?.let { sourceDisplayName(it) },
                source_icon = null,
                category = o.stringList("topic").ifEmpty { o.stringList("category") },
                pubDate = o.str("pubDate"),
                is_breaking = false,
                popularity = (30..150).random(),
                like_count = 0,
                comment_count = 0,
            )
        }

        val nextToken = (root["nextPage"] as? JsonPrimitive)?.contentOrNull
        if (!nextToken.isNullOrBlank()) {
            pageTokens["$cacheKey-p${page + 1}"] = nextToken
        }

        return NewsResponseDto(
            status = "success",
            totalResults = (root["totalResults"] as? JsonPrimitive)?.intOrNull ?: articles.size,
            results = articles,
            nextPage = if (!nextToken.isNullOrBlank()) page + 1 else null,
        )
    }

    /** فئات التطبيق → مواضيع NewsData */
    private val topicMap = mapOf(
        "politics" to "politics",
        "sports" to "sports",
        "technology" to "technology",
        "health" to "health",
        "business" to "business",
        "science" to "science",
        "travel" to "travel",
        "food" to "food",
        "ai" to "technology",
        "automotive" to "technology",
        "entertainment" to "entertainment",
        "world" to "world",
    )

    private fun topicOf(category: String?): String? {
        if (category.isNullOrBlank()) return null
        return category.split(',')
            .mapNotNull { topicMap[it.trim().lowercase()] }
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?.joinToString(",")
    }

    /** معرّفات كتالوج المصادر → نطاقات حقيقية في NewsData. */
    private val sourceDomains = mapOf(
        "aljazeera" to "aljazeera.net",
        "petra" to "petra.gov.jo",
        "addustour" to "addustour.com",
        "alghad" to "alghad.com",
        "kooora" to "kooora.com",
        "techarabi" to "techarabi.com",
        "reuters" to "reuters.com",
        "skynews" to "skynewsarabia.com",
        "alarabiya" to "alarabiya.net",
        "ammon" to "ammonnews.net",
        "saraya" to "sarayanews.com",
        "khaberni" to "khaberni.com",
    )

    private fun domainOf(source: String?): String? {
        if (source.isNullOrBlank()) return null
        return source.split(',')
            .mapNotNull { token ->
                val t = token.trim().lowercase()
                sourceDomains[t] ?: t.takeIf { it.contains('.') }
            }
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?.joinToString(",")
    }

    private val sourceNamesByDomain = mapOf(
        "aljazeera.net" to "الجزيرة نت",
        "petra.gov.jo" to "بترا",
        "addustour.com" to "الدستور",
        "alghad.com" to "الغد",
        "kooora.com" to "كورة",
        "techarabi.com" to "عالم التقنية",
        "reuters.com" to "رويترز عربي",
        "skynewsarabia.com" to "سكاي نيوز عربية",
        "alarabiya.net" to "العربية",
        "jordanjow" to "جوردان جو",
        "ammonnews.net" to "عمون نيوز",
        "sarayanews.com" to "سرايا نيوز",
        "khaberni.com" to "خبرني",
        "factjo.com" to "فكت جو",
    )

    private fun sourceDisplayName(sourceId: String): String =
        sourceNamesByDomain[sourceId.trim().lowercase()] ?: sourceId

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.stringList(key: String): List<String> =
        ((this[key] as? JsonArray)
            ?.filterIsInstance<JsonPrimitive>()
            ?.mapNotNull { it.contentOrNull }
            ?: emptyList())
}
