package chat.sphinx.feature_service_media_player_android.service.components

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.feature_service_media_player_android.R
import chat.sphinx.feature_service_media_player_android.service.MediaPlayerService
import java.math.BigInteger
import java.security.SecureRandom

internal class MediaPlayerNotification(
    private val mediaPlayerService: MediaPlayerService
) : BroadcastReceiver(),
    MediaPlayerServiceController.MediaServiceListener
{

    companion object {
        private const val CHANNEL_ID = "SphinxMediaPlayerService"
        private const val CHANNEL_DESCRIPTION = "Plays Media for Sphinx Chat"
        private const val NOTIFICATION_ID = 1984

        private val SERVICE_INTENT_FILTER: String by lazy {
            BigInteger(130, SecureRandom()).toString(32)
        }

        private const val ACTION_DELETE = "ACTION_DELETE"
        private const val ACTION_DELETE_CODE = 1
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null && intent.action == SERVICE_INTENT_FILTER) {
            when (intent.getStringExtra(SERVICE_INTENT_FILTER)) {
                ACTION_DELETE -> {
                    mediaPlayerService.shutDownService()
                }
                null -> {}
            }
        }
    }

    init {
        mediaPlayerService
            .serviceContext
            .registerReceiver(this, IntentFilter(SERVICE_INTENT_FILTER))
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
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.sphinx_white_notification)
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
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        null
                    )
                )
            }


        builder.addAction(
            0,
            "Stop",
            getActionPendingIntent(ACTION_DELETE, ACTION_DELETE_CODE)
        )

        this.builder = builder
        return builder
    }

    @Throws(IllegalArgumentException::class)
    private fun getActionPendingIntent(
        action: String,
        requestCode: Int,
    ): PendingIntent {
        if (action.isEmpty()) {
            throw IllegalArgumentException("Intent Action cannot be empty")
        }

        val intent = Intent(SERVICE_INTENT_FILTER)
        intent.putExtra(SERVICE_INTENT_FILTER, action)
        intent.setPackage(mediaPlayerService.serviceContext.packageName)

        return PendingIntent.getBroadcast(
            mediaPlayerService.serviceContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notify(builder: NotificationCompat.Builder) {
        this.builder = builder
        notificationManager?.notify(NOTIFICATION_ID, builder.build())
    }

    init {
        setupNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaPlayerService.startForeground(NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            mediaPlayerService.startForeground(NOTIFICATION_ID, builder.build())
        }

        mediaPlayerService.mediaServiceController.addListener(this)
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
            is MediaPlayerServiceState.ServiceActive.ServiceConnected -> {
                notify(builder.setContentText("Media Service Connected"))
            }
            is MediaPlayerServiceState.ServiceInactive -> {}
            else -> {}
        }
    }

    @JvmSynthetic
    fun clear() {
        mediaPlayerService.mediaServiceController.removeListener(this)
        try {
            mediaPlayerService.serviceContext.unregisterReceiver(this)
        } catch (e: RuntimeException) {}
        notificationManager?.cancel(NOTIFICATION_ID)
    }
}
