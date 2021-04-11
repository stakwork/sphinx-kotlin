package chat.sphinx.concept_coredb.util

import chat.sphinx.conceptcoredb.ChatDbo
import chat.sphinx.conceptcoredb.ContactDbo
import chat.sphinx.conceptcoredb.MessageDbo
import chat.sphinx.conceptcoredb.SphinxDatabaseQueries

@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun SphinxDatabaseQueries.upsertChat(dbo: ChatDbo): Unit =
    chatUpsert(
        dbo.uuid,
        dbo.name,
        dbo.photo_url,
        dbo.type,
        dbo.status,
        dbo.contact_ids,
        dbo.is_muted,
        dbo.created_at,
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
        dbo.id
    )

@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun SphinxDatabaseQueries.upsertMessage(dbo: MessageDbo): Unit =
    messageUpsert(
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
        dbo.status,
        dbo.media_key,
        dbo.media_type,
        dbo.media_token,
        dbo.seen,
        dbo.sender_alias,
        dbo.sender_pic,
        dbo.original_muid,
        dbo.reply_uuid,
        dbo.id,
        dbo.message_content_decrypted,
        dbo.media_key_decrypted,
    )

@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun SphinxDatabaseQueries.upsertContact(dbo: ContactDbo): Unit =
    contactUpsert(
        dbo.route_hint,
        dbo.node_pub_key,
        dbo.node_alias,
        dbo.alias,
        dbo.photo_url,
        dbo.private_photo,
        dbo.status,
        dbo.public_key,
        dbo.device_id,
        dbo.created_at,
        dbo.updated_at,
        dbo.notification_sound,
        dbo.tip_amount,
        dbo.invite_id,
        dbo.id,
        dbo.owner
    )
