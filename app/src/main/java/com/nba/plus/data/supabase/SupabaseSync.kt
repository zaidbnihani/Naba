package com.nba.plus.data.supabase

import io.github.jan.tennert.supabase.postgrest.Postgrest
import io.github.jan.tennert.supabase.SupabaseClient
import io.github.jan.tennert.supabase.gotrue.Auth
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * إعداد Supabase.
 *
 * TODO(مهم): بعد إنشاء مشروعك في Supabase:
 *  1) ضع رابط المشروع والمفتاح العام (anon key) أدناه.
 *  2) نفّذ سكربت SQL الموجود في README لإنشاء الجداول وسياسات RLS.
 *  3) فعّل Anonymous Sign-ins من Authentication ← Providers.
 * طالما لم تُستبدل القيم الوهمية، تعمل كل مزامنات Supabase بصمت دون أثر.
 */
object SupabaseConfig {
    const val SUPABASE_URL = "https://tbnwlsucgfcpwajerjuw.supabase.co"
    const val SUPABASE_ANON_KEY = "sb_publishable_7Ag4mo1vX7jpLvOxeUV_wQ_w4UHz-9Q"

    val isConfigured: Boolean
        get() = SUPABASE_URL.isNotBlank() &&
            !SUPABASE_URL.contains("YOUR-SUPABASE") &&
            SUPABASE_ANON_KEY.isNotBlank() &&
            !SUPABASE_ANON_KEY.startsWith("YOUR-")
}

/** صفوف جداول Supabase (مطابقة للسكربت في README). */
@Serializable
data class FollowedSourceRow(
    val user_id: String,
    val source_id: String,
)

@Serializable
data class FollowedCategoryRow(
    val user_id: String,
    val category_id: String,
)

@Serializable
data class SavedArticleRow(
    val user_id: String,
    val article_id: String,
    val title: String,
    val url: String,
    val image_url: String? = null,
    val saved_at: Long,
)

/**
 * مزامنة أحادية الاتجاه (الجهاز ← السحابة) للمتابعات والمحفوظات.
 * كل الاستدعاءات مغلّفة بأمان: أي فشل لا يعطّل التطبيق.
 */
@Singleton
class SupabaseSync @Inject constructor(
    private val client: SupabaseClient,
) {

    suspend fun currentUserId(): String? = runCatching {
        client.auth.currentSessionOrNull()?.user?.id
    }.getOrNull()

    suspend fun pushFollowedSource(userId: String, sourceId: String, followed: Boolean) {
        if (!SupabaseConfig.isConfigured) return
        runCatching {
            if (followed) {
                client.postgrest["followed_sources"].insert(FollowedSourceRow(userId, sourceId))
            } else {
                client.postgrest["followed_sources"].delete {
                    filter {
                        eq("user_id", userId)
                        eq("source_id", sourceId)
                    }
                }
            }
        }
    }

    suspend fun pushFollowedCategory(userId: String, categoryId: String, followed: Boolean) {
        if (!SupabaseConfig.isConfigured) return
        runCatching {
            if (followed) {
                client.postgrest["followed_categories"].insert(FollowedCategoryRow(userId, categoryId))
            } else {
                client.postgrest["followed_categories"].delete {
                    filter {
                        eq("user_id", userId)
                        eq("category_id", categoryId)
                    }
                }
            }
        }
    }

    suspend fun pushSavedArticle(
        userId: String,
        articleId: String,
        title: String,
        url: String,
        imageUrl: String?,
        saved: Boolean,
    ) {
        if (!SupabaseConfig.isConfigured) return
        runCatching {
            if (saved) {
                client.postgrest["saved_articles"].insert(
                    SavedArticleRow(
                        user_id = userId,
                        article_id = articleId,
                        title = title,
                        url = url,
                        image_url = imageUrl,
                        saved_at = System.currentTimeMillis(),
                    )
                )
            } else {
                client.postgrest["saved_articles"].delete {
                    filter {
                        eq("user_id", userId)
                        eq("article_id", articleId)
                    }
                }
            }
        }
    }
}
