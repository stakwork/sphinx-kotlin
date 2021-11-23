package chat.sphinx.dashboard.ui.viewstates

import chat.sphinx.concept_network_query_podcast_search.model.PodcastSearchResultDto
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class FeedViewState: ViewState<FeedViewState>() {

    object Idle: FeedViewState()

    object SearchPlaceHolder: FeedViewState()

    object LoadingSearchResults: FeedViewState()

    data class SearchResults(
        val searchResults: List<PodcastSearchResultDto>
    ): FeedViewState()
}