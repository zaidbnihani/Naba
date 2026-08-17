package com.nba.plus.data.remote

import com.nba.plus.data.remote.dto.NewsResponseDto
import com.nba.plus.data.remote.dto.SourcesResponseDto
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * يعترض نداءات الشبكة أثناء «الوضع التجريبي» (قبل ربط رابط الدالة الحقيقية)
 * ويرجّع بيانات عربية مدمجة من assets/mock بشكل مطابق لعقد الاستجابة،
 * مع محاكاة زمن استجابة الشبكة. عند استبدال BASE_URL يتوقف الاعتراض تلقائيًا.
 */
@Singleton
class MockNewsInterceptor @Inject constructor(
    private val mockDataSource: MockDataSource,
) : Interceptor {

    private val json = Json { encodeDefaults = false }
    private val contentType = "application/json; charset=utf-8".toMediaType()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (!ApiConfig.isDemoMode) {
            return chain.proceed(request)
        }

        // محاكاة زمن الشبكة
        try {
            Thread.sleep(350L + Random.nextLong(550))
        } catch (_: InterruptedException) {
        }

        val url = request.url
        val path = url.pathSegments.lastOrNull().orEmpty()

        val body = when (path) {
            "sources" -> json.encodeToString(
                SourcesResponseDto.serializer(),
                mockDataSource.getSources(),
            )
            else -> {
                val page = url.queryParameter("page")?.toIntOrNull() ?: 1
                val category = url.queryParameter("category")
                val source = url.queryParameter("source")
                val query = url.queryParameter("q")
                json.encodeToString(
                    NewsResponseDto.serializer(),
                    mockDataSource.getNews(page, category, source, query),
                )
            }
        }

        return Response.Builder()
            .request(request)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(200)
            .message("OK (mock)")
            .body(body.toResponseBody(contentType))
            .build()
    }
}
