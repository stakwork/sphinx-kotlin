package chat.sphinx.dashboard.ui.viewstates

import chat.sphinx.wrapper_podcast.FeedSearchResultRow
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class FeedViewState: ViewState<FeedViewState>() {

    object Idle: FeedViewState()

    object SearchPlaceHolder: FeedViewState()
    object SearchPodcastPlaceHolder: FeedViewState()
    object SearchVideoPlaceHolder: FeedViewState()

    object LoadingSearchResults: FeedViewState()

    data class SearchResults(
        val searchResults: List<FeedSearchResultRow>
    ): FeedViewState()
}