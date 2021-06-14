package chat.sphinx.feature_service_media_player_android.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState

internal class MediaPlayerNotification(
    private val mediaPlayerService: MediaPlayerService
): MediaPlayerServiceController.MediaServiceListener {

    companion object {
        private const val CHANNEL_ID = "SphinxMediaPlayerService"
        private const val CHANNEL_DESCRIPTION = "Plays Media for Sphinx Chat"
        private const val NOTIFICATION_ID = 1984
    }

    private inline val notificationManager: NotificationManager?
        get() = mediaPlayerService
            .serviceContext
            .applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    @JvmSynthetic
    fun setupNotificationChannel(): MediaPlayerNotification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            channel.description = CHANNEL_DESCRIPTION
            channel.setSound(null, null)
            notificationManager?.createNotificationChannel(channel)
        }

        return this
    }

    private val startTime: Long = System.currentTimeMillis()
    private var builder: NotificationCompat.Builder = buildNotification()

    @JvmSynthetic
    fun buildNotification(): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(
            mediaPlayerService.serviceContext.applicationContext,
            CHANNEL_ID,
        )
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

            // TODO: retrieve from mediaPlayer holder with specific
            //  meta data
            .setContentText("Loading Media")
            .setContentTitle("Sphinx Media Player")

            .setGroup(CHANNEL_ID)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setGroupSummary(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSound(null)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(startTime)

        mediaPlayerService.serviceContext.packageManager
            ?.getLaunchIntentForPackage(mediaPlayerService.serviceContext.packageName)
            ?.let { intent ->
                builder.setContentIntent(
                    PendingIntent.getActivity(
                        mediaPlayerService.serviceContext,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT,
                        null
                    )
                )
            }

        this.builder = builder
        return builder
    }

    init {
        setupNotificationChannel()

        notificationManager?.notify(NOTIFICATION_ID, buildNotification().build())
        mediaPlayerService.mediaServiceController.addListener(this)
    }

    private fun notify(builder: NotificationCompat.Builder) {
        this.builder = builder
        notificationManager?.notify(NOTIFICATION_ID, builder.build())
    }

    override fun mediaServiceState(serviceState: MediaPlayerServiceState) {
        when (serviceState) {
            is MediaPlayerServiceState.ServiceActive.MediaState.Ended -> {
                notify(builder.setContentText("Episode Ended"))
            }
            is MediaPlayerServiceState.ServiceActive.MediaState.Paused -> {
                notify(builder.setContentText("Episode Paused"))
            }
            is MediaPlayerServiceState.ServiceActive.MediaState.Playing -> {
                notify(builder.setContentText("Episode Playing"))
            }
            is MediaPlayerServiceState.ServiceActive.ServiceLoading -> {
                notify(builder.setContentText("Loading Media"))
            }
            is MediaPlayerServiceState.ServiceInactive -> {
                notificationManager?.cancel(NOTIFICATION_ID)
                mediaPlayerService.mediaServiceController.removeListener(this)
            }
        }
    }
}
