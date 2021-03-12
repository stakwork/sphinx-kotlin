package chat.sphinx.wrapper_message

import chat.sphinx.wrapper_common.contact.ContactId

class MessageStatusMap(
    val contactId: ContactId,
    val status: MessageStatus
)
