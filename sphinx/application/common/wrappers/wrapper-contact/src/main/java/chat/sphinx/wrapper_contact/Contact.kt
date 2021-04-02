package chat.sphinx.wrapper_contact

import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.Deleted
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.invite.InviteId
import chat.sphinx.wrapper_common.lightning.LightningNodeAlias
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.Sat

data class Contact(
    val id: ContactId,
    val pubKey: LightningNodePubKey,
    val nodeAlias: LightningNodeAlias?,
    val alias: ContactAlias,
    val photoUrl: PhotoUrl?,
    val privatePhoto: PrivatePhoto,
    val isOwner: Owner,
    val deleted: Deleted,
    val status: ContactStatus,
    val contactKey: ContactKey,
    val deviceId: DeviceId,
    val createdAt: DateTime,
    val updatedAt: DateTime,
    val notificationSound: NotificationSound?,
    val tipAmount: Sat?,
    val inviteId: InviteId?,
)
