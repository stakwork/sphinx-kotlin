package chat.sphinx.concept_repository_dashboard

import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.ExternalAuthorizeLink
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.dashboard.InviteId
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_invite.Invite
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_message.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface RepositoryDashboard {
    suspend fun getAccountBalance(): StateFlow<NodeBalance?>

    val getAllChats: Flow<List<Chat>>
    fun getUnseenMessagesByChatId(chatId: ChatId): Flow<Long?>

    val accountOwner: StateFlow<Contact?>
    val getAllContacts: Flow<List<Contact>>
    val getAllInvites: Flow<List<Invite>>
    fun getContactById(contactId: ContactId): Flow<Contact?>
    var updatedContactIds: MutableList<ContactId>

    fun getMessageById(messageId: MessageId): Flow<Message?>
    fun getInviteById(inviteId: InviteId): Flow<Invite?>

    suspend fun payForInvite(invite: Invite)
    suspend fun deleteInvite(invite: Invite): Response<Any, ResponseError>

    suspend fun authorizeExternal(
        relayUrl: String,
        host: String,
        challenge: String
    ): Response<Boolean, ResponseError>

    suspend fun saveProfile(
        relayUrl: String,
        host: String,
        key: String
    ): Response<Boolean, ResponseError>

    val networkRefreshBalance: Flow<LoadResponse<Boolean, ResponseError>>
    val networkRefreshContacts: Flow<LoadResponse<Boolean, ResponseError>>
    val networkRefreshMessages: Flow<LoadResponse<Boolean, ResponseError>>
}