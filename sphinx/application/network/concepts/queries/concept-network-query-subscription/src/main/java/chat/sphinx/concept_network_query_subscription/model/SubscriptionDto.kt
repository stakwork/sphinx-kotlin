package chat.sphinx.concept_network_query_subscription.model

import chat.sphinx.concept_network_query_chat.model.ChatDto
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SubscriptionDto(
    override val id: Long,
    override val chat_id: Long,
    override val contact_id: Long,
    override val cron: String,
    override val amount: Long,
    override val total_paid: Long,
    override val end_number: Int?,
    override val end_date: String?,
    override val count: Int,
    override val ended: Boolean,
    override val paused: Boolean,
    override val created_at: String,
    override val updated_at: String,
    override val interval: String,
    override val next: String,
    override val chat: ChatDto?,
): BaseSubscriptionDto<Boolean, Boolean>()
