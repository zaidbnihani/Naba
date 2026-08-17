package com.nba.plus.push

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.nba.plus.data.preferences.UserPreferencesDataStore
import com.nba.plus.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * يزامن مفاتيح التنبيهات في الإعدادات مع اشتراكات مواضيع FCM.
 * يعمل فقط عند تهيئة Firebase (وجود google-services.json) —
 * وإلا يتوقف بصمت دون أي أثر.
 */
@Singleton
class TopicSubscriptionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: UserPreferencesDataStore,
    @ApplicationScope private val appScope: CoroutineScope,
) {

    private var started = false

    fun start() {
        if (started) return
        started = true

        if (FirebaseApp.getApps(context).isEmpty()) return

        appScope.launch {
            preferences.preferences.collect { prefs ->
                syncTopic(TOPIC_BREAKING, prefs.breakingAlerts)
                syncTopic(TOPIC_TOP, prefs.topStoriesAlerts)
                syncTopic(TOPIC_SPORTS, prefs.sportsAlerts)
                syncTopic(TOPIC_MATCHES, prefs.matchResultsAlerts)
            }
        }
    }

    private fun syncTopic(topic: String, enabled: Boolean) {
        runCatching {
            val messaging = FirebaseMessaging.getInstance()
            if (enabled) messaging.subscribeToTopic(topic)
            else messaging.unsubscribeFromTopic(topic)
        }
    }

    companion object {
        const val TOPIC_BREAKING = "breaking"
        const val TOPIC_TOP = "top_stories"
        const val TOPIC_SPORTS = "sports"
        const val TOPIC_MATCHES = "match_results"
    }
}
