package chat.sphinx.wrapper_contact

import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.invite.InviteId
import chat.sphinx.wrapper_common.lightning.LightningNodeAlias
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_rsa.RsaPublicKey

data class Contact(
    val id: ContactId,
    val routeHint: LightningRouteHint?,
    val nodePubKey: LightningNodePubKey?,
    val nodeAlias: LightningNodeAlias?,
    val alias: ContactAlias?,
    val photoUrl: PhotoUrl?,
    val privatePhoto: PrivatePhoto,
    val isOwner: Owner,
    val status: ContactStatus,
    val rsaPublicKey: RsaPublicKey?,
    val deviceId: DeviceId?,
    val createdAt: DateTime,
    val updatedAt: DateTime,
    val fromGroup: ContactFromGroup,
    val notificationSound: NotificationSound?,
    val tipAmount: Sat?,
    val inviteId: InviteId?,
)
