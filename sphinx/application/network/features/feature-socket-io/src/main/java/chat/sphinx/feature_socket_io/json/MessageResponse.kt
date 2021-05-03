package chat.sphinx.feature_socket_io.json

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.concept_network_query_invite.model.InviteDto
import chat.sphinx.concept_network_query_lightning.model.invoice.InvoiceDto
import chat.sphinx.concept_network_query_message.model.MessageDto
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import java.io.IOException


@Suppress("NOTHING_TO_INLINE")
@Throws(IOException::class, JsonDataException::class)
internal inline fun<T: Any, V: MessageResponse<T>> Moshi.getMessageResponse(
    adapter: Class<V>,
    json: String
): T {
    return adapter(adapter)
        .fromJson(json)
        ?.response
        ?: throw JsonDataException("Failed to convert SocketIO Message.response Json to ${adapter.simpleName}")
}

internal sealed class MessageResponse<T> {
    abstract val response: T

    @JsonClass(generateAdapter = true)
    internal class ResponseChat(override val response: ChatDto): MessageResponse<ChatDto>()

    @JsonClass(generateAdapter = true)
    internal class ResponseContact(override val response: ContactDto): MessageResponse<ContactDto>()

    @JsonClass(generateAdapter = true)
    internal class ResponseInvite(override val response: InviteDto): MessageResponse<InviteDto>()

    @JsonClass(generateAdapter = true)
    internal class ResponseInvoice(override val response: InvoiceDto): MessageResponse<InvoiceDto>()

    @JsonClass(generateAdapter = true)
    internal class ResponseMessage(override val response: MessageDto): MessageResponse<MessageDto>()
}

