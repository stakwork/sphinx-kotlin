package chat.sphinx.concept_service_media

import chat.sphinx.wrapper_common.dashboard.ChatId

sealed class MediaPlayerServiceState {

    object ServiceInactive: MediaPlayerServiceState()

    sealed class ServiceActive(
        val chatId: ChatId,
        val episodeId: Long,
        val currentTime: Long,

//        val duration: Long,
    ): MediaPlayerServiceState() {
        class Playing(
            chatId: ChatId,
            episodeId: Long,
            currentTime: Long,
//            duration: Long,
        ): ServiceActive(
            chatId,
            episodeId,
            currentTime,
//            duration,
        )

        class Paused(
            chatId: ChatId,
            episodeId: Long,
            currentTime: Long,
//            duration: Long,
        ): ServiceActive(
            chatId,
            episodeId,
            currentTime,
//            duration,
        )

        class Ended(
            chatId: ChatId,
            episodeId: Long,
            currentTime: Long,
//            duration: Long,
        ): ServiceActive(
            chatId,
            episodeId,
            currentTime,
//            duration,
        )
    }
}
