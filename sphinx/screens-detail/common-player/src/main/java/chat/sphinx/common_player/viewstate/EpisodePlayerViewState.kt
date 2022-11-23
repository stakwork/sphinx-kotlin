package chat.sphinx.common_player.viewstate

import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.wrapper_feed.FeedRecommendation
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastEpisode
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class EpisodePlayerViewState: ViewState<EpisodePlayerViewState>() {

    object Idle: EpisodePlayerViewState()
    object ServiceLoading: EpisodePlayerViewState()
    object ServiceInactive: EpisodePlayerViewState()

    class EpisodeLoaded(
        val feedRecommendation: FeedRecommendation,
    ): EpisodePlayerViewState()

    class LoadingEpisode(
        val feedRecommendation: FeedRecommendation,
    ): EpisodePlayerViewState()

    class EpisodePlayed(
        val feedRecommendation: FeedRecommendation,
    ): EpisodePlayerViewState()

    class MediaStateUpdate(
        val feedRecommendation: FeedRecommendation,
        val state: MediaPlayerServiceState.ServiceActive.MediaState
    ): EpisodePlayerViewState()
}
