package chat.sphinx.chat_tribe.ui

import chat.sphinx.wrapper_podcast.Podcast
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class PodcastViewState: ViewState<PodcastViewState>() {
    object Idle: PodcastViewState()

    object ServiceLoading: PodcastViewState()
    
    object ServiceInactive: PodcastViewState()

    class PodcastLoaded(
        val podcast: Podcast,
    ): PodcastViewState()

    class PodcastContributionsLoaded(
        val contributions: String,
    ): PodcastViewState()

    class MediaStateUpdate(
        val podcast: Podcast,
    ): PodcastViewState()

}
