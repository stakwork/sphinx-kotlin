package chat.sphinx.chat_tribe.ui

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class PodcastContributionsViewState: ViewState<PodcastContributionsViewState>() {
    object None: PodcastContributionsViewState()
    data class Contributions(val text: String): PodcastContributionsViewState()
}
