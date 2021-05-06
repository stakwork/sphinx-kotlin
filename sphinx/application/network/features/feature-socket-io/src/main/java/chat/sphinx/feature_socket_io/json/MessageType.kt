package chat.sphinx.feature_socket_io.json

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import java.io.IOException

@Suppress("NOTHING_TO_INLINE")
@Throws(IOException::class, JsonDataException::class)
internal inline fun Moshi.getMessageType(json: String): MessageType =
    adapter(MessageType::class.java)
        .fromJson(json)
        ?: throw JsonDataException("Failed to convert SocketIO Message.type Json")

@JsonClass(generateAdapter = true)
internal data class MessageType(val type: String)
