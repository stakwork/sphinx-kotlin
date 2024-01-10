package chat.sphinx.wrapper_message

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonDataException
import java.lang.IllegalArgumentException

@JsonClass(generateAdapter = true)
data class Msg(
    val content: String,
    val amount: Int,
    val mediaToken: String?,
    val mediaKey: String?,
    val mediaType: String?
) {
    companion object {
        fun String.toMsgNull(moshi: Moshi): Msg? {
            return try {
                this.toMsg(moshi)
            } catch (e: Exception) {
                null
            }
        }

        @Throws(JsonDataException::class, IllegalArgumentException::class)
        fun String.toMsg(moshi: Moshi): Msg {
            val adapter = moshi.adapter(Msg::class.java)
            return adapter.fromJson(this) ?: throw IllegalArgumentException("Invalid JSON for Msg")
        }
    }
}
