package chat.sphinx.dto_contact

import chat.sphinx.dto_common.*
import chat.sphinx.dto_common.contact.*
import chat.sphinx.dto_contact.model.*

@Suppress("NOTHING_TO_INLINE")
inline fun ContactDto.isOwner(): Boolean =
    isOwner.isTrue()

@Suppress("NOTHING_TO_INLINE")
inline fun ContactDto.photoIsPrivate(): Boolean =
    privatePhoto.isTrue()

@Suppress("NOTHING_TO_INLINE")
inline fun ContactDto.isDeleted(): Boolean =
    deleted.isTrue()

@Suppress("NOTHING_TO_INLINE")
inline fun ContactDto.isFromGroup(): Boolean =
    fromGroup.isTrue()

class ContactDto(
    val id: ContactId,
    val pubKey: NodePubKey,
    val nodeAlias: NodeAlias?,
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
    val lastActive: DateTime?,
    val tipAmount: Sats?,
//    val invite: InviteDto?, // TODO: Create InviteDto
)
