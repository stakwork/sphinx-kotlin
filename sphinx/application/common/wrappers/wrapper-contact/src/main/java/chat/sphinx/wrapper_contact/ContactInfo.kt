package chat.sphinx.wrapper_contact

import chat.sphinx.wrapper_common.contact.ContactIndex
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey

data class ContactInfo(
    val childPubKey: LightningNodePubKey,
    val contactIndex: ContactIndex,
    val messagesFetchRequest: String
)
