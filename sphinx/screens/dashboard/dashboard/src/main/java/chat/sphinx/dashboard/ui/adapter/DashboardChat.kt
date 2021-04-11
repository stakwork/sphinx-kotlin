package chat.sphinx.dashboard.ui.adapter

import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.hhmmElseDate
import chat.sphinx.wrapper_common.time
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_message.*

/**
 * [DashboardChat]s are separated into 2 categories:
 *  - [Active]: An active chat
 *  - [Inactive]: A contact without a conversation yet, or an Invite
 * */
sealed class DashboardChat {

    abstract val chatName: String?
    abstract val sortBy: Long
    abstract fun getDisplayTime(today00: DateTime): String

    abstract fun getMessageText(): String

    sealed class Active: DashboardChat() {

        companion object {
            const val YOU = "You"
            const val DECRYPTION_ERROR = "DECRYPTION ERROR..."
        }

        abstract val chat: Chat
        abstract val message: Message?

        override val sortBy: Long
            get() = message?.date?.time ?: chat.createdAt.time

        override fun getDisplayTime(today00: DateTime): String {
            return message?.date?.hhmmElseDate(today00) ?: ""
        }

        fun isMessageSenderSelf(message: Message): Boolean =
            message.sender == chat.contactIds.firstOrNull()

        abstract fun getMessageSender(message: Message, withColon: Boolean = true): String

        override fun getMessageText(): String {
            val message: Message? = message
            return when {
                message == null -> {
                    ""
                }
                message.messageDecryptionError -> {
                    DECRYPTION_ERROR
                }
                message.type.isMessage() -> {
                    message.messageContentDecrypted?.value?.let { decrypted ->

                        if (message.giphyData != null) {
                            "${getMessageSender(message)}GIF shared"
                        } else {
                            "${getMessageSender(message)}$decrypted"
                        }

                    } ?: "${getMessageSender(message)}..."
                }
                message.type.isInvoice() -> {
                    val amount: String = if (message.amount.value > 1) {
                        "${message.amount.value} sats"
                    } else {
                        "${message.amount.value} sat"
                    }

                    if (isMessageSenderSelf(message)) {
                        "Invoice Sent: $amount"
                    } else {
                        "Invoice Received: $amount"
                    }

                }
                message.type.isPayment() || message.type.isDirectPayment() -> {
                    val amount: String = if (message.amount.value > 1) {
                        "${message.amount.value} sats"
                    } else {
                        "${message.amount.value} sat"
                    }

                    if (isMessageSenderSelf(message)) {
                        "Payment Sent: $amount"
                    } else {
                        "Payment Received: $amount"
                    }
                }
                message.type.isAttachment() -> {
                    // TODO: Implement
                    ""
                }
                message.type.isGroupJoin() -> {
                    "${getMessageSender(message, false)} has joined the ${chat.type.javaClass.simpleName}"
                }
                message.type.isGroupLeave() -> {
                    "${getMessageSender(message, false)} has left the ${chat.type.javaClass.simpleName}"
                }
                message.type.isBoost() -> {
                    val amount: Long = message.podBoost?.amount?.value ?: message.amount.value
                    "${getMessageSender(message)}Boost $amount " + if (amount > 1) "sats" else "sat"
                }
                else -> {
                    ""
                }
            }
        }

        class Conversation(
            override val chat: Chat,
            override val message: Message?,
            val contact: Contact,
        ): Active() {

            init {
                require(chat.type.isConversation()) {
                    """
                    DashboardChat.Conversation is strictly for
                    Contacts. Use DashboardChat.GroupOrTribe.
                """.trimIndent()
                }
            }

            override val chatName: String?
                get() = contact.alias?.value

            override fun getMessageSender(message: Message, withColon: Boolean): String {
                if (isMessageSenderSelf(message)) {
                    return YOU + if (withColon) ": " else ""
                }

                return contact.alias?.let { alias ->
                    alias.value + if (withColon) ": " else ""
                } ?: ""
            }
        }

        class GroupOrTribe(
            override val chat: Chat,
            override val message: Message?,
        ): Active() {
            override val chatName: String?
                get() = chat.name?.value

            override fun getMessageSender(message: Message, withColon: Boolean): String {
                if (isMessageSenderSelf(message)) {
                    return YOU + if (withColon) ": " else ""
                }

                return message.senderAlias?.let { alias ->
                    alias.value + if (withColon) ": " else ""
                } ?: ""
            }
        }
    }

    /**
     * Inactive chats are for newly added contacts that are awaiting
     * messages to be sent (the Chat has not been created yet)
     * */
    sealed class Inactive: DashboardChat() {

        override fun getDisplayTime(today00: DateTime): String {
            return ""
        }

        class Conversation(
            val contact: Contact
        ): Inactive() {
            override val chatName: String?
                get() = contact.alias?.value

            override val sortBy: Long
                get() = contact.createdAt.time

            override fun getMessageText(): String {
                return ""
            }
        }
    }
}
