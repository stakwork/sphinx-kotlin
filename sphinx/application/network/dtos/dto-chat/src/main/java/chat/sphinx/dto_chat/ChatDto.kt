package chat.sphinx.dto_chat

import chat.sphinx.dto_chat.model.*
import chat.sphinx.dto_common.DateTime
import chat.sphinx.dto_chat.model.ChatGroupKey
import chat.sphinx.dto_common.Deleted
import chat.sphinx.dto_common.PhotoUrl
import chat.sphinx.dto_common.Sats
import chat.sphinx.dto_common.chat.ChatId
import chat.sphinx.dto_common.contact.ContactId
import chat.sphinx.dto_common.contact.NodePubKey

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
    val priceToJoin: Sats,
    val pricePerMessage: Sats,
    val escrowAmount: Sats,
    val escrowMillis: Long,
    val unlisted: ChatListed,
    val private: ChatPrivate,
    val ownerPubKey: NodePubKey?,
    val seen: ChatSeen,
    val appUrl: AppUrl?,
    val feedUrl: FeedUrl?,
    val meta: ChatMetaData?,
    val myPhotoUrl: PhotoUrl?,
    val myAlias: ChatAlias?
)
