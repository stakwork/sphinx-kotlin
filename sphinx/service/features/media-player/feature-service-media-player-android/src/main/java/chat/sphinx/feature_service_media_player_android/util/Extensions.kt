package chat.sphinx.feature_service_media_player_android.util

import android.content.Context
import android.content.Intent
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.feature_service_media_player_android.service.SphinxMediaPlayerService
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.feed.Subscribed
import chat.sphinx.wrapper_common.feed.toSubscribed
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_feed.ContentEpisodeStatus
import chat.sphinx.wrapper_feed.ContentFeedStatus
import chat.sphinx.wrapper_feed.FeedItemDuration
import chat.sphinx.wrapper_feed.FeedPlayerSpeed

@Suppress("NOTHING_TO_INLINE")
internal inline fun UserAction.ServiceAction.Play.toIntent(
    context: Context
): Intent {
    val intent = Intent(context.applicationContext, SphinxMediaPlayerService::class.java)
    intent.action = "PLAY"
    intent.putExtra("CHAT_ID", chatId.value)
    intent.putExtra("PODCAST_ID", contentFeedStatus.feedId.value)
    intent.putExtra("PODCAST_URL", contentFeedStatus.feedUrl.value)
    intent.putExtra("SUBSCRIBED", contentFeedStatus.subscriptionStatus.value)
    intent.putExtra("SAT_PER_MINUTE", contentFeedStatus.satsPerMinute?.value ?: 0)
    intent.putExtra("SPEED", contentFeedStatus.playerSpeed?.value ?: 1.0)
    intent.putExtra("EPISODE_URL", episodeUrl)
    intent.putExtra("EPISODE_ID", contentEpisodeStatus.itemId.value)
    intent.putExtra("START_TIME", contentEpisodeStatus.currentTime.value)
    intent.putExtra("DURATION", contentEpisodeStatus.duration.value)
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
                ChatId(ChatId.NULL_CHAT_ID.toLong())
            }
        }
    }

    val speed: Double = getDoubleExtra("SPEED", -1.0).let {
        if (it == -1.0) {
            1.0
        } else {
            it
        }
    }

    val podcastId: String = getStringExtra("PODCAST_ID")?.let {
        it.ifEmpty {
            return null
        }
        it
    } ?: run {
        return null
    }

    val podcastUrl: String = getStringExtra("PODCAST_URL")?.let {
        it.ifEmpty {
            return null
        }
        it
    } ?: run {
        return null
    }

    val episodeId = getStringExtra("EPISODE_ID")?.let {
        it.ifEmpty {
            return null
        }
        it
    } ?: run {
        return null
    }

    val episodeUrl = getStringExtra("EPISODE_URL")?.let {
        it.ifEmpty {
            return null
        }
        it
    } ?: run {
        return null
    }

    val satsPerMinute = getLongExtra("SAT_PER_MINUTE", 0).toSat()
    val startTime = getLongExtra("START_TIME", 0)
    val duration = getLongExtra("DURATION", 0)

    val subscribed = getIntExtra("SUBSCRIBED", 0)

    return UserAction.ServiceAction.Play(
        chatId,
        episodeUrl,
        ContentFeedStatus(
            FeedId(podcastId),
            FeedUrl(podcastUrl),
            subscribed.toSubscribed(),
            chatId,
            FeedId(episodeId),
            satsPerMinute,
            FeedPlayerSpeed(speed)
        ),
        ContentEpisodeStatus(
            FeedId(podcastId),
            FeedId(episodeId),
            FeedItemDuration(duration),
            FeedItemDuration(startTime)
        )
    )
}
