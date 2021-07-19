package chat.sphinx.podcast_player.ui

import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.podcast_player.objects.ParcelablePodcast
import chat.sphinx.podcast_player.objects.ParcelablePodcastEpisode
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class PodcastPlayerViewState: ViewState<PodcastPlayerViewState>() {

    object Idle: PodcastPlayerViewState()
    object ServiceLoading: PodcastPlayerViewState()
    object ServiceInactive: PodcastPlayerViewState()

    class PodcastLoaded(
        val podcast: ParcelablePodcast
    ): PodcastPlayerViewState()

    class LoadingEpisode(
        val episode: ParcelablePodcastEpisode
    ): PodcastPlayerViewState()

    class EpisodePlayed(
        val podcast: ParcelablePodcast
    ): PodcastPlayerViewState()

    class MediaStateUpdate(
        val podcast: ParcelablePodcast,
        val state: MediaPlayerServiceState.ServiceActive.MediaState
    ): PodcastPlayerViewState()
}
