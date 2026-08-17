package com.nba.plus.data.remote

import com.nba.plus.data.remote.dto.NewsResponseDto
import com.nba.plus.data.remote.dto.SourcesResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * خدمة Retrofit التي تضرب Supabase Edge Function (وليس NewsData.io مباشرة).
 * جميع الاستدعاءات تمر عبر [com.nba.plus.data.remote.ApiConfig.BASE_URL].
 */
interface NewsApiService {

    @GET("news")
    suspend fun getNews(
        @Query("page") page: Int = 1,
        @Query("category") category: String? = null,
        @Query("source") source: String? = null,
        @Query("q") query: String? = null,
        @Query("country") country: String? = "jo",
        @Query("language") language: String? = "ar",
        @Query("removeduplicate") removeDuplicate: Int? = 1,
    ): NewsResponseDto

    @GET("sources")
    suspend fun getSources(): SourcesResponseDto
}
