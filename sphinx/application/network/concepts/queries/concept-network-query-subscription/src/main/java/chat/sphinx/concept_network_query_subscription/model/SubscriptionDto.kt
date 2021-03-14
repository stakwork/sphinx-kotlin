package chat.sphinx.concept_network_query_subscription.model

import chat.sphinx.concept_network_query_chat.model.ChatDto
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SubscriptionDto(
    val id: Long,
    val chat_id: Long,
    val contact_id: Long,
    val cron: String,
    val amount: Long,
    val total_paid: Long,
    val end_number: Long?,
    val end_date: String?,
    val count: Int?,
    val ended: Int,
    val paused: Int,
    val created_at: String,
    val updated_at: String,
    val tenant: Int,
    val interval: String,
    val next: String,
    val chat: ChatDto?,
)
