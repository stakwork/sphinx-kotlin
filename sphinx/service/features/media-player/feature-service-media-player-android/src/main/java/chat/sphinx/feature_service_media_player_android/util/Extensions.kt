package chat.sphinx.feature_service_media_player_android.util

import android.content.Context
import android.content.Intent
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.feature_service_media_player_android.service.SphinxMediaPlayerService
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.lightning.toSat

@Suppress("NOTHING_TO_INLINE")
internal inline fun UserAction.ServiceAction.Play.toIntent(
    context: Context
): Intent {
    val intent = Intent(context.applicationContext, SphinxMediaPlayerService::class.java)
    intent.action = "PLAY"
    intent.putExtra("CHAT_ID", chatId.value)
    intent.putExtra("EPISODE_ID", episodeId)
    intent.putExtra("EPISODE_URL", episodeUrl)
    intent.putExtra("SAT_PER_MINUTE", satPerMinute.value)
    intent.putExtra("SPEED", speed)
    intent.putExtra("START_TIME", startTime)
    return intent
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Intent.toServiceActionPlay(): UserAction.ServiceAction.Play? {
    if (action != "PLAY") {
        return null
    }

    val chatId: ChatId = getLongExtra("CHAT_ID", -1L).let { id ->
        if (id == -1L) {
            return null
        } else {
            try {
                ChatId(id)
            } catch (e: IllegalArgumentException) {
                return null
            }
        }
    }

    val episodeId: Long = getLongExtra("EPISODE_ID", -1L).let {
        if (it == -1L) {
            return null
        } else {
            it
        }
    }

    val startTime: Int = getIntExtra("START_TIME", -1).let {
        if (it == -1) {
            return null
        } else {
            it
        }
    }

    val speed: Double = getDoubleExtra("SPEED", -1.0).let {
        if (it == -1.0) {
            return null
        } else {
            it
        }
    }

    return UserAction.ServiceAction.Play(
        chatId,
        episodeId,
        getStringExtra("EPISODE_URL") ?: return null,
        getLongExtra("SAT_PER_MINUTE", -1L).toSat() ?: return null,
        speed,
        startTime,
    )
}
