package chat.sphinx.feature_network_query_discover_tribes

import chat.sphinx.concept_network_query_chat.model.NewTribeDto
import chat.sphinx.concept_network_query_discover_tribes.NetworkQueryDiscoverTribes
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import kotlinx.coroutines.flow.Flow

class NetworkQueryDiscoverTribesImpl(
    private val networkRelayCall: NetworkRelayCall
    ): NetworkQueryDiscoverTribes() {

    companion object {
        private const val TRIBES_DEFAULT_SERVER_URL = "http://34.229.52.200:8801"

        private const val ENDPOINT_OFFSET_TRIBES = "/tribes?limit=%s&page=%s&sortBy=member_count"
        private const val ENDPOINT_SEARCH_TRIBES = "&search=%s"
        private const val ENDPOINT_TAGS_TRIBES = "&tags=%s"
    }

    override fun getAllDiscoverTribes(
        page: Int,
        itemsPerPage: Int,
        searchTerm: String?,
        tags: String?
    ): Flow<LoadResponse<List<NewTribeDto>, ResponseError>> {
        var url = TRIBES_DEFAULT_SERVER_URL + String.format(ENDPOINT_OFFSET_TRIBES, itemsPerPage.toString(), page.toString())

        searchTerm?.let {
            if (it.isNotEmpty()){
                url += String.format(ENDPOINT_SEARCH_TRIBES, it)
            }
        }

        tags?.let {
            if (it.isNotEmpty()) {
                url += String.format(ENDPOINT_TAGS_TRIBES, it)
            }
        }

        return networkRelayCall.getList(
            url = url,
            responseJsonClass = NewTribeDto::class.java
        )
    }
}