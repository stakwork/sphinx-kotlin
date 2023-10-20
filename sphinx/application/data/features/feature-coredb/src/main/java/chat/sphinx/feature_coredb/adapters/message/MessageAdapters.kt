package chat.sphinx.feature_coredb.adapters.message

import chat.sphinx.wrapper_common.Push
import chat.sphinx.wrapper_common.Seen
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_common.contact.Blocked
import chat.sphinx.wrapper_common.contact.toBlocked
import chat.sphinx.wrapper_common.toPush
import chat.sphinx.wrapper_common.toSeen
import chat.sphinx.wrapper_message.*
import com.squareup.sqldelight.ColumnAdapter

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

internal class FlaggedAdapter private constructor(): ColumnAdapter<Flagged, Long> {

    companion object {
        @Volatile
        private var instance: FlaggedAdapter? = null
        fun getInstance(): FlaggedAdapter =
            instance ?: synchronized(this) {
                instance ?: FlaggedAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: Long): Flagged {
        return databaseValue.toInt().toFlagged()
    }

    override fun encode(value: Flagged): Long {
        return value.value.toLong()
    }
}

internal class RecipientAliasAdapter: ColumnAdapter<RecipientAlias, String> {
    override fun decode(databaseValue: String): RecipientAlias {
        return RecipientAlias(databaseValue)
    }

    override fun encode(value: RecipientAlias): String {
        return value.value
    }
}

internal class PushAdapter: ColumnAdapter<Push, Long> {

    override fun decode(databaseValue: Long): Push {
        return databaseValue.toInt().toPush()
    }

    override fun encode(value: Push): Long {
        return value.value.toLong()
    }
}

internal class PersonAdapter: ColumnAdapter<MessagePerson, String> {

    override fun decode(databaseValue: String): MessagePerson {
        return MessagePerson(databaseValue)
    }

    override fun encode(value: MessagePerson): String {
        return value.value
    }
}

internal class ThreadUUIDAdapter: ColumnAdapter<ThreadUUID, String> {
    override fun decode(databaseValue: String): ThreadUUID {
        return ThreadUUID(databaseValue)
    }

    override fun encode(value: ThreadUUID): String {
        return value.value
    }
}

internal class ErrorMessageAdapter: ColumnAdapter<ErrorMessage, String> {
    override fun decode(databaseValue: String): ErrorMessage {
        return ErrorMessage(databaseValue)
    }

    override fun encode(value: ErrorMessage): String {
        return value.value
    }
}

