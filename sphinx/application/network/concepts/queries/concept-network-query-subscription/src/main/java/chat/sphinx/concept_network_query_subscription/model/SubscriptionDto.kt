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
    val end_number: Int?,
    val end_date: String?,
    val count: Int,
    val ended: Any?,
    val paused: Any?,
    val created_at: String,
    val updated_at: String,
    val interval: String,
    val next: String,
    val chat: ChatDto?,
) {
    @Transient
    val endedActual: Boolean =
        when (ended) {
            is Boolean -> {
                ended
            }
            is Double -> {
                ended.toInt() == 1
            }
            else -> {
                true
            }
        }

    @Transient
    val pausedActual: Boolean =
        when (paused) {
            is Boolean -> {
                paused
            }
            is Double -> {
                paused.toInt() == 1
            }
            else -> {
                true
            }
        }
}
