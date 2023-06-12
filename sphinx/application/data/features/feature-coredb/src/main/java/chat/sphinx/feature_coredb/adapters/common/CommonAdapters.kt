package chat.sphinx.feature_coredb.adapters.common

import chat.sphinx.wrapper_common.*
import chat.sphinx.wrapper_common.dashboard.*
import chat.sphinx.wrapper_common.invite.InviteStatus
import chat.sphinx.wrapper_common.invite.toInviteStatus
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningPaymentHash
import chat.sphinx.wrapper_common.lightning.LightningPaymentRequest
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_common.message.toMessageUUID
import chat.sphinx.wrapper_common.message.toPinnedMessageUUID
import chat.sphinx.wrapper_common.subscription.SubscriptionId
import com.squareup.sqldelight.ColumnAdapter
import java.io.File

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

internal class PinMessageAdapter: ColumnAdapter<MessageUUID, String> {

    companion object {
        @Volatile
        private var instance: PinMessageAdapter? = null
        fun getInstance(): PinMessageAdapter =
            instance ?: synchronized(this) {
                instance ?: PinMessageAdapter()
                    .also { instance = it }
            }
    }
    override fun decode(databaseValue: String): MessageUUID {
        return databaseValue.toPinnedMessageUUID()
    }

    override fun encode(value: MessageUUID): String {
        return value.value
    }

}

internal class DashboardIdAdapter: ColumnAdapter<DashboardItemId, String> {
    override fun decode(databaseValue: String): DashboardItemId {
        return databaseValue.toDashboardItemId()
    }

    override fun encode(value: DashboardItemId): String {
        return value.toDashboardIdString()
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

internal class FileAdapter private constructor(): ColumnAdapter<File, String> {

    companion object {
        @Volatile
        private var instance: FileAdapter? = null
        fun getInstance(): FileAdapter =
            instance ?: synchronized(this) {
                instance ?: FileAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: String): File {
        return File(databaseValue)
    }

    override fun encode(value: File): String {
        return value.absolutePath
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

internal class InviteStatusAdapter private constructor(): ColumnAdapter<InviteStatus, Long> {

    companion object {
        @Volatile
        private var instance: InviteStatusAdapter? = null
        fun getInstance(): InviteStatusAdapter =
            instance ?: synchronized(this) {
                instance ?: InviteStatusAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: Long): InviteStatus {
        return databaseValue.toInt().toInviteStatus()
    }

    override fun encode(value: InviteStatus): Long {
        return value.value.toLong()
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
}internal class MessageIdAdapter private constructor(): ColumnAdapter<MessageId, Long> {

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

internal class SubscriptionIdAdapter private constructor(): ColumnAdapter<SubscriptionId, Long> {

    companion object {
        @Volatile
        private var instance: SubscriptionIdAdapter? = null
        fun getInstance(): SubscriptionIdAdapter =
            instance ?: synchronized(this) {
                instance ?: SubscriptionIdAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: Long): SubscriptionId {
        return SubscriptionId(databaseValue)
    }

    override fun encode(value: SubscriptionId): Long {
        return value.value
    }
}
