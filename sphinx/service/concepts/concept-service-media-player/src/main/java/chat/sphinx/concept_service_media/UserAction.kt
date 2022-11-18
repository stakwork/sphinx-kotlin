package chat.sphinx.concept_service_media

import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_feed.FeedDestination
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastDestination

// TODO: info - episode id, episode url, start time, speed, general info about episode, chatId
sealed class UserAction(val chatId: ChatId) {

    sealed class ServiceAction(chatId: ChatId): UserAction(chatId) {

        class Play(
            chatId: ChatId,
            val podcastId: String,
            val episodeId: String,
            val episodeUrl: String,
            val satPerMinute: Sat,
            val speed: Double,
            val startTime: Int,
        ): ServiceAction(chatId)

        class Pause(
            chatId: ChatId,
            val episodeId: String,
        ): ServiceAction(chatId)

        class Seek(
            chatId: ChatId,
            val chatMetaData: ChatMetaData,
        ): ServiceAction(chatId)

    }

    class AdjustSatsPerMinute(
        chatId: ChatId,
        val chatMetaData: ChatMetaData,
    ): UserAction(chatId)

    class AdjustSpeed(
        chatId: ChatId,
        val chatMetaData: ChatMetaData,
    ): UserAction(chatId)

    class SendBoost(
        chatId: ChatId,
        val podcastId: String,
        val metaData: ChatMetaData,
        val destinations: List<FeedDestination>
    ): UserAction(chatId)

    class SetPaymentsDestinations(
        chatId: ChatId,
        val destinations: List<FeedDestination>
    ): UserAction(chatId)

    class TrackPodcastConsumed(
        chatId: ChatId
    ): UserAction(chatId)
}
