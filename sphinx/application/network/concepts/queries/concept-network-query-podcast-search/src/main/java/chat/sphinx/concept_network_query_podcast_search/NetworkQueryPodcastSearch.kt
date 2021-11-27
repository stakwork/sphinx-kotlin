package chat.sphinx.concept_network_query_podcast_search

import chat.sphinx.concept_network_query_podcast_search.model.PodcastSearchResultDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryPodcastSearch {

    ///////////
    /// GET ///
    ///////////
    abstract fun searchPodcasts(
        searchTerm: String,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<List<PodcastSearchResultDto>, ResponseError>>
}