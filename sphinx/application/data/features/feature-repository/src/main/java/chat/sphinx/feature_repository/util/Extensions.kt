package chat.sphinx.feature_repository.util

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_query_chat.model.podcast.PodcastDto
import chat.sphinx.concept_network_query_chat.model.TribeDto
import chat.sphinx.concept_network_query_chat.model.feed.FeedDto
import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.concept_network_query_invite.model.InviteDto
import chat.sphinx.concept_network_query_lightning.model.balance.BalanceDto
import chat.sphinx.concept_network_query_message.model.MessageDto
import chat.sphinx.concept_network_query_subscription.model.SubscriptionDto
import chat.sphinx.conceptcoredb.SphinxDatabaseQueries
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.*
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.contact.toBlocked
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.dashboard.InviteId
import chat.sphinx.wrapper_common.feed.*
import chat.sphinx.wrapper_common.invite.InviteStatus
import chat.sphinx.wrapper_common.invite.isPaymentPending
import chat.sphinx.wrapper_common.invite.isProcessingPayment
import chat.sphinx.wrapper_common.invite.toInviteStatus
import chat.sphinx.wrapper_common.lightning.*
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.message.toMessageUUID
import chat.sphinx.wrapper_common.subscription.Cron
import chat.sphinx.wrapper_common.subscription.EndNumber
import chat.sphinx.wrapper_common.subscription.SubscriptionCount
import chat.sphinx.wrapper_common.subscription.SubscriptionId
import chat.sphinx.wrapper_contact.*
import chat.sphinx.wrapper_feed.*
import chat.sphinx.wrapper_invite.InviteString
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_message_media.*
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

@Suppress("NOTHING_TO_INLINE")
inline fun TransactionCallbacks.updateChatNotificationLevel(
    chatId: ChatId,
    notificationLevel: NotificationLevel?,
    queries: SphinxDatabaseQueries
) {
    queries.chatUpdateNotificationLevel(notificationLevel, chatId)
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
    contactDto: ContactDto? = null,
    ownerPubKey: LightningNodePubKey? = null
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
    val adminPubKey = dto.owner_pub_key?.toLightningNodePubKey()

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
        dto.notify?.toNotificationLevel(),
        chatId,
        ChatUUID(dto.uuid),
        chatType,
        createdAt,
        pricePerMessage,
        escrowAmount,
    )

    if (chatType.isTribe() && (ownerPubKey == adminPubKey) && (pricePerMessage != null || escrowAmount != null)) {
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
        dto.blockedActual.toBlocked(),
        contactId,
        isOwner,
        createdAt,
    )

    dto.invite?.let { inviteDto ->
        upsertInvite(inviteDto, queries)
    }

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
}

