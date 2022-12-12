package chat.sphinx.common_player.viewstate

import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastEpisode
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class RecommendationsPodcastPlayerViewState: ViewState<RecommendationsPodcastPlayerViewState>() {

    object Idle: RecommendationsPodcastPlayerViewState()
    object ServiceLoading: RecommendationsPodcastPlayerViewState()
    object ServiceInactive: RecommendationsPodcastPlayerViewState()

    class PodcastLoaded(
        val podcast: Podcast,
    ): RecommendationsPodcastPlayerViewState()

    class LoadingEpisode(
        val episode: PodcastEpisode
    ): RecommendationsPodcastPlayerViewState()

    class EpisodePlayed(
        val podcast: Podcast
    ): RecommendationsPodcastPlayerViewState()

    class MediaStateUpdate(
        val podcast: Podcast,
        val state: MediaPlayerServiceState.ServiceActive.MediaState
    ): RecommendationsPodcastPlayerViewState()
}
