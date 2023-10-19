package chat.sphinx.concept_network_query_message.model

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.wrapper_message_media.FileName
import com.squareup.moshi.JsonClass
import java.io.File

@JsonClass(generateAdapter = true)
data class MessageDto(
    val id: Long,
    val uuid: String?,
    val chat_id: Long?,
    val type: Int,
    val sender: Long,
    val receiver: Long?,
    val amount: Long,
    val amount_msat: Long,
    val payment_hash: String?,
    val payment_request: String?,
    val date: String,
    val expiration_date: String?,
    val message_content: String?,
    val remote_message_content: String?,
    val status: Int?,
    val parent_id: Long?,
    val subscription_id: Long?,
    val media_key: String?,
    val media_type: String?,
    val media_token: String?,
    val seen: Any,
    val created_at: String,
    val updated_at: String,
    val sender_alias: String?,
    val sender_pic: String?,
    val original_muid: String?,
    val reply_uuid: String?,
    val network_type: Int?,
    val chat: ChatDto?,
    val contact: ContactDto?,
    val recipient_alias: String?,
    val recipient_pic: String?,
    val push: Any?,
    val person: String?,
    val thread_uuid: String?,
    val error_message: String?
) {
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

    @Transient
    val pushActual: Boolean =
        when (push) {
            is Boolean -> {
                push
            }
            is Double -> {
                push.toInt() == 1
            }
            else -> {
                false
            }
        }

    @Transient
    @Volatile
    var messageContentDecrypted: String? = null
        private set

    fun setMessageContentDecrypted(value: String) {
        if (value.isEmpty()) return
        messageContentDecrypted = value
    }

    @Transient
    @Volatile
    var mediaKeyDecrypted: String? = null
        private set

    fun setMediaKeyDecrypted(value: String) {
        if (value.isEmpty()) return
        mediaKeyDecrypted = value
    }

    @Transient
    @Volatile
    var mediaLocalFile: File? = null
        private set

    fun setMediaLocalFile(file: File) {
        mediaLocalFile = try {
            if (file.exists() && file.isFile) {
                file
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
