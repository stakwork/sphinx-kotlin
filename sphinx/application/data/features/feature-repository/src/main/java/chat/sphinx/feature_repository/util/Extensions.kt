package chat.sphinx.feature_repository.util

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_query_chat.model.TribeDto
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
import chat.sphinx.wrapper_common.message.toMessageUUID
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

@Suppress("NOTHING_TO_INLINE")
inline fun TransactionCallbacks.updateChatMuted(
    chatId: ChatId,
    muted: ChatMuted,
    queries: SphinxDatabaseQueries
) {
    queries.chatUpdateMuted(muted, chatId)
    queries.dashboardUpdateMuted(muted, chatId)
}

@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun TransactionCallbacks.updateChatTribeData(
    tribe: TribeDto,
    chatId: ChatId,
    queries: SphinxDatabaseQueries,
) {
    val pricePerMessage = tribe.price_per_message.toSat()
    val escrowAmount = tribe.escrow_amount.toSat()
    val name = tribe.name.toChatName()
    val photoUrl = tribe.img?.toPhotoUrl()

    queries.chatUpdateTribeData(
        pricePerMessage,
        escrowAmount,
        name,
        photoUrl,
        chatId,
    )

    queries.dashboardUpdateTribe(
        name?.value ?: "",
        photoUrl,
        chatId
    )
}

@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun TransactionCallbacks.upsertChat(
    dto: ChatDto,
    moshi: Moshi,
    chatSeenMap: SynchronizedMap<ChatId, Seen>,
    queries: SphinxDatabaseQueries,
    contactDto: ContactDto? = null
) {
    val seen = dto.seenActual.toSeen()
    val chatId = ChatId(dto.id)
    val chatType = dto.type.toChatType()
    val createdAt = dto.created_at.toDateTime()
    val contactIds = dto.contact_ids.map { ContactId(it) }
    val muted = dto.isMutedActual.toChatMuted()
    val chatPhotoUrl = dto.photo_url?.toPhotoUrl()
    val pricePerMessage = dto.price_per_message?.toSat()
    val escrowAmount = dto.escrow_amount?.toSat()
    val chatName = dto.name?.toChatName()

    queries.chatUpsert(
        chatName,
        chatPhotoUrl,
        dto.status.toChatStatus(),
        contactIds,
        muted,
        dto.group_key?.toChatGroupKey(),
        dto.host?.toChatHost(),
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
        chatType,
        createdAt,
        pricePerMessage,
        escrowAmount,
    )

    if (chatType.isTribe() && (pricePerMessage != null || escrowAmount != null)) {
        queries.chatUpdateTribeData(pricePerMessage, escrowAmount, chatName, chatPhotoUrl, chatId)
    }

    val conversationContactId: ContactId? = if (chatType.isConversation()) {
        contactIds.elementAtOrNull(1)?.let { contactId ->
            queries.dashboardUpdateIncludeInReturn(false, contactId)
            contactId
        }
    } else {
        null
    }

    queries.dashboardUpsert(
        if (conversationContactId != null && contactDto != null) {
            contactDto.alias
        } else {
            dto.name ?: " "
        },
        muted,
        seen,
        if (conversationContactId != null && contactDto != null) {
            contactDto.photo_url?.toPhotoUrl()
        } else {
            chatPhotoUrl
        },
        chatId,
        conversationContactId,
        createdAt
    )

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
    val photoUrl = dto.photo_url?.toPhotoUrl()

    queries.contactUpsert(
        dto.route_hint?.toLightningRouteHint(),
        dto.public_key?.toLightningNodePubKey(),
        dto.node_alias?.toLightningNodeAlias(),
        dto.alias?.toContactAlias(),
        photoUrl,
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
        queries.dashboardUpsert(
            dto.alias,
            ChatMuted.False,
            Seen.True,
            photoUrl,
            contactId,
            null,
            createdAt,
        )
        queries.dashboardUpdateConversation(
            dto.alias,
            photoUrl,
            contactId
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
