package chat.sphinx.concept_service_media

import chat.sphinx.wrapper_common.dashboard.ChatId

sealed class MediaPlayerServiceState {

    object ServiceInactive: MediaPlayerServiceState()

    sealed class ServiceActive: MediaPlayerServiceState() {

        object ServiceLoading: ServiceActive()

        object ServiceConnected: ServiceActive()

        sealed class MediaState(
            val chatId: ChatId,
            val episodeId: Long,
            val currentTime: Int,
            val episodeDuration: Long,
        ): ServiceActive() {

            class Playing(
                chatId: ChatId,
                episodeId: Long,
                currentTime: Int,
                episodeDuration: Long
            ): MediaState(
                chatId,
                episodeId,
                currentTime,
                episodeDuration
            )

            class Paused(
                chatId: ChatId,
                episodeId: Long,
                currentTime: Int,
                episodeDuration: Long
            ): MediaState(
                chatId,
                episodeId,
                currentTime,
                episodeDuration
            )

            class Ended(
                chatId: ChatId,
                episodeId: Long,
                currentTime: Int,
                episodeDuration: Long
            ): MediaState(
                chatId,
                episodeId,
                currentTime,
                episodeDuration
            )
        }
    }
}
