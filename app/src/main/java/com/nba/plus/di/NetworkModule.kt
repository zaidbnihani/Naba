package com.nba.plus.di

import com.nba.plus.data.remote.ApiConfig
import com.nba.plus.data.remote.MockNewsInterceptor
import com.nba.plus.data.remote.NewsApiService
import com.nba.plus.data.remote.NewsDataDirectInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        newsDataDirectInterceptor: NewsDataDirectInterceptor,
        mockInterceptor: MockNewsInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            // الترتيب: اتصال مباشر بـ NewsData (إن وُضع المفتاح) ثم البيانات المدمجة
            .addInterceptor(newsDataDirectInterceptor)
            .addInterceptor(mockInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    /**
     * TODO(مهم): Retrofit يستهدف Supabase Edge Function — وليس NewsData.io.
     * غيّر [ApiConfig.BASE_URL] فقط (انظر README) دون أي تعديل هنا.
     */
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideNewsApiService(retrofit: Retrofit): NewsApiService =
        retrofit.create(NewsApiService::class.java)
}
