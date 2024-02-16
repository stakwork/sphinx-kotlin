package chat.sphinx.feature_socket_io.json

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.concept_network_query_invite.model.InviteDto
import chat.sphinx.concept_network_query_message.model.MessageDto
import chat.sphinx.concept_socket_io.GroupDto
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
    val jsonResolved: String = if (
        adapter == MessageResponse.ResponseGroup::class.java &&
        json.contains("\"contact\":{}")
    ) {
        json.replace("\"contact\":{}", "\"contact\":null")
    } else {
        json
    }

    return adapter(adapter)
        .fromJson(jsonResolved)
        ?.response
        ?: throw JsonDataException("Failed to convert SocketIO Message.response Json to ${adapter.simpleName}")
}

@JsonClass(generateAdapter = true)
internal data class GroupDtoImpl(
    override val chat: ChatDto,
    override val contact: ContactDto?,
    override val message: MessageDto
): GroupDto()

internal sealed class MessageResponse<T> {
    abstract val response: T

    @JsonClass(generateAdapter = true)
    internal class ResponseChat(override val response: ChatDto): MessageResponse<ChatDto>()

    @JsonClass(generateAdapter = true)
    internal class ResponseContact(override val response: ContactDto): MessageResponse<ContactDto>()

    @JsonClass(generateAdapter = true)
    internal class ResponseGroup(override val response: GroupDtoImpl): MessageResponse<GroupDtoImpl>()

    @JsonClass(generateAdapter = true)
    internal class ResponseInvite(override val response: InviteDto): MessageResponse<InviteDto>()

    @JsonClass(generateAdapter = true)
    internal class ResponseMessage(override val response: MessageDto): MessageResponse<MessageDto>()
}

