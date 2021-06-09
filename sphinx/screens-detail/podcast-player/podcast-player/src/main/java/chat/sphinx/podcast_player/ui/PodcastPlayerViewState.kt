package chat.sphinx.podcast_player.ui

import chat.sphinx.podcast_player.objects.Podcast
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class PodcastPlayerViewState: ViewState<PodcastPlayerViewState>() {
    object Idle: PodcastPlayerViewState()

    class PodcastObject(
        val podcast: Podcast
    ): PodcastPlayerViewState()
}
