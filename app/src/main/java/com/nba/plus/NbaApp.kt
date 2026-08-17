package com.nba.plus

import android.app.Application
import com.nba.plus.data.remote.MockDataSource
import com.nba.plus.di.ApplicationScope
import com.nba.plus.push.NotificationHelper
import com.nba.plus.push.TopicSubscriptionManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NbaApp : Application() {

    @Inject
    lateinit var mockDataSource: MockDataSource

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var topicSubscriptionManager: TopicSubscriptionManager

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()
        appScope.launch {
            runCatching { mockDataSource.warmUp() }
            topicSubscriptionManager.start()
        }
    }
}
