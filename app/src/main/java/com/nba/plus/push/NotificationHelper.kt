package com.nba.plus.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nba.plus.MainActivity
import com.nba.plus.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * إنشاء قنوات الإشعارات وبناء التنبيهات المتوافقة مع مفاتيح
 * الإعدادات في شاشة «حسابي».
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun createChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            NotificationChannel(
                CHANNEL_BREAKING,
                context.getString(R.string.channel_breaking),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_breaking_desc)
            },
            NotificationChannel(
                CHANNEL_TOP,
                context.getString(R.string.channel_top),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.channel_top_desc)
            },
            NotificationChannel(
                CHANNEL_SPORTS,
                context.getString(R.string.channel_sports),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.channel_sports_desc)
            },
        )
        manager.createNotificationChannels(channels)
    }

    fun show(
        notificationId: Int,
        channelId: String,
        title: String,
        body: String,
    ) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33

        if (!granted) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    companion object {
        const val CHANNEL_BREAKING = "breaking"
        const val CHANNEL_TOP = "top_stories"
        const val CHANNEL_SPORTS = "sports"
    }
}
