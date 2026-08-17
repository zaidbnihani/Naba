package com.nba.plus.data.remote

import com.nba.plus.BuildConfig
import com.nba.plus.domain.model.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * محرك التخصيص الذكي المعتمد على نموذج Google Gemini 3.7 Flash.
 *
 * يقوم النموذج بتحليل اهتمامات المستخدم وسجل قراءاته وتفاعلاته (الإعجابات والمصادر والمجالات المتابعة)،
 * وإعادة ترتيب المقالات المعروضة في تبويب «لك» وفقًا لدرجة ملاءمتها لاهتماماته الحالية.
 */
@Singleton
class GeminiPersonalizationEngine @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = BuildConfig.GEMINI_API_KEY.trim()

    val isAvailable: Boolean
        get() = apiKey.isNotBlank() && !apiKey.startsWith("YOUR-")

    /**
     * يعيد ترتيب قائمة المقالات باستخدام Gemini 3.7 Flash وفق اهتمامات المستخدم وسجله.
     */
    suspend fun rankArticles(
        articles: List<Article>,
        userLikedTitles: List<String>,
        followedCategories: Set<String>,
        followedSources: Set<String>,
    ): List<Article> = withContext(Dispatchers.IO) {
        if (!isAvailable || articles.size <= 2) {
            return@withContext articles
        }

        try {
            // نأخذ عينة من المقالات لتسريع الاستجابة وتقليل استهلاك الـ Tokens
            val candidates = articles.take(25)
            val candidatesMap = candidates.associateBy { it.id }

            val prompt = buildPrompt(
                candidates = candidates,
                userLikedTitles = userLikedTitles.take(10),
                followedCategories = followedCategories,
                followedSources = followedSources,
            )

            val orderedIds = callGeminiRankApi(prompt)
            if (orderedIds.isEmpty()) return@withContext articles

            val ranked = orderedIds.mapNotNull { candidatesMap[it] }
            val remaining = articles.filter { it.id !in candidatesMap.keys || it !in ranked }

            ranked + remaining
        } catch (_: Exception) {
            // في حال حدوث أي خطأ في الشبكة، نحافظ على الترتيب الأصلي دون أي انقطاع
            articles
        }
    }

    private fun buildPrompt(
        candidates: List<Article>,
        userLikedTitles: List<String>,
        followedCategories: Set<String>,
        followedSources: Set<String>,
    ): String {
        val articlesListStr = candidates.mapIndexed { idx, art ->
            """{"index": ${idx + 1}, "id": "${art.id}", "title": "${art.title.replace("\"", "\\\"")}", "category": "${art.category}", "source": "${art.sourceName}"}"""
        }.joinToString(",\n")

        val userProfileStr = buildString {
            if (userLikedTitles.isNotEmpty()) {
                append("المقالات التي نالت إعجاب المستخدم مؤخرًا:\n")
                userLikedTitles.forEach { append("- ").append(it).append("\n") }
            }
            if (followedCategories.isNotEmpty()) {
                append("المجالات المهتم بها: ").append(followedCategories.joinToString(", ")).append("\n")
            }
            if (followedSources.isNotEmpty()) {
                append("المصادر المتابعة: ").append(followedSources.joinToString(", ")).append("\n")
            }
        }

        return """
أنت محرك ذكي لتخصيص وترتيب الأخبار في تطبيق «نبأ+».
بناءً على اهتمامات المستخدم وسجل إعجابته، رتّب المقالات التالية من الأكثر ملاءمة وفائدة للمستخدم إلى الأقل ملاءمة.

$userProfileStr

قائمة المقالات المرشحة:
[
$articlesListStr
]

المطلوب:
أرجع فقط مصفوفة JSON تحتوي على معرّفات (id) المقالات بالترتيب الأنسب، مثال:
["id1", "id2", "id3"]
لا تضف أي نص آخر أو شرح، فقط JSON Array.
        """.trimIndent()
    }

    private fun callGeminiRankApi(prompt: String): List<String> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val requestBodyJson = """
        {
          "contents": [
            {
              "parts": [
                {
                  "text": ${JsonPrimitive(prompt)}
                }
              ]
            }
          ],
          "generationConfig": {
            "temperature": 0.2,
            "responseMimeType": "application/json"
          }
        }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toRequestBody(mediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) return emptyList()

        val root = json.parseToJsonElement(responseBody).jsonObject
        val text = root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("parts")?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty()

        if (text.isBlank()) return emptyList()

        val parsedArray = runCatching {
            json.parseToJsonElement(text.trim()).jsonArray
        }.getOrNull() ?: return emptyList()

        return parsedArray.mapNotNull {
            (it as? JsonPrimitive)?.contentOrNull
        }
    }
}