@Suppress("NOTHING_TO_INLINE")
inline fun TransactionCallbacks.upsertInvite(dto: InviteDto, queries: SphinxDatabaseQueries) {

    val inviteStatus = dto.status.toInviteStatus().let { dtoStatus ->
        if (dtoStatus.isPaymentPending()) {

            val invite = queries.inviteGetById(InviteId(dto.id)).executeAsOneOrNull()

            if (invite?.status?.isProcessingPayment() == true) {
                InviteStatus.ProcessingPayment
            } else {
                dtoStatus
            }

        } else {
            dtoStatus
        }
    }

    queries.inviteUpsert(
        InviteString(dto.invite_string),
        dto.invoice?.toLightningPaymentRequestOrNull(),
        inviteStatus,
        dto.price?.toSat(),
        InviteId(dto.id),
        ContactId(dto.contact_id),
        dto.created_at.toDateTime(),
    )

    queries.contactUpdateInvite(
        inviteStatus,
        InviteId(dto.id),
        ContactId(dto.contact_id)
    )

// TODO: Work out what status needs to be included to be shown on the dashboard

//        when (inviteStatus) {
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

@Suppress("NOTHING_TO_INLINE")
inline fun TransactionCallbacks.updateInviteStatus(
    inviteId: InviteId,
    inviteStatus: InviteStatus,
    queries: SphinxDatabaseQueries,
) {
    queries.inviteUpdateStatus(inviteStatus, inviteId)
}

@Suppress("SpellCheckingInspection")
fun TransactionCallbacks.upsertMessage(
    dto: MessageDto,
    queries: SphinxDatabaseQueries,
    fileName: FileName? = null
) {

    val chatId: ChatId = dto.chat_id?.let {
        ChatId(it)
    } ?: dto.chat?.id?.let {
        ChatId(it)
    } ?: ChatId(ChatId.NULL_CHAT_ID.toLong())

    dto.media_token?.let { mediaToken ->

        if (mediaToken.isEmpty()) return

        queries.messageMediaUpsert(
            (dto.media_key ?: "").toMediaKey(),
            (dto.media_type ?: "").toMediaType(),
            MediaToken(mediaToken),
            MessageId(dto.id),
            chatId,
            dto.mediaKeyDecrypted?.toMediaKeyDecrypted(),
            dto.mediaLocalFile,
            fileName
        )
    }

    queries.messageUpsert(
        dto.status.toMessageStatus(),
        dto.seenActual.toSeen(),
        dto.sender_alias?.toSenderAlias(),
        dto.sender_pic?.toPhotoUrl(),
        dto.original_muid?.toMessageMUID(),
        dto.reply_uuid?.toReplyUUID(),
        dto.type.toMessageType(),
        dto.recipient_alias?.toRecipientAlias(),
        dto.recipient_pic?.toPhotoUrl(),
        dto.pushActual.toPush(),
        dto.person?.toMessagePerson(),
        MessageId(dto.id),
        dto.uuid?.toMessageUUID(),
        chatId,
        ContactId(dto.sender),
        dto.receiver?.let { ContactId(it) },
        Sat(dto.amount),
        dto.payment_hash?.toLightningPaymentHash(),
        dto.payment_request?.toLightningPaymentRequestOrNull(),
        dto.date.toDateTime(),
        dto.expiration_date?.toDateTime(),
        dto.message_content?.toMessageContent(),
        dto.messageContentDecrypted?.toMessageContentDecrypted(),
        dto.media_token?.toMediaToken()?.getMUIDFromMediaToken()?.value?.toMessageMUID(),
        false.toFlagged()
    )

    if (dto.type.toMessageType()?.isInvoicePayment()) {
        dto.payment_hash?.toLightningPaymentHash()?.let {
            queries.messageUpdateInvoiceAsPaidByPaymentHash(it)
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
    deleteFeedsByChatId(chatId, queries)
}

@Suppress("NOTHING_TO_INLINE")
inline fun TransactionCallbacks.deleteContactById(
    contactId: ContactId,
    queries: SphinxDatabaseQueries
) {
    queries.contactDeleteById(contactId)
    queries.inviteDeleteByContactId(contactId)
    queries.dashboardDeleteById(contactId)
    queries.subscriptionDeleteByContactId(contactId)
}

@Suppress("NOTHING_TO_INLINE")
inline fun TransactionCallbacks.deleteMessageById(
    messageId: MessageId,
    queries: SphinxDatabaseQueries
) {
    queries.messageDeleteById(messageId)
    queries.messageMediaDeleteById(messageId)
}

@Suppress("NOTHING_TO_INLINE")
inline fun TransactionCallbacks.upsertSubscription(subscriptionDto: SubscriptionDto, queries: SphinxDatabaseQueries) {
    queries.subscriptionUpsert(
        id = SubscriptionId(subscriptionDto.id),
        amount = Sat(subscriptionDto.amount),
        contact_id = ContactId(subscriptionDto.contact_id),
        chat_id = ChatId(subscriptionDto.chat_id),
        count = SubscriptionCount(subscriptionDto.count.toLong()),
        cron = Cron(subscriptionDto.cron),
        end_date = subscriptionDto.end_date?.toDateTime(),
        end_number = subscriptionDto.end_number?.let { EndNumber(it.toLong()) },
        created_at = subscriptionDto.created_at.toDateTime(),
        updated_at = subscriptionDto.updated_at.toDateTime(),
        ended = subscriptionDto.endedActual,
        paused = subscriptionDto.pausedActual,
    )
}

@Suppress("NOTHING_TO_INLINE")
inline fun TransactionCallbacks.deleteSubscriptionById(
    subscriptionId: SubscriptionId,
    queries: SphinxDatabaseQueries
) {
    queries.subscriptionDeleteById(subscriptionId)
}

fun TransactionCallbacks.upsertFeed(
    feedDto: FeedDto,
    feedUrl: FeedUrl,
    searchResultDescription: FeedDescription? = null,
    searchResultImageUrl: PhotoUrl? = null,
    chatId: ChatId,
    currentItemId: FeedId?,
    subscribed: Subscribed,
    queries: SphinxDatabaseQueries
) {

    if (feedDto.items.count() == 0) {
        return
    }

    var cItemId: FeedId? = null

    if (chatId.value != ChatId.NULL_CHAT_ID.toLong()) {
        queries.feedGetAllByChatId(chatId).executeAsList()?.forEach { feedDbo ->
            //Deleting old feed associated with chat
            if (feedDbo.feed_url.value != feedUrl.value) {
                deleteFeedById(
                    feedDbo.id,
                    queries
                )
            } else {
                //Using existing current item id on update if param is null
                cItemId = currentItemId ?: feedDbo.current_item_id
            }
        }
    }

    val feedId = FeedId(feedDto.id)

    feedDto.value?.let { feedValueDto ->
        queries.feedModelUpsert(
            type = FeedModelType(feedValueDto.model.type),
            suggested = FeedModelSuggested(feedValueDto.model.suggested),
            id = feedId
        )
    }

    val itemIds: MutableList<FeedId> = mutableListOf()

    for (item in feedDto.items) {
        val itemId = FeedId(item.id)

        itemIds.add(itemId)

        queries.feedItemUpsert(
            title = FeedTitle(item.title),
            description = item.description?.toFeedDescription(),
            date_published = item.datePublished?.secondsToDateTime(),
            date_updated = item.dateUpdated?.secondsToDateTime(),
            author = item.author?.toFeedAuthor(),
            content_type = item.contentType?.toFeedContentType(),
            enclosure_length = item.enclosureLength?.toFeedEnclosureLength(),
            enclosure_url = FeedUrl(item.enclosureUrl),
            enclosure_type = item.enclosureType?.toFeedEnclosureType(),
            image_url = item.imageUrl?.toPhotoUrl(),
            thumbnail_url = item.thumbnailUrl?.toPhotoUrl(),
            link = item.link?.toFeedUrl(),
            feed_id = feedId,
            id = itemId,
            duration = item.duration?.toFeedItemDuration(),
        )
    }

    queries.feedItemsDeleteOldByFeedId(feedId, itemIds)

    for (destination in feedDto.value?.destinations ?: listOf()) {
        queries.feedDestinationUpsert(
            address = FeedDestinationAddress(destination.address),
            split = FeedDestinationSplit(destination.split.toDouble()),
            type = FeedDestinationType(destination.type),
            feed_id = feedId
        )
    }

    val description = searchResultDescription
        ?: if (feedDto.description?.toFeedDescription() != null) {
            feedDto.description?.toFeedDescription()
        } else {
            null
        }

    val imageUrl = searchResultImageUrl
        ?: if (feedDto.imageUrl?.toPhotoUrl() != null) {
            feedDto.imageUrl?.toPhotoUrl()
        } else {
            null
        }

    queries.feedUpsert(
        feed_type = feedDto.feedType.toInt().toFeedType(),
        title = FeedTitle(feedDto.title),
        description = description,
        feed_url = feedUrl,
        author = feedDto.author?.toFeedAuthor(),
        image_url = imageUrl,
        owner_url = feedDto.ownerUrl?.toFeedUrl(),
        link = feedDto.link?.toFeedUrl(),
        date_published = feedDto.datePublished?.secondsToDateTime(),
        date_updated = feedDto.dateUpdated?.secondsToDateTime(),
        content_type = feedDto.contentType?.toFeedContentType(),
        language = feedDto.language?.toFeedLanguage(),
        items_count = FeedItemsCount(feedDto.items.count().toLong()),
        current_item_id = cItemId,
        chat_id = chatId,
        subscribed = subscribed,
        id = feedId,
        generator = feedDto.generator?.toFeedGenerator(),
    )
}


fun TransactionCallbacks.deleteFeedsByChatId(
    chatId: ChatId,
    queries: SphinxDatabaseQueries
) {
    queries.feedGetAllByChatId(chatId).executeAsList()?.forEach { feedDbo ->
        queries.feedItemsDeleteByFeedId(feedDbo.id)
        queries.feedModelDeleteById(feedDbo.id)
        queries.feedDestinationDeleteByFeedId(feedDbo.id)
        queries.feedDeleteById(feedDbo.id)
    }
}

fun TransactionCallbacks.deleteFeedById(
    feedId: FeedId,
    queries: SphinxDatabaseQueries
) {
    queries.feedGetById(feedId).executeAsOneOrNull()?.let { feedDbo ->
        queries.feedItemsDeleteByFeedId(feedDbo.id)
        queries.feedModelDeleteById(feedDbo.id)
        queries.feedDestinationDeleteByFeedId(feedDbo.id)
        queries.feedDeleteById(feedDbo.id)
    }
}
