package chat.sphinx.example.wrapper_mqtt

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonDataException

@JsonClass(generateAdapter = true)
data class MessagesFetchRequest(
    val since: Long,
    val limit: Int
)

@Throws(AssertionError::class)
fun MessagesFetchRequest.toJson(moshi: Moshi): String {
    val adapter = moshi.adapter(MessagesFetchRequest::class.java)
    return adapter.toJson(this)
}

fun String.toMessagesFetchRequestOrNull(moshi: Moshi): MessagesFetchRequest? {
    return try {
        this.toMessagesFetchRequest(moshi)
    } catch (e: Exception) {
        null
    }
}

@Throws(JsonDataException::class)
fun String.toMessagesFetchRequest(moshi: Moshi): MessagesFetchRequest {
    val adapter = moshi.adapter(MessagesFetchRequest::class.java)
    return adapter.fromJson(this) ?: throw IllegalArgumentException("Invalid JSON for MessagesFetchRequest")
}
