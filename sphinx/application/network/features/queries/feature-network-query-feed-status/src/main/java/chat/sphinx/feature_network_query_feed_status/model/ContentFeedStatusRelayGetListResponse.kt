package chat.sphinx.feature_network_query_feed_status.model

import chat.sphinx.concept_network_query_feed_status.model.ContentFeedStatusDto
import chat.sphinx.concept_network_relay_call.RelayListResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ContentFeedStatusRelayGetListResponse(
    override val success: Boolean,
    override val response: List<ContentFeedStatusDto>,
    override val error: String?
): RelayListResponse<ContentFeedStatusDto>()