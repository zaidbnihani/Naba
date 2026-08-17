package com.nba.plus.data.repository

import com.nba.plus.data.local.NbaDatabase
import com.nba.plus.data.preferences.UserPreferencesDataStore
import com.nba.plus.data.supabase.SupabaseConfig
import com.nba.plus.di.ApplicationScope
import com.nba.plus.domain.model.AuthState
import com.nba.plus.domain.repository.AuthRepository
import io.github.jan.tennert.supabase.gotrue.Auth
import io.github.jan.tennert.supabase.gotrue.providers.builtin.Email
import io.github.jan.tennert.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مصادقة Supabase (مجهولة الهوية افتراضيًا أو بريد إلكتروني).
 * في «الوضع التجريبي» (قبل ضبط Supabase) تعمل الجلسة بهوية محلية ثابتة.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
    private val prefsStore: UserPreferencesDataStore,
    private val database: NbaDatabase,
    @ApplicationScope private val appScope: CoroutineScope,
) : AuthRepository {

    private val state = MutableStateFlow(AuthState())
    override val authState: StateFlow<AuthState> = state.asStateFlow()

    init {
        appScope.launch { runCatching { ensureSignedIn() } }
    }

    override suspend fun ensureSignedIn() {
        val saved = prefsStore.getSavedAuthProfile()
        if (saved != null && !saved.isAnonymous) {
            state.value = saved
            return
        }

        if (!SupabaseConfig.isConfigured) {
            val localId = prefsStore.ensureAnonymousId()
            state.value = AuthState(userId = localId, isAnonymous = true)
            return
        }

        val existing = runCatching { client.auth.currentSessionOrNull() }.getOrNull()
        if (existing == null) {
            runCatching { client.auth.signInAnonymously() }
        }
        publishFromSession()
    }

    private suspend fun publishFromSession() {
        val session = runCatching { client.auth.currentSessionOrNull() }.getOrNull()
        val user = session?.user
        val auth = if (user != null) {
            AuthState(
                userId = user.id,
                email = user.email,
                isAnonymous = user.isAnonymous,
            )
        } else {
            // تعذّر الاتصال: هوية محلية حتى لا يتعطل التطبيق
            val localId = prefsStore.ensureAnonymousId()
            AuthState(userId = localId, isAnonymous = true)
        }
        state.value = auth
        if (auth.isSignedIn) {
            prefsStore.saveAuthProfile(
                userId = auth.userId.orEmpty(),
                email = auth.email,
                displayName = auth.displayName,
                photoUrl = auth.photoUrl,
                isAnonymous = auth.isAnonymous,
            )
        }
    }

    override suspend fun signInWithEmail(email: String, password: String) {
        if (SupabaseConfig.isConfigured) {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            publishFromSession()
        } else {
            val userId = "user_${email.hashCode()}"
            val auth = AuthState(
                userId = userId,
                email = email,
                displayName = email.substringBefore("@"),
                isAnonymous = false,
            )
            prefsStore.saveAuthProfile(userId, email, auth.displayName, null, false)
            state.value = auth
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String) {
        if (SupabaseConfig.isConfigured) {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            publishFromSession()
        } else {
            val userId = "user_${email.hashCode()}"
            val auth = AuthState(
                userId = userId,
                email = email,
                displayName = email.substringBefore("@"),
                isAnonymous = false,
            )
            prefsStore.saveAuthProfile(userId, email, auth.displayName, null, false)
            state.value = auth
        }
    }

    override suspend fun signInWithGoogle(
        idToken: String?,
        email: String?,
        displayName: String?,
        photoUrl: String?,
    ) {
        if (SupabaseConfig.isConfigured && !idToken.isNullOrBlank()) {
            runCatching {
                client.auth.signInWith(io.github.jan.tennert.supabase.gotrue.providers.builtin.IDToken) {
                    provider = io.github.jan.tennert.supabase.gotrue.providers.Google
                    this.idToken = idToken
                }
                publishFromSession()
            }
        }
        
        val userId = if (!idToken.isNullOrBlank()) "google_${idToken.hashCode()}" else "google_${email.hashCode()}"
        val auth = AuthState(
            userId = userId,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            isAnonymous = false,
        )
        prefsStore.saveAuthProfile(userId, email, displayName, photoUrl, false)
        state.value = auth
    }

    override suspend fun signOut() {
        if (SupabaseConfig.isConfigured) {
            runCatching { client.auth.signOut() }
        }
        prefsStore.clearAuthProfile()
        val localId = prefsStore.ensureAnonymousId()
        state.value = AuthState(userId = localId, isAnonymous = true)
    }

    /**
     * حذف الحساب. حذف مستخدم GoTrue من جهة العميل يتطلب دالة طرفية
     * بصلاحيات admin (انظر README — قسم Supabase).
     * TODO: عند تجهيز الدالة: استدعاؤها هنا قبل مسح البيانات المحلية.
     */
    override suspend fun deleteAccount() {
        if (SupabaseConfig.isConfigured) {
            runCatching { client.auth.signOut() }
        }
        prefsStore.clearAuthProfile()
        with(database) {
            followsDao().clearFollowedSources()
            followsDao().clearFollowedCategories()
            recentSearchesDao().clearAll()
        }
        runCatching {
            database.clearAllTables()
        }
        val localId = prefsStore.ensureAnonymousId()
        state.value = AuthState(userId = localId, isAnonymous = true)
    }
}
