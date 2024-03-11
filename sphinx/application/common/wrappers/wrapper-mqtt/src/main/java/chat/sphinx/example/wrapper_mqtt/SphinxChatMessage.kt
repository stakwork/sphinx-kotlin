package chat.sphinx.example.wrapper_mqtt

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class Message(
    val content: String?,
    val amount: Int?,
    val mediaToken: String?,
    val mediaKey: String?,
    val mediaType: String?,
    val replyUuid: String?,
    val threadUuid: String?,
    val member: String?,
    val invoice: String?
) {
    @Throws(AssertionError::class)
    fun toJson(moshi: Moshi): String {
        val adapter = moshi.adapter(Message::class.java)
        return adapter.toJson(this)
    }

    companion object {
        fun String.toMessageNull(moshi: Moshi): Message? {
            return try {
                this.toMessage(moshi)
            } catch (e: Exception) {
                null
            }
        }

        @Throws(JsonDataException::class)
        fun String.toMessage(moshi: Moshi): Message {
            val adapter = moshi.adapter(Message::class.java)
            return adapter.fromJson(this) ?: throw IllegalArgumentException("Invalid JSON for Message")
        }
    }
}