package chat.sphinx.concept_repository_dashboard

import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.dashboard.InviteId
import chat.sphinx.wrapper_common.dashboard.RestoreProgress
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_invite.Invite
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_podcast.FeedRecommendation
import chat.sphinx.wrapper_podcast.Podcast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface RepositoryDashboard {
    suspend fun getAccountBalance(): StateFlow<NodeBalance?>

    val getAllChats: Flow<List<Chat>>
    val getAllContactChats: Flow<List<Chat>>
    val getAllTribeChats: Flow<List<Chat>>
    fun getConversationByContactId(contactId: ContactId): Flow<Chat?>

    fun getUnseenMessagesByChatId(chatId: ChatId): Flow<Long?>
    fun getUnseenMentionsByChatId(chatId: ChatId): Flow<Long?>
    fun getUnseenActiveConversationMessagesCount(): Flow<Long?>
    fun getUnseenTribeMessagesCount(): Flow<Long?>

    val accountOwner: StateFlow<Contact?>
    val getAllNotBlockedContacts: Flow<List<Contact>>
    val getAllInvites: Flow<List<Invite>>
    fun getContactById(contactId: ContactId): Flow<Contact?>
    var updatedContactIds: MutableList<ContactId>

    fun getMessageById(messageId: MessageId): Flow<Message?>
    fun getInviteById(inviteId: InviteId): Flow<Invite?>

    suspend fun payForInvite(invite: Invite)
    suspend fun deleteInvite(invite: Invite): Response<Any, ResponseError>

    fun getAllFeedsOfType(feedType: FeedType): Flow<List<Feed>>
    fun getAllFeeds(): Flow<List<Feed>>

    fun getRecommendedFeeds(): Flow<List<FeedRecommendation>>

    suspend fun authorizeExternal(
        relayUrl: String,
        host: String,
        challenge: String
    ): Response<Boolean, ResponseError>

    suspend fun authorizeStakwork(
        host: String,
        id: String,
        challenge: String
    ): Response<String, ResponseError>

    suspend fun savePeopleProfile(
        body: String
    ): Response<Boolean, ResponseError>

    suspend fun deletePeopleProfile(
        body: String
    ): Response<Boolean, ResponseError>

    suspend fun redeemBadgeToken(
        body: String
    ): Response<Boolean, ResponseError>

    val networkRefreshBalance: Flow<LoadResponse<Boolean, ResponseError>>
    val networkRefreshContacts: Flow<LoadResponse<Boolean, ResponseError>>
    val networkRefreshLatestContacts: Flow<LoadResponse<RestoreProgress, ResponseError>>
    val networkRefreshMessages: Flow<LoadResponse<RestoreProgress, ResponseError>>

    suspend fun didCancelRestore()

    fun getAndSaveTransportKey()
    fun getOrCreateHMacKey()
}
