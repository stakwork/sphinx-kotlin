package chat.sphinx.feature_coredb.adapters.message

import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_message.*
import com.squareup.sqldelight.ColumnAdapter

internal class MediaKeyAdapter: ColumnAdapter<MediaKey, String> {
    override fun decode(databaseValue: String): MediaKey {
        return MediaKey(databaseValue)
    }

    override fun encode(value: MediaKey): String {
        return value.value
    }
}

internal class MediaTypeAdapter: ColumnAdapter<MediaType, String> {
    override fun decode(databaseValue: String): MediaType {
        return MediaType(databaseValue)
    }

    override fun encode(value: MediaType): String {
        return value.value
    }
}

internal class MediaTokenAdapter: ColumnAdapter<MediaToken, String> {
    override fun decode(databaseValue: String): MediaToken {
        return MediaToken(databaseValue)
    }

    override fun encode(value: MediaToken): String {
        return value.value
    }
}

internal class MessageUUIDAdapter: ColumnAdapter<MessageUUID, String> {
    override fun decode(databaseValue: String): MessageUUID {
        return MessageUUID(databaseValue)
    }

    override fun encode(value: MessageUUID): String {
        return value.value
    }
}

internal class MessageTypeAdapter: ColumnAdapter<MessageType, Long> {
    override fun decode(databaseValue: Long): MessageType {
        return databaseValue.toInt().toMessageType()
    }

    override fun encode(value: MessageType): Long {
        return value.value.toLong()
    }
}

internal class MessageContentAdapter: ColumnAdapter<MessageContent, String> {
    override fun decode(databaseValue: String): MessageContent {
        return MessageContent(databaseValue)
    }

    override fun encode(value: MessageContent): String {
        return value.value
    }
}

internal class MessageContentDecryptedAdapter: ColumnAdapter<MessageContentDecrypted, String> {
    override fun decode(databaseValue: String): MessageContentDecrypted {
        return MessageContentDecrypted(databaseValue)
    }

    override fun encode(value: MessageContentDecrypted): String {
        return value.value
    }
}

internal class MessageStatusAdapter: ColumnAdapter<MessageStatus, Long> {

    companion object {
        private const val NULL = Long.MIN_VALUE
    }

    override fun decode(databaseValue: Long): MessageStatus {
        return if (databaseValue == NULL) {
            MessageStatus.NoStatus
        } else {
            databaseValue.toInt().toMessageStatus()
        }
    }

    override fun encode(value: MessageStatus): Long {
        return value.value?.toLong() ?: NULL
    }
}

/**
 * Stores the map as a string value that looks like:
 *
 * 22||8|--|15||NULL|--|18||12
 *
 * SqlDelight does not like Maps, so we use a List of Pairs for the DBO
 * and use the
 * */
internal class MessageStatusMapAdapter: ColumnAdapter<List<Pair<ContactId, MessageStatus>>, String> {

    companion object {
        private const val DELIMINATOR_MAJOR = "|--|"
        private const val DELIMINATOR_MINOR = "||"
        private const val NULL = "NULL"
        private const val EMPTY = "EMPTY"
    }

    override fun decode(databaseValue: String): List<Pair<ContactId, MessageStatus>> {
        if (databaseValue == EMPTY) {
            return emptyList()
        }

        databaseValue.split(DELIMINATOR_MAJOR).let { kvps ->
            val list: ArrayList<Pair<ContactId, MessageStatus>> = ArrayList(kvps.size)
            kvps.forEach { kvp ->
                kvp.split(DELIMINATOR_MINOR).let { split ->
                    list.add(
                        Pair(
                            ContactId(split[0].toLong()),
                            if (split[1] == NULL) {
                                MessageStatus.NoStatus
                            } else {
                                split[1].toInt().toMessageStatus()
                            }
                        )
                    )
                }
            }
            return list.toList()
        }
    }

    override fun encode(value: List<Pair<ContactId, MessageStatus>>): String {
        if (value.isEmpty()) {
            return EMPTY
        }

        val sb = StringBuilder()
        var count = 1
        for (item in value) {
            sb.append(item.first.value)
            sb.append(DELIMINATOR_MINOR)
            sb.append(item.second.value ?: NULL)
            if (count < value.size) {
                sb.append(DELIMINATOR_MAJOR)
            }
            count++
        }

        return sb.toString()
    }
}

internal class SenderAliasAdapter: ColumnAdapter<SenderAlias, String> {
    override fun decode(databaseValue: String): SenderAlias {
        return SenderAlias(databaseValue)
    }

    override fun encode(value: SenderAlias): String {
        return value.value
    }
}

internal class MessageMUIDAdapter: ColumnAdapter<MessageMUID, String> {
    override fun decode(databaseValue: String): MessageMUID {
        return MessageMUID(databaseValue)
    }

    override fun encode(value: MessageMUID): String {
        return value.value
    }
}

internal class ReplyUUIDAdapter: ColumnAdapter<ReplyUUID, String> {
    override fun decode(databaseValue: String): ReplyUUID {
        return ReplyUUID(databaseValue)
    }

    override fun encode(value: ReplyUUID): String {
        return value.value
    }
}
