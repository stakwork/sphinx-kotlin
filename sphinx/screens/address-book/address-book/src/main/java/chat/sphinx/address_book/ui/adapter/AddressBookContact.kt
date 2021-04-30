package chat.sphinx.address_book.ui.adapter

import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.time
import chat.sphinx.wrapper_contact.Contact

class AddressBookContact(
    private val contact: Contact
) {
    val contactName: String?
        get() = contact.alias?.value

    val photoUrl: PhotoUrl?
        get() = contact.photoUrl ?: contact.photoUrl

    val sortBy: String?
        get() = contact?.alias?.value
}