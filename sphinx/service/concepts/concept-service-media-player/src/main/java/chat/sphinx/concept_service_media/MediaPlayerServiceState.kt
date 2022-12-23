package chat.sphinx.concept_service_media

import chat.sphinx.wrapper_common.dashboard.ChatId

sealed class MediaPlayerServiceState {

    object ServiceInactive: MediaPlayerServiceState()

    sealed class ServiceActive: MediaPlayerServiceState() {

        object ServiceLoading: ServiceActive()

        object ServiceConnected: ServiceActive()

        sealed class MediaState(
            val chatId: ChatId,
            val podcastId: String,
            val episodeId: String,
            val currentTime: Int,
            val episodeDuration: Int,
            val speed: Double,
        ): ServiceActive() {

            class Playing(
                chatId: ChatId,
                podcastId: String,
                episodeId: String,
                currentTime: Int,
                episodeDuration: Int,
                speed: Double
            ): MediaState(
                chatId,
                podcastId,
                episodeId,
                currentTime,
                episodeDuration,
                speed
            )

            class Paused(
                chatId: ChatId,
                podcastId: String,
                episodeId: String,
                currentTime: Int,
                episodeDuration: Int,
                speed: Double
            ): MediaState(
                chatId,
                podcastId,
                episodeId,
                currentTime,
                episodeDuration,
                speed
            )

            class Ended(
                chatId: ChatId,
                podcastId: String,
                episodeId: String,
                currentTime: Int,
                episodeDuration: Int,
                speed: Double
            ): MediaState(
                chatId,
                podcastId,
                episodeId,
                currentTime,
                episodeDuration,
                speed
            )

            class Failed(
                chatId: ChatId,
                podcastId: String,
                episodeId: String,
                currentTime: Int,
                episodeDuration: Int,
                speed: Double
            ): MediaState(
                chatId,
                podcastId,
                episodeId,
                currentTime,
                episodeDuration,
                speed
            )
        }
    }
}
