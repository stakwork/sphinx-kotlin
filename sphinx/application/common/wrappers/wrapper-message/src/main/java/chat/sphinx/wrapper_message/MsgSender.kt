package chat.sphinx.wrapper_message

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonDataException
import java.lang.IllegalArgumentException

@JsonClass(generateAdapter = true)
data class MsgSender(
    val pubkey: String,
    val alias: String?,
    val photo_url: String?,
    val person: String?,
    val confirmed: Boolean
) {
    companion object {
        fun String.toMsgSenderNull(moshi: Moshi): MsgSender? {
            return try {
                this.toMsgSender(moshi)
            } catch (e: JsonDataException) {
                null
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        @Throws(JsonDataException::class, IllegalArgumentException::class)
        fun String.toMsgSender(moshi: Moshi): MsgSender {
            val adapter = moshi.adapter(MsgSender::class.java)
            return adapter.fromJson(this) ?: throw IllegalArgumentException("Invalid JSON for MsgSender")
        }
    }
}
