package chat.sphinx.wrapper_contact

import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.dashboard.InviteId
import chat.sphinx.wrapper_common.invite.InviteStatus
import chat.sphinx.wrapper_common.isTrue
import chat.sphinx.wrapper_common.lightning.LightningNodeAlias
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_rsa.RsaPublicKey
import java.net.MalformedURLException
import java.net.URL

@Suppress("NOTHING_TO_INLINE")
inline fun Contact.isInviteContact(): Boolean =
    status.isPending() && inviteId != null

@Suppress("NOTHING_TO_INLINE")
inline fun Contact.isOnVirtualNode(): Boolean =
    routeHint != null && routeHint.value.isNotEmpty()

inline val Contact.avatarUrl: URL?
    get() {
        return try {
            if (photoUrl?.value != null) {
                URL(photoUrl!!.value)
            } else {
                null
            }
        } catch (e: MalformedURLException) {
            null
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Contact.getColorKey(): String {
    return "contact-${id.value}-color"
}

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
    val inviteStatus: InviteStatus?,
)
