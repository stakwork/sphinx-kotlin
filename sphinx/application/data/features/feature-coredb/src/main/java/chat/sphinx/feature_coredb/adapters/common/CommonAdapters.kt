package chat.sphinx.feature_coredb.adapters.common

import chat.sphinx.wrapper_common.*
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.invite.InviteId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningPaymentHash
import chat.sphinx.wrapper_common.lightning.LightningPaymentRequest
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.MessageId
import com.squareup.sqldelight.ColumnAdapter

internal class ChatIdAdapter private constructor(): ColumnAdapter<ChatId, Long> {

    companion object {
        @Volatile
        private var instance: ChatIdAdapter? = null
        fun getInstance(): ChatIdAdapter =
            instance ?: synchronized(this) {
                instance ?: ChatIdAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: Long): ChatId {
        return ChatId(databaseValue)
    }

    override fun encode(value: ChatId): Long {
        return value.value
    }
}

internal class ContactIdAdapter private constructor(): ColumnAdapter<ContactId, Long> {

    companion object {
        @Volatile
        private var instance: ContactIdAdapter? = null
        fun getInstance(): ContactIdAdapter =
            instance ?: synchronized(this) {
                instance ?: ContactIdAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: Long): ContactId {
        return ContactId(databaseValue)
    }

    override fun encode(value: ContactId): Long {
        return value.value
    }
}

internal class ContactIdsAdapter private constructor(): ColumnAdapter<List<ContactId>, String> {

    companion object {
        @Volatile
        private var instance: ContactIdsAdapter? = null
        fun getInstance(): ContactIdsAdapter =
            instance ?: synchronized(this) {
                instance ?: ContactIdsAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: String): List<ContactId> {
        if (databaseValue.isEmpty()) {
            return listOf()
        }

        return databaseValue.split(",").map { ContactId(it.toLong()) }
    }

    override fun encode(value: List<ContactId>): String {
        return value.joinToString(",") { it.value.toString() }
    }
}

internal class DateTimeAdapter private constructor(): ColumnAdapter<DateTime, Long> {

    companion object {
        @Volatile
        private var instance: DateTimeAdapter? = null
        fun getInstance(): DateTimeAdapter =
            instance ?: synchronized(this) {
                instance ?: DateTimeAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: Long): DateTime {
        return databaseValue.toDateTime()
    }

    override fun encode(value: DateTime): Long {
        return value.time
    }
}

internal class MessageIdAdapter private constructor(): ColumnAdapter<MessageId, Long> {

    companion object {
        @Volatile
        private var instance: MessageIdAdapter? = null
        fun getInstance(): MessageIdAdapter =
            instance ?: synchronized(this) {
                instance ?: MessageIdAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: Long): MessageId {
        return MessageId(databaseValue)
    }

    override fun encode(value: MessageId): Long {
        return value.value
    }
}

internal class PhotoUrlAdapter private constructor(): ColumnAdapter<PhotoUrl, String> {

    companion object {
        @Volatile
        private var instance: PhotoUrlAdapter? = null
        fun getInstance(): PhotoUrlAdapter =
            instance ?: synchronized(this) {
                instance ?: PhotoUrlAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: String): PhotoUrl {
        return PhotoUrl(databaseValue)
    }

    override fun encode(value: PhotoUrl): String {
        return value.value
    }
}

internal class InviteIdAdapter private constructor(): ColumnAdapter<InviteId, Long> {

    companion object {
        @Volatile
        private var instance: InviteIdAdapter? = null
        fun getInstance(): InviteIdAdapter =
            instance ?: synchronized(this) {
                instance ?: InviteIdAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: Long): InviteId {
        return InviteId(databaseValue)
    }

    override fun encode(value: InviteId): Long {
        return value.value
    }
}

internal class LightningNodePubKeyAdapter private constructor(): ColumnAdapter<LightningNodePubKey, String> {

    companion object {
        @Volatile
        private var instance: LightningNodePubKeyAdapter? = null
        fun getInstance(): LightningNodePubKeyAdapter =
            instance ?: synchronized(this) {
                instance ?: LightningNodePubKeyAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: String): LightningNodePubKey {
        return LightningNodePubKey(databaseValue)
    }

    override fun encode(value: LightningNodePubKey): String {
        return value.value
    }
}

internal class LightningPaymentHashAdapter private constructor(): ColumnAdapter<LightningPaymentHash, String> {

    companion object {
        @Volatile
        private var instance: LightningPaymentHashAdapter? = null
        fun getInstance(): LightningPaymentHashAdapter =
            instance ?: synchronized(this) {
                instance ?: LightningPaymentHashAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: String): LightningPaymentHash {
        return LightningPaymentHash(databaseValue)
    }

    override fun encode(value: LightningPaymentHash): String {
        return value.value
    }
}

internal class LightningPaymentRequestAdapter private constructor(): ColumnAdapter<LightningPaymentRequest, String> {

    companion object {
        @Volatile
        private var instance: LightningPaymentRequestAdapter? = null
        fun getInstance(): LightningPaymentRequestAdapter =
            instance ?: synchronized(this) {
                instance ?: LightningPaymentRequestAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: String): LightningPaymentRequest {
        return LightningPaymentRequest(databaseValue)
    }

    override fun encode(value: LightningPaymentRequest): String {
        return value.value
    }
}

internal class SatAdapter private constructor(): ColumnAdapter<Sat, Long> {

    companion object {
        @Volatile
        private var instance: SatAdapter? = null
        fun getInstance(): SatAdapter =
            instance ?: synchronized(this) {
                instance ?: SatAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: Long): Sat {
        return Sat(databaseValue)
    }

    override fun encode(value: Sat): Long {
        return value.value
    }
}

internal class SeenAdapter private constructor(): ColumnAdapter<Seen, Long> {

    companion object {
        @Volatile
        private var instance: SeenAdapter? = null
        fun getInstance(): SeenAdapter =
            instance ?: synchronized(this) {
                instance ?: SeenAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: Long): Seen {
        return databaseValue.toInt().toSeen()
    }

    override fun encode(value: Seen): Long {
        return value.value.toLong()
    }
}
