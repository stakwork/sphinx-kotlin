package chat.sphinx.concept_service_media

import chat.sphinx.wrapper_common.dashboard.ChatId

sealed class MediaPlayerServiceState {

    object ServiceInactive: MediaPlayerServiceState()

    sealed class ServiceActive: MediaPlayerServiceState() {

        object ServiceLoading: ServiceActive()

        object ServiceLoaded: ServiceActive()

        sealed class MediaState(
            val chatId: ChatId,
            val episodeId: Long,
            val currentTime: Int,
        ): ServiceActive() {

            class Playing(
                chatId: ChatId,
                episodeId: Long,
                currentTime: Int,
            ): MediaState(
                chatId,
                episodeId,
                currentTime,
            )

            class Paused(
                chatId: ChatId,
                episodeId: Long,
                currentTime: Int,
            ): MediaState(
                chatId,
                episodeId,
                currentTime,
            )

            class Ended(
                chatId: ChatId,
                episodeId: Long,
                currentTime: Int,
            ): MediaState(
                chatId,
                episodeId,
                currentTime,
            )
        }
    }
}
