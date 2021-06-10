package chat.sphinx.concept_service_media

import chat.sphinx.wrapper_common.dashboard.ChatId

// TODO: info - episode id, episode url, start time, speed, general info about episode, chatId
sealed class UserAction(val chatId: ChatId) {

    sealed class ServiceAction(chatId: ChatId): UserAction(chatId) {

        class Play(
            chatId: ChatId,
            val episodeId: Long,
            val episodeUrl: String,
            // TODO: General episode info?
            val startTime: Long
        ): ServiceAction(chatId)

        class Pause(
            chatId: ChatId,
            val episodeId: Long,
        ): ServiceAction(chatId)

        class Seek(
            chatId: ChatId,
            val episodeId: Long,
            val seekTime: Long,
        ): ServiceAction(chatId)

    }

    class AdjustSpeed(
        chatId: ChatId,
        val speed: Double
    ): UserAction(chatId)
}
