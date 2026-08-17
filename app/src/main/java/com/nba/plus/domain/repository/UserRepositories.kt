package com.nba.plus.domain.repository

import com.nba.plus.domain.model.Article
import com.nba.plus.domain.model.AuthState
import com.nba.plus.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface SavedArticlesRepository {

    fun observeSaved(): Flow<List<Article>>

    fun observeSavedIds(): Flow<Set<String>>

    /** يبدّل حالة الحفظ ويعيد الحالة الجديدة. */
    suspend fun toggleSaved(article: Article): Boolean

    suspend fun isSaved(articleId: String): Boolean
}

interface UserPreferencesRepository {

    val preferences: Flow<UserPreferences>

    suspend fun update(transform: (UserPreferences) -> UserPreferences)
}

interface AuthRepository {

    val authState: Flow<AuthState>

    /** يضمن وجود جلسة (مجهولة الهوية إن لزم) عند الإقلاع. */
    suspend fun ensureSignedIn()

    suspend fun signInWithEmail(email: String, password: String)

    suspend fun signUpWithEmail(email: String, password: String)

    suspend fun signInWithGoogle(
        idToken: String?,
        email: String?,
        displayName: String?,
        photoUrl: String?,
    )

    suspend fun signOut()

    suspend fun deleteAccount()
}

/**
 * واجهة التخصيص بالذكاء الاصطناعي — scaffold فقط.
 * التنفيذ الحالي Mock يخزّن تفضيل Boolean؛ يُستبدل لاحقًا
 * بتنفيذ حقيقي يتصل بخدمة التخصيص.
 */
interface PersonalizationRepository {

    val isEnabled: Flow<Boolean>

    suspend fun setEnabled(enabled: Boolean)
}

/** سجل التفاعلات المحلي (إعجابات). */
interface InteractionsRepository {

    fun observeLikedArticleIds(): Flow<Set<String>>

    fun observeLiked(): Flow<List<Article>>

    suspend fun toggleLike(article: Article): Boolean
}
