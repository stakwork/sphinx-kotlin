package chat.sphinx.podcast_player.ui

import chat.sphinx.podcast_player.objects.Podcast
import chat.sphinx.podcast_player.objects.PodcastEpisode
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class PodcastPlayerViewState: ViewState<PodcastPlayerViewState>() {
    abstract val episodesList: List<PodcastEpisode>

    class Idle(
        override val episodesList: List<PodcastEpisode>,
    ): PodcastPlayerViewState()

    class PodcastObject(
        val podcast: Podcast,
        override val episodesList: List<PodcastEpisode>,
    ): PodcastPlayerViewState()
}
