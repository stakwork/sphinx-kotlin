package chat.sphinx.concept_repository_contact

import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_contact.Contact
import kotlinx.coroutines.flow.Flow

/**
 * All [Contact]s are cached to the DB such that a network refresh will update
 * them, and thus proc and [Flow] being collected.
 * */
interface ContactRepository {
    suspend fun getContacts(): Flow<List<Contact>>
    suspend fun getContactById(contactId: ContactId): Flow<Contact?>
    suspend fun getOwner(): Flow<Contact?>
    fun networkRefreshContacts(): Flow<LoadResponse<Boolean, ResponseError>>
}
