package chat.sphinx.concept_network_query_discover_tribes

import chat.sphinx.concept_network_query_chat.model.NewTribeDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryDiscoverTribes {

    ///////////
    /// GET ///
    ///////////
    abstract fun getAllDiscoverTribes(
        page: Int,
        itemsPerPage: Int,
        searchTerm: String?,
        tags: String?
    ): Flow<LoadResponse<List<NewTribeDto>, ResponseError>>
}