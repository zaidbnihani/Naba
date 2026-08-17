package com.nba.plus.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nba.plus.domain.model.HeadlineFontSize
import com.nba.plus.domain.model.ThemeMode
import com.nba.plus.domain.model.UserPreferences
import com.nba.plus.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPrefsDataStore by preferencesDataStore(name = "nba_user_prefs")

private object PrefsKeys {
    val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val HEADLINE_FONT = stringPreferencesKey("headline_font_size")
    val AUTOPLAY = booleanPreferencesKey("autoplay_videos")
    val NOTIF_BREAKING = booleanPreferencesKey("notif_breaking")
    val NOTIF_TOP = booleanPreferencesKey("notif_top")
    val NOTIF_SPORTS = booleanPreferencesKey("notif_sports")
    val NOTIF_MATCHES = booleanPreferencesKey("notif_matches")
    val NOTIF_SOUND = booleanPreferencesKey("notif_sound")
    val AI_PERSONALIZATION = booleanPreferencesKey("ai_personalization")
    val ANONYMOUS_USER_ID = stringPreferencesKey("anonymous_user_id")
    val ALERT_SOURCES = stringSetPreferencesKey("alert_source_ids")
    val AUTH_USER_ID = stringPreferencesKey("auth_user_id")
    val AUTH_EMAIL = stringPreferencesKey("auth_email")
    val AUTH_DISPLAY_NAME = stringPreferencesKey("auth_display_name")
    val AUTH_PHOTO_URL = stringPreferencesKey("auth_photo_url")
    val AUTH_IS_ANONYMOUS = booleanPreferencesKey("auth_is_anonymous")
}

/**
 * تخزين تفضيلات المستخدم في DataStore (الثيم، حجم الخط، التنبيهات…).
 */
@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserPreferencesRepository {

    override val preferences: Flow<UserPreferences> = context.userPrefsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it.toUserPreferences() }

    override suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        context.userPrefsDataStore.edit { prefs ->
            transform(prefs.toUserPreferences()).writeInto(prefs)
        }
    }

    suspend fun saveAuthProfile(
        userId: String,
        email: String?,
        displayName: String?,
        photoUrl: String?,
        isAnonymous: Boolean,
    ) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[PrefsKeys.AUTH_USER_ID] = userId
            if (email != null) prefs[PrefsKeys.AUTH_EMAIL] = email else prefs.remove(PrefsKeys.AUTH_EMAIL)
            if (displayName != null) prefs[PrefsKeys.AUTH_DISPLAY_NAME] = displayName else prefs.remove(PrefsKeys.AUTH_DISPLAY_NAME)
            if (photoUrl != null) prefs[PrefsKeys.AUTH_PHOTO_URL] = photoUrl else prefs.remove(PrefsKeys.AUTH_PHOTO_URL)
            prefs[PrefsKeys.AUTH_IS_ANONYMOUS] = isAnonymous
        }
    }

    suspend fun getSavedAuthProfile(): com.nba.plus.domain.model.AuthState? {
        val prefs = context.userPrefsDataStore.data.first()
        val userId = prefs[PrefsKeys.AUTH_USER_ID] ?: return null
        return com.nba.plus.domain.model.AuthState(
            userId = userId,
            email = prefs[PrefsKeys.AUTH_EMAIL],
            displayName = prefs[PrefsKeys.AUTH_DISPLAY_NAME],
            photoUrl = prefs[PrefsKeys.AUTH_PHOTO_URL],
            isAnonymous = prefs[PrefsKeys.AUTH_IS_ANONYMOUS] ?: false,
        )
    }

    suspend fun clearAuthProfile() {
        context.userPrefsDataStore.edit { prefs ->
            prefs.remove(PrefsKeys.AUTH_USER_ID)
            prefs.remove(PrefsKeys.AUTH_EMAIL)
            prefs.remove(PrefsKeys.AUTH_DISPLAY_NAME)
            prefs.remove(PrefsKeys.AUTH_PHOTO_URL)
            prefs.remove(PrefsKeys.AUTH_IS_ANONYMOUS)
        }
    }

    /** يعيد المعرّف المحلي المجهول، ويُنشئه عند أول استخدام. */
    suspend fun ensureAnonymousId(): String {
        val key = PrefsKeys.ANONYMOUS_USER_ID
        val existing = context.userPrefsDataStore.data.first()[key]
        if (!existing.isNullOrBlank()) return existing
        val newId = UUID.randomUUID().toString()
        context.userPrefsDataStore.edit { it[key] = newId }
        return newId
    }

    private fun androidx.datastore.preferences.core.Preferences.toUserPreferences(): UserPreferences =
        UserPreferences(
            onboardingCompleted = this[PrefsKeys.ONBOARDING_DONE] ?: false,
            themeMode = enumOrDefault(this[PrefsKeys.THEME_MODE], ThemeMode.DARK),
            headlineFontSize = enumOrDefault(this[PrefsKeys.HEADLINE_FONT], HeadlineFontSize.MEDIUM),
            autoplayVideos = this[PrefsKeys.AUTOPLAY] ?: false,
            breakingAlerts = this[PrefsKeys.NOTIF_BREAKING] ?: true,
            topStoriesAlerts = this[PrefsKeys.NOTIF_TOP] ?: true,
            sportsAlerts = this[PrefsKeys.NOTIF_SPORTS] ?: false,
            matchResultsAlerts = this[PrefsKeys.NOTIF_MATCHES] ?: false,
            alertSound = this[PrefsKeys.NOTIF_SOUND] ?: true,
            aiPersonalizationEnabled = this[PrefsKeys.AI_PERSONALIZATION] ?: false,
            anonymousUserId = this[PrefsKeys.ANONYMOUS_USER_ID].orEmpty(),
            alertSourceIds = this[PrefsKeys.ALERT_SOURCES] ?: emptySet(),
        )

    private fun UserPreferences.writeInto(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        prefs[PrefsKeys.ONBOARDING_DONE] = onboardingCompleted
        prefs[PrefsKeys.THEME_MODE] = themeMode.name
        prefs[PrefsKeys.HEADLINE_FONT] = headlineFontSize.name
        prefs[PrefsKeys.AUTOPLAY] = autoplayVideos
        prefs[PrefsKeys.NOTIF_BREAKING] = breakingAlerts
        prefs[PrefsKeys.NOTIF_TOP] = topStoriesAlerts
        prefs[PrefsKeys.NOTIF_SPORTS] = sportsAlerts
        prefs[PrefsKeys.NOTIF_MATCHES] = matchResultsAlerts
        prefs[PrefsKeys.NOTIF_SOUND] = alertSound
        prefs[PrefsKeys.AI_PERSONALIZATION] = aiPersonalizationEnabled
        prefs[PrefsKeys.ANONYMOUS_USER_ID] = anonymousUserId
        prefs[PrefsKeys.ALERT_SOURCES] = alertSourceIds
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}

/**
 * مستودع التخصيص بالذكاء الاصطناعي — يعتمد على Gemini 3.7 Flash وتفضيلات المستخدم.
 */
@Singleton
class RealPersonalizationRepository @Inject constructor(
    private val store: UserPreferencesDataStore,
) : com.nba.plus.domain.repository.PersonalizationRepository {

    override val isEnabled: Flow<Boolean> =
        store.preferences.map { it.aiPersonalizationEnabled }

    override suspend fun setEnabled(enabled: Boolean) {
        store.update { it.copy(aiPersonalizationEnabled = enabled) }
    }
}
