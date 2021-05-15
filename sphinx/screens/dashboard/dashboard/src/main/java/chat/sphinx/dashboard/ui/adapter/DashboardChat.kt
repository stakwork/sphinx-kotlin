package chat.sphinx.dashboard.ui.adapter

import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_common.*
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_message.media.MediaType
import kotlinx.coroutines.flow.Flow

/**
 * [DashboardChat]s are separated into 2 categories:
 *  - [Active]: An active chat
 *  - [Inactive]: A contact without a conversation yet, or an Invite
 * */
sealed class DashboardChat {

    abstract val chatName: String?
    abstract val photoUrl: PhotoUrl?
    abstract val sortBy: Long

    abstract val unseenMessageFlow: Flow<Long?>?

    abstract fun getDisplayTime(today00: DateTime): String

    abstract fun getMessageText(): String

    abstract fun hasUnseenMessages(): Boolean

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

        override fun hasUnseenMessages(): Boolean {
            val ownerId: ContactId? = chat?.contactIds?.firstOrNull()
            val isLastMessageOutgoing = message?.sender == ownerId
            val lastMessageSeen = message?.seen?.isTrue() ?: true
            val chatSeen = chat?.seen.isTrue() ?: true
            return !lastMessageSeen && !chatSeen && !isLastMessageOutgoing
        }

        @ExperimentalStdlibApi
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

                        when {
                            message.giphyData != null -> {
                                "${getMessageSender(message)}GIF shared"
                            }
                            message.podBoost != null -> {
                                val amount: Long = message.podBoost?.amount?.value ?: message.amount.value
                                "${getMessageSender(message)}Boost $amount " + if (amount > 1) "sats" else "sat"
                            }
                            // TODO: check for clip::
                            else -> {
                                "${getMessageSender(message)}$decrypted"
                            }
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
                    message.messageMedia?.let { media ->
                        when (media.mediaType) {
                            is MediaType.Audio -> {
                                "an Audio clip"
                            }
                            is MediaType.Gif -> {
                                "a GIF"
                            }
                            is MediaType.Image -> {
                                "an Image"
                            }
                            is MediaType.Pdf -> {
                                "a PDF"
                            }
                            is MediaType.SphinxText -> {
                                "a Paid Message"
                            }
                            is MediaType.Unknown -> {
                                "an Attachment"
                            }
                            is MediaType.Video -> {
                                "a Video"
                            }
                            else -> {
                                null
                            }
                        }?.let { text ->
                            "${getMessageSender(message, false)} sent $text"
                        }
                    } ?: ""
                }
                message.type.isGroupJoin() -> {
                    "${getMessageSender(message, false)} just joined the ${chat.type.javaClass.simpleName.lowercase()}"
                }
                message.type.isGroupLeave() -> {
                    "${getMessageSender(message, false)} just left the ${chat.type.javaClass.simpleName.lowercase()}"
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
            override val unseenMessageFlow: Flow<Long?>,
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

            override val photoUrl: PhotoUrl?
                get() = chat.photoUrl ?: contact.photoUrl

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
            override val unseenMessageFlow: Flow<Long?>,
        ): Active() {

            override val chatName: String?
                get() = chat.name?.value

            override val photoUrl: PhotoUrl?
                get() = chat.photoUrl

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

            override val photoUrl: PhotoUrl?
                get() = contact.photoUrl

            override val sortBy: Long
                get() = contact.createdAt.time

            override val unseenMessageFlow: Flow<Long?>?
                get() = null

            override fun getMessageText(): String {
                return ""
            }

            override fun hasUnseenMessages(): Boolean {
                return false
            }

        }
    }
}
