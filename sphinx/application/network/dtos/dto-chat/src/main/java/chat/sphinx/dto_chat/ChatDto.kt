package chat.sphinx.dto_chat

import chat.sphinx.wrapper_chat.ChatGroupKey
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.Deleted
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.Seen
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.Sat

// TODO: Move to Presenter Object
//@Suppress("NOTHING_TO_INLINE")
//inline fun ChatDto.isMuted(): Boolean =
//    isMuted.isTrue()
//
//@Suppress("NOTHING_TO_INLINE")
//inline fun ChatDto.isDeleted(): Boolean =
//    deleted.isTrue()
//
//@Suppress("NOTHING_TO_INLINE")
//inline fun ChatDto.isListed(): Boolean =
//    unlisted.isTrue()
//
//@Suppress("NOTHING_TO_INLINE")
//inline fun ChatDto.isPrivate(): Boolean =
//    private.isTrue()
//
//@Suppress("NOTHING_TO_INLINE")
//inline fun ChatDto.hasBeenSeen(): Boolean =
//    seen.isTrue()
//
//@Suppress("NOTHING_TO_INLINE")
//inline fun ChatDto.hasPendingContacts(): Boolean =
//    pendingContactIds.isNotEmpty()

class ChatDto(
    val id: ChatId,
    val uuid: ChatUUID,
    val name: ChatName?,
    val photoUrl: PhotoUrl?,
    val type: ChatType,
    val status: ChatStatus,
    val contactIds: List<ContactId>,
    val isMuted: ChatMuted,
    val createdAt: DateTime,
    val updatedAt: DateTime,
    val deleted: Deleted,
    val groupKey: ChatGroupKey?,
    val host: ChatHost?,
    val priceToJoin: Sat,
    val pricePerMessage: Sat,
    val escrowAmount: Sat?,
    val escrowMillis: Long?, // milliseconds
    val unlisted: ChatUnlisted,
    val private: ChatPrivate,
    val ownerPubKey: LightningNodePubKey?,
    val seen: Seen,
    val appUrl: AppUrl?,
    val feedUrl: FeedUrl?,
    val meta: ChatMetaData?,
    val myPhotoUrl: PhotoUrl?,
    val myAlias: ChatAlias?,
//    val skipBroadcastJoins: Boolean,
    val pendingContactIds: List<ContactId>?,
)
