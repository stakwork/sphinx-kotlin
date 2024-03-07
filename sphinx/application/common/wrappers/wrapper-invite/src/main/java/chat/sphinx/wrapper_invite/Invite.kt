package chat.sphinx.wrapper_invite

import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.dashboard.InviteId
import chat.sphinx.wrapper_common.invite.InviteStatus
import chat.sphinx.wrapper_common.lightning.LightningPaymentRequest
import chat.sphinx.wrapper_common.lightning.Sat

data class Invite(
    val id: InviteId,
    val inviteString: InviteString,
    val inviteCode: InviteCode?,
    val paymentRequest: LightningPaymentRequest?,
    val contactId: ContactId,
    val status: InviteStatus,
    val price: Sat?,
    val createdAt: DateTime,
)
