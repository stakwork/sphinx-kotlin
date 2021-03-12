package chat.sphinx.wrapper_message

import chat.sphinx.wrapper_common.contact.ContactId

class RemoteMessageContent(
    val contactId: ContactId,
    val messageContent: MessageContent
)
