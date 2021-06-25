package chat.sphinx.feature_network_query_attachment.model

import chat.sphinx.concept_network_query_attachment.model.AttachmentChallengeSigDto
import chat.sphinx.concept_network_relay_call.RelayResponse

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SignChallengeRelayResponse(
    override val success: Boolean,
    override val response: AttachmentChallengeSigDto?,
    override val error: String?
) : RelayResponse<AttachmentChallengeSigDto>()
