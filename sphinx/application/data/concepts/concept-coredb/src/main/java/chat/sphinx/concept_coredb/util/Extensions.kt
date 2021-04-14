package chat.sphinx.concept_coredb.util

import chat.sphinx.conceptcoredb.*

@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun SphinxDatabaseQueries.upsertChat(dbo: ChatDbo): Unit =
    chatUpsert(
        dbo.name,
        dbo.photo_url,
        dbo.status,
        dbo.contact_ids,
        dbo.is_muted,
        dbo.group_key,
        dbo.host,
        dbo.price_per_message,
        dbo.escrow_amount,
        dbo.unlisted,
        dbo.private_tribe,
        dbo.owner_pub_key,
        dbo.seen,
        dbo.meta_data,
        dbo.my_photo_url,
        dbo.my_alias,
        dbo.pending_contact_ids,
        dbo.id,
        dbo.uuid,
        dbo.type,
        dbo.created_at,
    )

@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun SphinxDatabaseQueries.upsertMessage(dbo: MessageDbo): Unit =
    messageUpsert(
        dbo.status,
        dbo.seen,
        dbo.sender_alias,
        dbo.sender_pic,
        dbo.original_muid,
        dbo.reply_uuid,
        dbo.id,
        dbo.uuid,
        dbo.chat_id,
        dbo.type,
        dbo.sender,
        dbo.receiver,
        dbo.amount,
        dbo.payment_hash,
        dbo.payment_request,
        dbo.date,
        dbo.expiration_date,
        dbo.message_content,
        dbo.message_content_decrypted,
    )

@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun SphinxDatabaseQueries.upsertMessageMedia(dbo: MessageMediaDbo): Unit =
    messageMediaUpsert(
        dbo.media_key,
        dbo.media_type,
        dbo.media_token,
        dbo.id,
        dbo.chat_id,
        dbo.media_key_decrypted,
    )
