package chat.sphinx.feature_repository.util

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.concept_network_query_invite.model.InviteDto
import chat.sphinx.concept_network_query_lightning.model.balance.BalanceDto
import chat.sphinx.concept_network_query_message.model.MessageDto
import chat.sphinx.conceptcoredb.SphinxDatabaseQueries
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.*
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.invite.InviteId
import chat.sphinx.wrapper_common.invite.toInviteStatus
import chat.sphinx.wrapper_common.lightning.*
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_contact.*
import chat.sphinx.wrapper_invite.InviteString
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_message.media.MediaToken
import chat.sphinx.wrapper_message.media.toMediaKey
import chat.sphinx.wrapper_message.media.toMediaKeyDecrypted
import chat.sphinx.wrapper_message.media.toMediaType
import chat.sphinx.wrapper_rsa.RsaPublicKey
import com.squareup.moshi.Moshi
import com.squareup.sqldelight.TransactionCallbacks

@Suppress("NOTHING_TO_INLINE")
inline fun BalanceDto.toNodeBalanceOrNull(): NodeBalance? =
    try {
        toNodeBalance()
    } catch (e: IllegalArgumentException) {
        null
    }

@Suppress("NOTHING_TO_INLINE")
inline fun BalanceDto.toNodeBalance(): NodeBalance =
    NodeBalance(
        Sat(reserve),
        Sat(full_balance),
        Sat(balance),
        Sat(pending_open_balance),
    )

inline val MessageDto.updateChatDboLatestMessage: Boolean
    get() = type.toMessageType().show           &&
            type != MessageType.BOT_RES         &&
            status != MessageStatus.DELETED

