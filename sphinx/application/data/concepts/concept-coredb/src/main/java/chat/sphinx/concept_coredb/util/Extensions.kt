package chat.sphinx.concept_coredb.util

import chat.sphinx.conceptcoredb.*

@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun SphinxDatabaseQueries.upsertMessage(dbo: MessageDbo): Unit =
    messageUpsert(
        dbo.status,
        dbo.seen,
        dbo.sender_alias,
        dbo.sender_pic,
        dbo.original_muid,
        dbo.reply_uuid,
        dbo.type,
        dbo.id,
        dbo.uuid,
        dbo.chat_id,
        dbo.sender,
        dbo.receiver_,
        dbo.amount,
        dbo.payment_hash,
        dbo.payment_request,
        dbo.date,
        dbo.expiration_date,
        dbo.message_content,
        dbo.message_content_decrypted,
        dbo.muid
    )
