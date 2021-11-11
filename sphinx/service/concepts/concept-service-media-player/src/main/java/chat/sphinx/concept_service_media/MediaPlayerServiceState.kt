package chat.sphinx.concept_service_media

import chat.sphinx.wrapper_common.dashboard.ChatId

sealed class MediaPlayerServiceState {

    object ServiceInactive: MediaPlayerServiceState()

    sealed class ServiceActive: MediaPlayerServiceState() {

        object ServiceLoading: ServiceActive()

        object ServiceConnected: ServiceActive()

        sealed class MediaState(
            val chatId: ChatId,
            val episodeId: String,
            val currentTime: Int,
            val episodeDuration: Int,
        ): ServiceActive() {

            class Playing(
                chatId: ChatId,
                episodeId: String,
                currentTime: Int,
                episodeDuration: Int
            ): MediaState(
                chatId,
                episodeId,
                currentTime,
                episodeDuration
            )

            class Paused(
                chatId: ChatId,
                episodeId: String,
                currentTime: Int,
                episodeDuration: Int
            ): MediaState(
                chatId,
                episodeId,
                currentTime,
                episodeDuration
            )

            class Ended(
                chatId: ChatId,
                episodeId: String,
                currentTime: Int,
                episodeDuration: Int
            ): MediaState(
                chatId,
                episodeId,
                currentTime,
                episodeDuration
            )
        }
    }
}
