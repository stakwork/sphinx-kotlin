package chat.sphinx.podcast_player.ui

import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.podcast_player.objects.Podcast
import chat.sphinx.podcast_player.objects.PodcastEpisode
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class PodcastPlayerViewState: ViewState<PodcastPlayerViewState>() {

    object Idle: PodcastPlayerViewState()
    object ServiceLoading: PodcastPlayerViewState()
    object ServiceInactive: PodcastPlayerViewState()

    class PodcastLoaded(
        val podcast: Podcast
    ): PodcastPlayerViewState()

    class LoadingEpisode(
        val episode: PodcastEpisode
    ): PodcastPlayerViewState()

    class EpisodePlayed(
        val podcast: Podcast
    ): PodcastPlayerViewState()

    class MediaStateUpdate(
        val podcast: Podcast,
        val state: MediaPlayerServiceState.ServiceActive.MediaState
    ): PodcastPlayerViewState()
}
