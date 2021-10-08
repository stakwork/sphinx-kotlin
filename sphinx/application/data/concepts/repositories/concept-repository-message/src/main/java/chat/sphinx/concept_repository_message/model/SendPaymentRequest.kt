package chat.sphinx.concept_repository_message.model

import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId

class SendPaymentRequest private constructor(
    val chatId: ChatId?,
    val contactId: ContactId?,
    val memo: String?,
    val amount: Long,
) {
    class Builder {
        private var chatId: ChatId?         = null
        private var contactId: ContactId?   = null
        private var memo: String?           = null
        private var amount: Long            = 0

        @Synchronized
        fun clear() {
            chatId = null
            contactId = null
            memo = null
            amount = 0
        }

        @get:Synchronized
        val isValid: Boolean
            get() = (amount > 0)

        @get:Synchronized
        val isContactRequest: Boolean
            get() = amount > 0 && contactId != null

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
        fun setAmount(amount: Long): Builder {
            this.amount = amount
            return this
        }

        @Synchronized
        fun setMemo(text: String?): Builder {
            if (text == null || text.isEmpty()) {
                this.memo = null
            } else {
                this.memo = text
            }
            return this
        }

        @Synchronized
        fun build(): SendPaymentRequest? =
            if (!isValid) {
                null
            } else {
                SendPaymentRequest(
                    chatId,
                    contactId,
                    memo,
                    amount
                )
            }
    }
}