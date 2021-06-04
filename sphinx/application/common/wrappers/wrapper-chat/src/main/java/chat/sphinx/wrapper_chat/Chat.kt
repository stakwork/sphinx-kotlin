package chat.sphinx.wrapper_chat

import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.Seen
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.isTrue
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.MessageId

@Suppress("NOTHING_TO_INLINE")
inline fun Chat.isMuted(): Boolean =
    isMuted.isTrue()

@Suppress("NOTHING_TO_INLINE")
inline fun Chat.isUnlisted(): Boolean =
    unlisted.isTrue()

@Suppress("NOTHING_TO_INLINE")
inline fun Chat.isPrivateTribe(): Boolean =
    privateTribe.isTrue()

@Suppress("NOTHING_TO_INLINE")
inline fun Chat.hasBeenSeen(): Boolean =
    seen.isTrue()

@Suppress("NOTHING_TO_INLINE")
inline fun Chat.hasPendingContacts(): Boolean =
    !pendingContactIds.isNullOrEmpty()

@Suppress("NOTHING_TO_INLINE")
inline fun Chat.isTribe(): Boolean =
    type.isTribe()

@Suppress("NOTHING_TO_INLINE")
inline fun Chat.getColorKey(): String {
    return "chat-${id}-color"
}

data class Chat(
    val id: ChatId,
    val uuid: ChatUUID,
    val name: ChatName?,
    val photoUrl: PhotoUrl?,
    val type: ChatType,
    val status: ChatStatus,
    val contactIds: List<ContactId>,
    val isMuted: ChatMuted,
    val createdAt: DateTime,
    val groupKey: ChatGroupKey?,
    val host: ChatHost?,
    val pricePerMessage: Sat?,
    val escrowAmount: Sat?,
    val unlisted: ChatUnlisted,
    val privateTribe: ChatPrivate,
    val ownerPubKey: LightningNodePubKey?,
    val seen: Seen,
    val metaData: ChatMetaData?,
    val myPhotoUrl: PhotoUrl?,
    val myAlias: ChatAlias?,
    val pendingContactIds: List<ContactId>?,
    val latestMessageId: MessageId?,
)
