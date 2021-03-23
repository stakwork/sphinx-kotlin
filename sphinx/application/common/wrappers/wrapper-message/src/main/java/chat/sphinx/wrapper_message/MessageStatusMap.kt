package chat.sphinx.wrapper_message

import chat.sphinx.wrapper_common.contact.ContactId

@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class)
inline fun Map<Long, Int?>.toMessageStatusMap(): Map<ContactId, MessageStatus> =
    this.mapKeys { ContactId(it.key) }.mapValues { it.value.toMessageStatus() }

@Suppress("NOTHING_TO_INLINE")
inline fun Map<ContactId, MessageStatus>.toPrimitivesMap(): Map<Long, Int?> =
    mapKeys { it.key.value }.mapValues { it.value.value }