@Suppress("NOTHING_TO_INLINE")
inline fun MessageDto.updateChatDboLatestMessage(
    chatId: ChatId,
    latestMessageUpdatedTimeMap: MutableMap<ChatId, DateTime>,
    queries: SphinxDatabaseQueries,
) {
    val dateTime = created_at.toDateTime()

    if (
        updateChatDboLatestMessage &&
        (latestMessageUpdatedTimeMap[chatId]?.time ?: 0L) <= dateTime.time
    ){
        queries.chatUpdateLatestMessage(
            MessageId(id),
            chatId
        )
        latestMessageUpdatedTimeMap[chatId] = dateTime
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun MessageDto.updateChatDboLatestMessage(
    chatId: ChatId,
    latestMessageUpdatedTimeMap: SynchronizedMap<ChatId, DateTime>,
    queries: SphinxDatabaseQueries,
) {
    latestMessageUpdatedTimeMap.withLock { map ->
        updateChatDboLatestMessage(chatId, map, queries)
    }
}

@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun SphinxDatabaseQueries.upsertChat(
    dto: ChatDto,
    moshi: Moshi,
    chatSeenMap: SynchronizedMap<ChatId, Seen>
) {
    val seen = dto.seenActual.toSeen()

    chatUpsert(
        dto.name?.toChatName(),
        dto.photo_url?.toPhotoUrl(),
        dto.status.toChatStatus(),
        dto.contact_ids.map { ContactId(it) },
        dto.isMutedActual.toChatMuted(),
        dto.group_key?.toChatGroupKey(),
        dto.host?.toChatHost(),
        dto.price_per_message?.toSat(),
        dto.escrow_amount?.toSat(),
        dto.unlistedActual.toChatUnlisted(),
        dto.privateActual.toChatPrivate(),
        dto.owner_pub_key?.toLightningNodePubKey(),
        seen,
        dto.meta?.toChatMetaDataOrNull(moshi),
        dto.my_photo_url?.toPhotoUrl(),
        dto.my_alias?.toChatAlias(),
        dto.pending_contact_ids?.map { ContactId(it) },
        ChatId(dto.id),
        ChatUUID(dto.uuid),
        dto.type.toChatType(),
        dto.created_at.toDateTime()
    )

    chatSeenMap.withLock { it[ChatId(dto.id)] = seen }
}

@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun TransactionCallbacks.upsertContact(dto: ContactDto, queries: SphinxDatabaseQueries) {

    if (dto.fromGroupActual) {
        return
    }

    queries.contactUpsert(
        dto.route_hint?.toLightningRouteHint(),
        dto.public_key?.toLightningNodePubKey(),
        dto.node_alias?.toLightningNodeAlias(),
        dto.alias.toContactAlias(),
        dto.photo_url?.toPhotoUrl(),
        dto.privatePhotoActual.toPrivatePhoto(),
        dto.status.toContactStatus(),
        dto.contact_key?.let { RsaPublicKey(it.toCharArray()) },
        dto.device_id?.toDeviceId(),
        dto.updated_at.toDateTime(),
        dto.notification_sound?.toNotificationSound(),
        dto.tip_amount?.toSat(),
        dto.invite?.id?.let { InviteId(it) },
        dto.invite?.status?.toInviteStatus(),
        ContactId(dto.id),
        dto.isOwnerActual.toOwner(),
        dto.created_at.toDateTime()
    )
    dto.invite?.let { queries.upsertInvite(it) }
}

@Suppress("NOTHING_TO_INLINE")
inline fun SphinxDatabaseQueries.upsertInvite(dto: InviteDto) {
    inviteUpsert(
        InviteString(dto.invite_string),
        dto.invoice?.toLightningPaymentRequest(),
        dto.status.toInviteStatus(),
        dto.price?.toSat(),
        InviteId(dto.id),
        ContactId(dto.contact_id),
        dto.created_at.toDateTime(),
    )
}

@Suppress("SpellCheckingInspection")
fun TransactionCallbacks.upsertMessage(dto: MessageDto, queries: SphinxDatabaseQueries) {

    val chatId: ChatId = dto.chat_id?.let {
        ChatId(it)
    } ?: dto.chat?.id?.let {
        ChatId(it)
    } ?: ChatId(ChatId.NULL_CHAT_ID.toLong())

    queries.messageUpsert(
        dto.status.toMessageStatus(),
        dto.seenActual.toSeen(),
        dto.sender_alias?.toSenderAlias(),
        dto.sender_pic?.toPhotoUrl(),
        dto.original_muid?.toMessageMUID(),
        dto.reply_uuid?.toReplyUUID(),
        MessageId(dto.id),
        dto.uuid?.toMessageUUID(),
        chatId,
        dto.type.toMessageType(),
        ContactId(dto.sender),
        dto.receiver?.let { ContactId(it) },
        Sat(dto.amount),
        dto.payment_hash?.toLightningPaymentHash(),
        dto.payment_request?.toLightningPaymentRequest(),
        dto.date.toDateTime(),
        dto.expiration_date?.toDateTime(),
        dto.message_content?.toMessageContent(),
        dto.messageContentDecrypted?.toMessageContentDecrypted(),
    )

    dto.media_token?.let { mediaToken ->
        dto.media_type?.let { mediaType ->

            if (mediaToken.isEmpty() || mediaType.isEmpty()) return

            queries.messageMediaUpsert(
                dto.media_key?.toMediaKey(),
                mediaType.toMediaType(),
                MediaToken(mediaToken),
                MessageId(dto.id),
                chatId,
                dto.mediaKeyDecrypted?.toMediaKeyDecrypted()
            )

        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun SphinxDatabaseQueries.updateSeen(chatId: ChatId) {
    transaction {
        chatUpdateSeen(Seen.True, chatId)
        messageUpdateSeenByChatId(Seen.True, chatId)
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun TransactionCallbacks.deleteChatById(
    chatId: ChatId?,
    queries: SphinxDatabaseQueries,
    latestMessageUpdatedTimeMap: SynchronizedMap<ChatId, DateTime>?,
) {
    queries.messageDeleteByChatId(chatId ?: return)
    queries.messageMediaDeleteByChatId(chatId)
    queries.chatDeleteById(chatId)
    latestMessageUpdatedTimeMap?.withLock { it.remove(chatId) }
}

@Suppress("NOTHING_TO_INLINE")
inline fun TransactionCallbacks.deleteContactById(
    contactId: ContactId,
    queries: SphinxDatabaseQueries
) {
    queries.contactDeleteById(contactId)
    queries.inviteDeleteByContactId(contactId)
}
