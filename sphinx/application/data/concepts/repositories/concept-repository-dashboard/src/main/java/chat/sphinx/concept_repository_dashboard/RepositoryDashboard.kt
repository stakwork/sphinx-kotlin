package chat.sphinx.concept_repository_dashboard

import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.attachment_authentication.AuthenticationToken
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_message.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface RepositoryDashboard {
    suspend fun authenticateForAttachments(): AuthenticationToken?

    suspend fun getAccountBalance(): StateFlow<NodeBalance?>

    val getAllChats: Flow<List<Chat>>
    fun getUnseenMessagesByChatId(chatId: ChatId): Flow<Long?>

    val accountOwner: StateFlow<Contact?>
    val getAllContacts: Flow<List<Contact>>
    fun getContactById(contactId: ContactId): Flow<Contact?>
    var updatedContactIds: MutableList<ContactId>

    fun getMessageById(messageId: MessageId): Flow<Message?>

    val networkRefreshBalance: Flow<LoadResponse<Boolean, ResponseError>>
    val networkRefreshContacts: Flow<LoadResponse<Boolean, ResponseError>>
    val networkRefreshMessages: Flow<LoadResponse<Boolean, ResponseError>>
}