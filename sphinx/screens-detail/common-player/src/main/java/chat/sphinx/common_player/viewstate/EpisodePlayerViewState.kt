package chat.sphinx.common_player.viewstate

import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastEpisode
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class RecommendationsPodcastPlayerViewState: ViewState<RecommendationsPodcastPlayerViewState>() {

    object Idle: RecommendationsPodcastPlayerViewState()
    object ServiceLoading: RecommendationsPodcastPlayerViewState()
    object ServiceInactive: RecommendationsPodcastPlayerViewState()

    sealed class PodcastViewState(
        val podcast: Podcast,
    ) : RecommendationsPodcastPlayerViewState() {

        class PodcastLoaded(
            podcast: Podcast
        ): PodcastViewState(podcast)

        class LoadingEpisode(
            podcast: Podcast,
            val episode: PodcastEpisode
        ): PodcastViewState(podcast)

        class EpisodePlayed(
            podcast: Podcast
        ): PodcastViewState(podcast)

        class MediaStateUpdate(
            podcast: Podcast,
            val state: MediaPlayerServiceState.ServiceActive.MediaState
        ): PodcastViewState(podcast)
    }
}
