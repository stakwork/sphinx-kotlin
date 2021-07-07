package chat.sphinx.feature_network_query_invite.model

import chat.sphinx.concept_network_query_invite.model.PayInviteDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PayInviteResponse(
    override val success: Boolean,
    override val response: PayInviteDto,
    override val error: String? = null
): RelayResponse<PayInviteDto>()
