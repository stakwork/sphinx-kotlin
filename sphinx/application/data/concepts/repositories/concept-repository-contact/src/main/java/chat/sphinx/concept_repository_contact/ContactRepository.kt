package chat.sphinx.concept_repository_contact

import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.dashboard.InviteId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_contact.*
import chat.sphinx.wrapper_invite.Invite
import chat.sphinx.wrapper_io_utils.InputStreamProvider
import chat.sphinx.wrapper_message_media.MediaType
import io.matthewnelson.crypto_common.clazzes.Password
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream

/**
 * All [Contact]s are cached to the DB such that a network refresh will update
 * them, and thus proc and [Flow] being collected.
 * */
interface ContactRepository {
    val accountOwner: StateFlow<Contact?>

    fun createContact(
        contactAlias: ContactAlias,
        lightningNodePubKey: LightningNodePubKey,
        lightningRouteHint: LightningRouteHint?,
        contactKey: ContactKey? = null,
        photoUrl: PhotoUrl? = null
    ): Flow<LoadResponse<Any, ResponseError>>

    suspend fun connectToContact(
        contactAlias: ContactAlias,
        lightningNodePubKey: LightningNodePubKey,
        lightningRouteHint: LightningRouteHint?,
        contactKey: ContactKey,
        message: String,
        photoUrl: PhotoUrl?,
        priceToMeet: Sat,
    ): Response<ContactId?, ResponseError>

    val getAllContacts: Flow<List<Contact>>
    fun getContactById(contactId: ContactId): Flow<Contact?>
    fun getContactByPubKey(pubKey: LightningNodePubKey): Flow<Contact?>
    suspend fun getAllContactsByIds(contactIds: List<ContactId>): List<Contact>

    fun getInviteByContactId(contactId: ContactId): Flow<Invite?>
    fun getInviteById(inviteId: InviteId): Flow<Invite?>

    fun createNewInvite(nickname: String, welcomeMessage: String): Flow<LoadResponse<Any, ResponseError>>

    val networkRefreshContacts: Flow<LoadResponse<Boolean, ResponseError>>
    var updatedContactIds: MutableList<ContactId>

    suspend fun deleteContactById(contactId: ContactId): Response<Any, ResponseError>
    suspend fun updateOwnerDeviceId(deviceId: DeviceId): Response<Any, ResponseError>
    suspend fun updateOwnerNameAndKey(name: String, contactKey: Password): Response<Any, ResponseError>
    suspend fun updateOwner(alias: String?, privatePhoto: PrivatePhoto?, tipAmount: Sat?): Response<Any, ResponseError>

    // TODO: add chatId to argument to update alias photo
    suspend fun updateProfilePic(
//        chatId: ChatId?,
        stream: InputStreamProvider,
        mediaType: MediaType,
        fileName: String,
        contentLength: Long?
    ): Response<Any, ResponseError>
}
