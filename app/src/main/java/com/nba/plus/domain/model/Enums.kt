package com.nba.plus.domain.model

import androidx.annotation.StringRes
import com.nba.plus.R

/** تبويبات التغذية العلوية في شاشة «آخر الأخبار». */
enum class FeedTab(@StringRes val labelRes: Int) {
    MOST_READ(R.string.feed_tab_most_read),
    LATEST(R.string.feed_tab_latest),
    BREAKING(R.string.feed_tab_breaking),
    FOR_YOU(R.string.feed_tab_for_you),
}

/** حجم خط العناوين (معامل مضاعف). */
enum class HeadlineFontSize(val scale: Float, @StringRes val labelRes: Int) {
    SMALL(0.85f, R.string.font_size_small),
    MEDIUM(1.0f, R.string.font_size_medium),
    LARGE(1.15f, R.string.font_size_large),
    EXTRA_LARGE(1.3f, R.string.font_size_xlarge),
}

/** وضع العرض. */
enum class ThemeMode(@StringRes val labelRes: Int) {
    DARK(R.string.dark_mode_dark),
    LIGHT(R.string.dark_mode_light),
    SYSTEM(R.string.dark_mode_system),
}

/** حالة المصادقة. */
data class AuthState(
    val userId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isAnonymous: Boolean = true,
) {
    val isSignedIn: Boolean get() = userId != null && !isAnonymous
}

/** تفضيلات المستخدم الكاملة. */
data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val headlineFontSize: HeadlineFontSize = HeadlineFontSize.MEDIUM,
    val autoplayVideos: Boolean = false,
    val breakingAlerts: Boolean = true,
    val topStoriesAlerts: Boolean = true,
    val sportsAlerts: Boolean = false,
    val matchResultsAlerts: Boolean = false,
    val alertSound: Boolean = true,
    val aiPersonalizationEnabled: Boolean = false,
    val anonymousUserId: String = "",
    val alertSourceIds: Set<String> = emptySet(),
)
