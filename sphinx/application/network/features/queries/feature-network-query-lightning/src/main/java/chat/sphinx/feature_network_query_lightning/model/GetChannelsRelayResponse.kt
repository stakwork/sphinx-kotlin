package chat.sphinx.feature_network_query_lightning.model

import chat.sphinx.concept_network_query_lightning.model.channel.ChannelsDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetChannelsRelayResponse(
    override val success: Boolean,
    override val response: ChannelsDto?,
    override val error: String?
): RelayResponse<ChannelsDto>()
