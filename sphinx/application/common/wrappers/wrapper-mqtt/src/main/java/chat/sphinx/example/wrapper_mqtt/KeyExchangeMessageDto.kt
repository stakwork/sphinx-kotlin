package chat.sphinx.example.wrapper_mqtt

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class KeyExchangeMessageDto(
    val uuid: String,
    val type: Int,
    val sender: Sender,
    val message: Message
)

@JsonClass(generateAdapter = true)
data class Message(
    val content: String?
)

@JsonClass(generateAdapter = true)
data class Sender(
    val pubkey: String,
    val route_hint: String?,
    val contact_pubkey: String?,
    val contact_route_hint: String?,
    val alias: String,
    val photo_url: String
)

@Throws(AssertionError::class)
fun KeyExchangeMessageDto.toJson(moshi: Moshi): String {
    val adapter = moshi.adapter(KeyExchangeMessageDto::class.java)
    return adapter.toJson(this)
}

fun String.toKeyExchangeMessageDtoOrNull(moshi: Moshi): KeyExchangeMessageDto? {
    return try {
        this.toKeyExchangeMessageDto(moshi)
    } catch (e: Exception) {
        null
    }
}

@Throws(JsonDataException::class)
fun String.toKeyExchangeMessageDto(moshi: Moshi): KeyExchangeMessageDto {
    val adapter = moshi.adapter(KeyExchangeMessageDto::class.java)
    return adapter.fromJson(this) ?: throw IllegalArgumentException("Invalid JSON for KeyExchangeMessageDto")
}
