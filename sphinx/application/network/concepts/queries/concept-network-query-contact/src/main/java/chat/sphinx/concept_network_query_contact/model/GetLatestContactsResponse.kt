package chat.sphinx.concept_network_query_contact.model

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_query_invite.model.InviteDto
import chat.sphinx.concept_network_query_subscription.model.SubscriptionDto
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetLatestContactsResponse(
    val contacts: List<ContactDto>,
    val chats: List<ChatDto>,
    val subscriptions: List<SubscriptionDto>,
    val invites: List<InviteDto>,
)
