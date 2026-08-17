package com.nba.plus.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * استقبال رسائل FCM للأخبار العاجلة والتنبيهات.
 * تُرسَل الخوادم رسائل البيانات بالشكل:
 * data: { "channel": "breaking|top_stories|sports", "title": "...", "body": "..." }
 */
@AndroidEntryPoint
class NbaMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: return
        val body = data["body"] ?: message.notification?.body.orEmpty()
        val channel = when (data["channel"]) {
            NotificationHelper.CHANNEL_TOP -> NotificationHelper.CHANNEL_TOP
            NotificationHelper.CHANNEL_SPORTS -> NotificationHelper.CHANNEL_SPORTS
            else -> NotificationHelper.CHANNEL_BREAKING
        }
        notificationHelper.show(
            notificationId = (title + body).hashCode(),
            channelId = channel,
            title = title,
            body = body,
        )
    }

    override fun onNewToken(token: String) {
        // الاشتراكات تُدار عبر المواضيع (topics) حسب مفاتيح الإعدادات،
        // فلا نحتاج حاليًا لإرسال التوكن إلى خادم.
    }
}
