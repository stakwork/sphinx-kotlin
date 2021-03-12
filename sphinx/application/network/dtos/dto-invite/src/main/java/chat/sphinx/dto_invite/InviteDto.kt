package chat.sphinx.dto_invite

import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.lightning.Sats
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.invite.InviteId
import chat.sphinx.dto_invite.model.InviteStatus
import chat.sphinx.dto_invite.model.InviteString
import chat.sphinx.dto_invite.model.LNInvoice

class InviteDto(
    val id: InviteId,
    val inviteString: InviteString,
    val invoice: LNInvoice,
    val welcomeMessage: String,
    val contactId: ContactId,
    val status: InviteStatus,
    val price: Sats,
    val createdAt: DateTime,
    val updatedAt: DateTime,
)
