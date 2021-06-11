package chat.sphinx.podcast_player.ui

import chat.sphinx.podcast_player.objects.Podcast
import chat.sphinx.podcast_player.objects.PodcastEpisode
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class PodcastPlayerViewState: ViewState<PodcastPlayerViewState>() {

    object Idle: PodcastPlayerViewState()

    data class PodcastLoaded(
        val podcast: Podcast
    ): PodcastPlayerViewState()

    data class LoadingEpisode(
        val episode: PodcastEpisode
    ): PodcastPlayerViewState()

    data class EpisodePlayed(
        val podcast: Podcast
    ): PodcastPlayerViewState()
}
