package chat.sphinx.concept_service_media

import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.lightning.Sat

// TODO: info - episode id, episode url, start time, speed, general info about episode, chatId
sealed class UserAction(val chatId: ChatId) {

    sealed class ServiceAction(chatId: ChatId): UserAction(chatId) {

        class Play(
            chatId: ChatId,
            val episodeId: Long,
            val episodeUrl: String,
            val satPerMinute: Sat,
            val speed: Double,
//            val podcastInfo: PodcastDto,
            val startTime: Int,
        ): ServiceAction(chatId)

        class Pause(
            chatId: ChatId,
            val episodeId: Long,
        ): ServiceAction(chatId)

        class Seek(
            chatId: ChatId,
            val chatMetaData: ChatMetaData,
        ): ServiceAction(chatId)

    }

//    class AdjustSatsPerMinute(
//        chatId: ChatId,
//        val chatMetaData: ChatMetaData,
//    ): UserAction(chatId)

    class AdjustSpeed(
        chatId: ChatId,
        val chatMetaData: ChatMetaData,
    ): UserAction(chatId)
}
