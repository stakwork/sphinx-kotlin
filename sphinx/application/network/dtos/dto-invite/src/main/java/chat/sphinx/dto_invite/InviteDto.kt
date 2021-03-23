package chat.sphinx.dto_invite

import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.invite.InviteId
import chat.sphinx.wrapper_invite.InviteStatus
import chat.sphinx.wrapper_invite.InviteString
import chat.sphinx.wrapper_common.lightning.LightningPaymentRequest

class InviteDto(
    val id: InviteId,
    val inviteString: InviteString,
    val invoice: LightningPaymentRequest,
    val welcomeMessage: String,
    val contactId: ContactId,
    val status: InviteStatus,
    val price: Sat,
    val createdAt: DateTime,
    val updatedAt: DateTime,
)
