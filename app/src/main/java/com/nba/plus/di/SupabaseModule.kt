package com.nba.plus.di

import com.nba.plus.data.supabase.SupabaseConfig
import io.github.jan.tennert.supabase.SupabaseClient
import io.github.jan.tennert.supabase.gotrue.Auth
import io.github.jan.tennert.supabase.postgrest.Postgrest
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    /**
     * عميل Supabase للمصادقة (GoTrue) وقاعدة البيانات (Postgrest).
     * القيم الوهمية في [SupabaseConfig] تُستبدل يدويًا — انظر README.
     */
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = SupabaseClient(
        supabaseUrl = SupabaseConfig.SUPABASE_URL,
        supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY,
    ) {
        install(Auth)
        install(Postgrest)
    }
}
