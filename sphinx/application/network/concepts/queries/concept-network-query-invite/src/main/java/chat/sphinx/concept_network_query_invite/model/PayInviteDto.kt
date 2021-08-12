package chat.sphinx.concept_network_query_invite.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PayInviteDto(
    val invite: InviteDto
)