package chat.sphinx.concept_repository_contact

import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.invite.InviteId
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_invite.Invite
import kotlinx.coroutines.flow.Flow

/**
 * All [Contact]s are cached to the DB such that a network refresh will update
 * them, and thus proc and [Flow] being collected.
 * */
interface ContactRepository {
    suspend fun getContacts(): Flow<List<Contact>>
    suspend fun getContactById(contactId: ContactId): Flow<Contact?>

    suspend fun getInviteById(inviteId: InviteId): Flow<Invite?>
    suspend fun getInviteByContactId(contactId: ContactId): Flow<Invite?>

    suspend fun getOwner(): Flow<Contact?>
    fun networkRefreshContacts(): Flow<LoadResponse<Boolean, ResponseError>>

    suspend fun deleteContactById(contactId: ContactId): Response<Any, ResponseError>
}
