package chat.sphinx.concept_repository_message.model

import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_common.payment.PaymentTemplate

class SendPayment private constructor(
    val chatId: ChatId?,
    val contactId: ContactId?,
    val text: String?,
    val destinationKey: LightningNodePubKey?,
    val amount: Long,
    val paymentTemplate: PaymentTemplate?,
    val routeHint: LightningRouteHint?
) {
    class Builder {
        private var chatId: ChatId?                      = null
        private var contactId: ContactId?                = null
        private var text: String?                        = null
        private var destinationKey: LightningNodePubKey? = null
        private var amount: Long                         = 0
        private var paymentTemplate: PaymentTemplate?    = null
        private var routeHint: LightningRouteHint?       = null

        fun clear() {
            chatId = null
            contactId = null
            text = null
            amount = 0
            destinationKey = null
            paymentTemplate = null
            routeHint = null
        }

        @get:Synchronized
        val isValid: Boolean
            get() = (
                        amount > 0
                    )                                                           &&
                    (
                        chatId != null                                          ||
                        contactId != null                                       ||
                        destinationKey != null
                    )

        @get:Synchronized
        val isContactPayment: Boolean
            get() = (
                        amount > 0
                    )                                                           &&
                    (
                        contactId != null
                    )

        @get:Synchronized
        val isKeySendPayment: Boolean
            get() = (
                        amount > 0
                    )                                                           &&
                    (
                        destinationKey != null
                    )

        @get:Synchronized
        val paymentAmount: Long
            get() = amount

        fun setChatId(chatId: ChatId?): Builder {
            this.chatId = chatId
            return this
        }

        fun setContactId(contactId: ContactId?): Builder {
            this.contactId = contactId
            return this
        }

        fun setDestinationKey(destinationKey: LightningNodePubKey?): Builder {
            this.destinationKey = destinationKey
            return this
        }

        fun setRouteHint(routeHint: LightningRouteHint?): Builder {
            this.routeHint = routeHint
            return this
        }

        fun setAmount(amount: Long): Builder {
            this.amount = amount
            return this
        }

        fun setPaymentTemplate(paymentTemplate: PaymentTemplate?): Builder {
            this.paymentTemplate = paymentTemplate
            return this
        }

        fun setText(text: String?): Builder {
            if (text == null || text.isEmpty()) {
                this.text = null
            } else {
                this.text = text
            }
            return this
        }

        fun build(): SendPayment? =
            if (!isValid) {
                null
            } else {
                SendPayment(
                    chatId,
                    contactId,
                    text,
                    destinationKey,
                    amount,
                    paymentTemplate,
                    routeHint
                )
            }
    }
}
