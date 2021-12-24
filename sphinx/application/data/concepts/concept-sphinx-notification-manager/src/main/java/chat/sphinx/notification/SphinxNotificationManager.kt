package chat.sphinx.notification

import java.math.BigInteger
import java.security.SecureRandom

interface SphinxNotificationManager {

    companion object {
        const val CHANNEL_ID = "SphinxNotification"
        const val CHANNEL_DESCRIPTION = "Notifications for Sphinx Chat"
        const val MEDIA_NOTIFICATION_ID = 1984
        const val DOWNLOAD_NOTIFICATION_ID = 1985

        val SERVICE_INTENT_FILTER: String by lazy {
            BigInteger(130, SecureRandom()).toString(32)
        }
    }

    fun notify(
        notificationId: Int,
        groupId: String? = null,
        title: String,
        message: String
    )

    fun clearNotification(notificationId: Int)
}
