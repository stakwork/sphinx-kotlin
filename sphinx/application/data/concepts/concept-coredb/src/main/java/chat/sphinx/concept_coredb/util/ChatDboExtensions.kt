@file:Suppress("unused")

package chat.sphinx.concept_coredb.util

import chat.sphinx.conceptcoredb.ChatDbo
import chat.sphinx.conceptcoredb.SphinxDatabaseQueries

@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun SphinxDatabaseQueries.upsertChat(dbo: ChatDbo): Unit =
    upsertChat(
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