package chat.sphinx.concept_repository_message

import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_message.ReplyUUID
import java.io.File

class SendPayment private constructor(
    val chatId: ChatId?,
    val contactId: ContactId?,
    val text: String?,
    val destinationKey: LightningNodePubKey?,
    val amount: Long?,
) {

    class Builder {
        private var chatId: ChatId?         = null
        private var contactId: ContactId?   = null
        private var text: String?           = null
        private var destinationKey: LightningNodePubKey? = null
        private var amount: Long            = 0

        @Synchronized
        fun clear() {
            chatId = null
            contactId = null
            text = null
            amount = 0
            destinationKey = null
        }

        @get:Synchronized
        val isValid: Boolean
            get() = (
                        amount > 0
                    )                                                           &&
                    (
                        chatId != null                                          ||
                        contactId != null
                    )

        @Synchronized
        fun setChatId(chatId: ChatId?): Builder {
            this.chatId = chatId
            return this
        }

        @Synchronized
        fun setContactId(contactId: ContactId?): Builder {
            this.contactId = contactId
            return this
        }

        @Synchronized
        fun setDestinationKey(destinationKey: LightningNodePubKey?): Builder {
            this.destinationKey = destinationKey
            return this
        }

        @Synchronized
        fun setAmount(amount: Long): Builder {
            this.amount = amount
            return this
        }

        @Synchronized
        fun setText(text: String?): Builder {
            if (text == null || text.isEmpty()) {
                this.text = null
            } else {
                this.text = text
            }
            return this
        }

        @Synchronized
        fun build(): SendPayment? =
            if (!isValid) {
                null
            } else {
                SendPayment(
                    chatId,
                    contactId,
                    text,
                    destinationKey,
                    amount
                )
            }
    }
}
