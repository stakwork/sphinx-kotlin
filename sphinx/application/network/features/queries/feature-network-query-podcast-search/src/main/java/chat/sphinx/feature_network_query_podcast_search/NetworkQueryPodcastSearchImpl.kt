package chat.sphinx.feature_network_query_podcast_search

import chat.sphinx.concept_network_query_podcast_search.NetworkQueryPodcastSearch
import chat.sphinx.concept_network_query_podcast_search.model.PodcastSearchResultDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

class NetworkQueryPodcastSearchImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryPodcastSearch() {

    companion object {
        private const val TRIBES_DEFAULT_SERVER_URL = "https://tribes.sphinx.chat"

        private const val ENDPOINT_PODCAST_SEARCH = "$TRIBES_DEFAULT_SERVER_URL/search_podcasts?q=%s"
    }

    override fun searchPodcasts(
        searchTerm: String,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<List<PodcastSearchResultDto>, ResponseError>> =
        networkRelayCall.getList(
            url = String.format(ENDPOINT_PODCAST_SEARCH, searchTerm),
            responseJsonClass = PodcastSearchResultDto::class.java,
        )

}