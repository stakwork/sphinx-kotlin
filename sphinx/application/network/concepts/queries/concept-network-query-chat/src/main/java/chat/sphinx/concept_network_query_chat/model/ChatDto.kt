package chat.sphinx.concept_network_query_chat.model

import chat.sphinx.wrapper_common.message.MessageId
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatDto(
    val id: Long,
    val uuid: String,
    val name: String?,
    val photo_url: String?,
    val type: Int,
    val status: Int?,
    val contact_ids: List<Long>,
    val is_muted: Any?,
    val created_at: String,
    val updated_at: String,
    val deleted: Any?,
    val group_key: String?,
    val host: String?,
    val price_to_join: Long?,
    val price_per_message: Long?,
    val escrow_amount: Long?,
    val escrow_millis: Long?,
    val unlisted: Any?,
    val private: Any?,
    @Json(name = "owner_pubkey")
    val owner_pub_key: String?,
    val seen: Any?,
    val app_url: String?,
    val feed_url: String?,
    val meta: String?,
    val my_photo_url: String?,
    val my_alias: String?,
    val pending_contact_ids: List<Long>?,
    val pin: String?,
    val notify: Int?,
) {
    @Transient
    val isMutedActual: Boolean =
        when (is_muted) {
            is Boolean -> {
                is_muted
            }
            is Double -> {
                is_muted.toInt() == 1
            }
            else -> {
                false
            }
        }

    @Transient
    val deletedActual: Boolean =
        when (deleted) {
            is Boolean -> {
                deleted
            }
            is Double -> {
                deleted.toInt() == 1
            }
            else -> {
                false
            }
        }

    @Transient
    val unlistedActual: Boolean =
        when (unlisted) {
            is Boolean -> {
                unlisted
            }
            is Double -> {
                unlisted.toInt() == 1
            }
            else -> {
                false
            }
        }

    @Transient
    val privateActual: Boolean =
        when (private) {
            is Boolean -> {
                private
            }
            is Double -> {
                private.toInt() == 1
            }
            else -> {
                true
            }
        }

    @Transient
    val seenActual: Boolean =
        when (seen) {
            is Boolean -> {
                seen
            }
            is Double -> {
                seen.toInt() == 1
            }
            else -> {
                false
            }
        }
}
