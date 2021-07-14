package chat.sphinx.feature_network_query_meme_server.model

import chat.sphinx.concept_network_query_meme_server.model.MemeServerChallengeSigDto
import chat.sphinx.concept_network_relay_call.RelayResponse

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MemeServerChallengeSigRelayResponse(
    override val success: Boolean,
    override val response: MemeServerChallengeSigDto?,
    override val error: String?
) : RelayResponse<MemeServerChallengeSigDto>()
