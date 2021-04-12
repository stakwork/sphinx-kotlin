package chat.sphinx.concept_network_query_contact.model

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_query_subscription.model.SubscriptionDto
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetContactsResponse(
    val contacts: List<ContactDto>,
    val chats: List<ChatDto>,
    val subscriptions: List<SubscriptionDto>,
)
