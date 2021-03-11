package chat.sphinx.chat_dtos

import chat.sphinx.chat_dtos.model.*
import chat.sphinx.common.DateTime
import chat.sphinx.chat_dtos.model.ChatGroupKey
import chat.sphinx.common.PhotoUrl
import chat.sphinx.common.Sats
import chat.sphinx.common.chat.ChatId
import chat.sphinx.common.contact.ContactId
import chat.sphinx.common.contact.NodePubKey

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
    val deleted: ChatDeleted,
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