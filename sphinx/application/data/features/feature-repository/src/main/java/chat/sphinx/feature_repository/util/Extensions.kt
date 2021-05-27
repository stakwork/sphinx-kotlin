package chat.sphinx.feature_repository.util

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.concept_network_query_invite.model.InviteDto
import chat.sphinx.concept_network_query_lightning.model.balance.BalanceDto
import chat.sphinx.concept_network_query_message.model.MessageDto
import chat.sphinx.conceptcoredb.SphinxDatabaseQueries
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.*
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.dashboard.InviteId
import chat.sphinx.wrapper_common.invite.InviteStatus
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
inline fun TransactionCallbacks.updateChatDboLatestMessage(
    messageDto: MessageDto,
    chatId: ChatId,
    latestMessageUpdatedTimeMap: MutableMap<ChatId, DateTime>,
    queries: SphinxDatabaseQueries,
) {
    val dateTime = messageDto.created_at.toDateTime()

    if (
        messageDto.updateChatDboLatestMessage &&
        (latestMessageUpdatedTimeMap[chatId]?.time ?: 0L) <= dateTime.time
    ){
        queries.chatUpdateLatestMessage(
            MessageId(messageDto.id),
            chatId,
        )
        queries.dashboardUpdateLatestMessage(
            dateTime,
            MessageId(messageDto.id),
            chatId,
        )
        latestMessageUpdatedTimeMap[chatId] = dateTime
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun TransactionCallbacks.updateChatDboLatestMessage(
    messageDto: MessageDto,
    chatId: ChatId,
    latestMessageUpdatedTimeMap: SynchronizedMap<ChatId, DateTime>,
    queries: SphinxDatabaseQueries,
) {
    latestMessageUpdatedTimeMap.withLock { map ->
        updateChatDboLatestMessage(messageDto, chatId, map, queries)
    }
}

@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun TransactionCallbacks.upsertChat(
    dto: ChatDto,
    moshi: Moshi,
    chatSeenMap: SynchronizedMap<ChatId, Seen>,
    queries: SphinxDatabaseQueries,
) {
    val seen = dto.seenActual.toSeen()
    val chatId = ChatId(dto.id)
    val createdAt = dto.created_at.toDateTime()

    queries.chatUpsert(
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
        chatId,
        ChatUUID(dto.uuid),
        dto.type.toChatType(),
        createdAt,
    )

    queries.dashboardInsert(chatId, createdAt)

    if (dto.type.toChatType().isConversation()) {
        dto.contact_ids.elementAtOrNull(1)?.let { contactId ->
            queries.dashboardUpdateIncludeInReturn(false, ContactId(contactId))
        }
    }

    chatSeenMap.withLock { it[ChatId(dto.id)] = seen }
}

@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun TransactionCallbacks.upsertContact(dto: ContactDto, queries: SphinxDatabaseQueries) {

    if (dto.fromGroupActual) {
        return
    }

    val contactId = ContactId(dto.id)
    val createdAt = dto.created_at.toDateTime()
    val isOwner = dto.isOwnerActual.toOwner()

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
        contactId,
        isOwner,
        createdAt,
    )

    if (!isOwner.isTrue()) {
        queries.dashboardInsert(
            contactId,
            createdAt,
        )
    }

    dto.invite?.let {
        upsertInvite(it, queries)
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun TransactionCallbacks.upsertInvite(dto: InviteDto, queries: SphinxDatabaseQueries) {
    queries.inviteUpsert(
        InviteString(dto.invite_string),
        dto.invoice?.toLightningPaymentRequest(),
        dto.status.toInviteStatus(),
        dto.price?.toSat(),
        InviteId(dto.id),
        ContactId(dto.contact_id),
        dto.created_at.toDateTime(),
    )

    // TODO: Work out what status needs to be included to be shown on the dashboard
//        when (it.status.toInviteStatus()) {
//            is InviteStatus.Complete -> TODO()
//            is InviteStatus.Delivered -> TODO()
//            is InviteStatus.Expired -> TODO()
//            is InviteStatus.InProgress -> TODO()
//            is InviteStatus.PaymentPending -> TODO()
//            is InviteStatus.Pending -> TODO()
//            is InviteStatus.Ready -> TODO()
//            is InviteStatus.Unknown -> TODO()
//        }
//        queries.dashboardInsert(
//            InviteId(it.id),
//            DateTime.nowUTC().toDateTime(),
//        )
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
    queries.dashboardDeleteById(chatId)
    latestMessageUpdatedTimeMap?.withLock { it.remove(chatId) }
}

@Suppress("NOTHING_TO_INLINE")
inline fun TransactionCallbacks.deleteContactById(
    contactId: ContactId,
    queries: SphinxDatabaseQueries
) {
    queries.contactDeleteById(contactId)
    queries.inviteDeleteByContactId(contactId)
    queries.dashboardDeleteById(contactId)
}
