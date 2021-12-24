package chat.sphinx.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.notification.SphinxNotificationManager.Companion.CHANNEL_DESCRIPTION
import chat.sphinx.notification.SphinxNotificationManager.Companion.CHANNEL_ID


class SphinxNotificationManagerImpl(
    private val context: Context,
    private val LOG: SphinxLogger,
) : SphinxNotificationManager {
    private inline val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            channel.description = CHANNEL_DESCRIPTION
            channel.setSound(null, null)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun notify(
        notificationId: Int,
        groupId: String?,
        title: String,
        message: String
    ) {
        val builder = NotificationCompat.Builder(
            context,
            CHANNEL_ID,
        )
            .setSmallIcon(R.drawable.sphinx_white_notification)
            .setContentText(message)
            .setContentTitle(title)

        groupId?.let {
            builder.setGroup(it)
        }
        val notification = builder.build()

        notificationManager.notify(notificationId, notification)
    }

    override fun clearNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

}
