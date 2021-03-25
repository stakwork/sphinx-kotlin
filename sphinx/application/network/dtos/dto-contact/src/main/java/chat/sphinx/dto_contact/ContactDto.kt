package chat.sphinx.dto_contact

import chat.sphinx.dto_invite.InviteDto
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.Deleted
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.lightning.LightningNodeAlias
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_contact.*

// TODO: Move to Presenter Object
//@Suppress("NOTHING_TO_INLINE")
//inline fun ContactDto.isOwner(): Boolean =
//    isOwner.isTrue()
//
//@Suppress("NOTHING_TO_INLINE")
//inline fun ContactDto.photoIsPrivate(): Boolean =
//    privatePhoto.isTrue()
//
//@Suppress("NOTHING_TO_INLINE")
//inline fun ContactDto.isDeleted(): Boolean =
//    deleted.isTrue()
//
//@Suppress("NOTHING_TO_INLINE")
//inline fun ContactDto.isFromGroup(): Boolean =
//    fromGroup.isTrue()

class ContactDto(
    val id: ContactId,
//    val routHint: String?,
    val pubKey: LightningNodePubKey,
    val nodeAlias: LightningNodeAlias?,
    val alias: ContactAlias,
    val photoUrl: PhotoUrl?,
    val privatePhoto: PrivatePhoto,
    val isOwner: Owner, // User's contact info. authToken, deviceId, lastActive, and notificationSound will always be null for non-owner contact
    val deleted: Deleted,
    val authToken: AuthToken?,
    val remoteId: Int?, // unused, always null
    val status: ContactStatus,
    val contactKey: ContactKey,
    val deviceId: DeviceId?,
    val createdAt: DateTime,
    val updatedAt: DateTime,
    val fromGroup: ContactFromGroup,
    val notificationSound: NotificationSound?,
//    val lastActive: DateTime?,
    val tipAmount: Sat?,
    val invite: InviteDto?,
)
