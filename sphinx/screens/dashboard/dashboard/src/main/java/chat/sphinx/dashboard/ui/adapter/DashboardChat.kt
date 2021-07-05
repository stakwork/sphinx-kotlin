package chat.sphinx.dashboard.ui.adapter

import chat.sphinx.dashboard.R
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_common.*
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.invite.InviteStatus
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_invite.Invite as InviteWrapper
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_message_media.MediaType
import kotlinx.coroutines.flow.Flow
import java.util.*

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

    abstract fun isEncrypted(): Boolean

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
            val ownerId: ContactId? = chat.contactIds.firstOrNull()
            val isLastMessageOutgoing = message?.sender == ownerId
            val lastMessageSeen = message?.seen?.isTrue() ?: true
            val chatSeen = chat.seen.isTrue()
            return !lastMessageSeen && !chatSeen && !isLastMessageOutgoing
        }

        override fun isEncrypted(): Boolean {
            return true
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
                        when (val type = media.mediaType) {
                            is MediaType.Audio -> {
                                "an Audio clip"
                            }
                            is MediaType.Image -> {
                                if (type.isGif) {
                                    "a GIF"
                                } else {
                                    "an Image"
                                }
                            }
                            is MediaType.Pdf -> {
                                "a PDF"
                            }
                            is MediaType.Text -> {
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

            override fun isEncrypted(): Boolean {
                return !(contact.rsaPublicKey?.value?.isEmpty() ?: true)
            }

        }

        class Invite(
            val contact: Contact,
            val invite: InviteWrapper?
        ): Inactive() {

            override val chatName: String?
                get() =  "Invite: ${contact.alias?.value}"

            override val photoUrl: PhotoUrl?
                get() = contact.photoUrl

            //Invites should always appear at the top of the dashboard list
            override val sortBy: Long
                get() = Date().time

            override val unseenMessageFlow: Flow<Long?>?
                get() = null

            override fun getMessageText(): String {
                when (invite?.status) {
                    is InviteStatus.Pending -> {
                        return "Looking for an available node for $chatName."
                    }
                    is InviteStatus.Ready, InviteStatus.Delivered -> {
                        return "Ready! Tap to share. Expires in 24 hrs."
                    }
                    is InviteStatus.InProgress -> {
                        return "$chatName is signing on."
                    }
                    is InviteStatus.Complete -> {
                        return "Signup complete."
                    }
                    is InviteStatus.PaymentPending -> {
                        return "Tap to pay and activate the invite."
                    }
                    is InviteStatus.ProcessingPayment -> {
                        return "Payment sent. Waiting confirmation."
                    }
                    is InviteStatus.Expired -> {
                        return "Expired"
                    }
                    is InviteStatus.Unknown -> {
                        return ""
                    }
                }
                return ""
            }

            fun getInviteIconAndColor(): Pair<Int, Int>? {
                when (invite?.status) {
                    is InviteStatus.Pending -> {
                        return Pair(R.string.material_icon_name_invite_pending, R.color.sphinxOrange)
                    }
                    is InviteStatus.Ready, InviteStatus.Delivered -> {
                        return Pair(R.string.material_icon_name_invite_ready, R.color.primaryGreen)
                    }
                    is InviteStatus.InProgress -> {
                        return Pair(R.string.material_icon_name_invite_in_progress, R.color.primaryBlue)
                    }
                    is InviteStatus.Complete -> {
                        return Pair(R.string.material_icon_name_invite_complete, R.color.primaryGreen)
                    }
                    is InviteStatus.PaymentPending -> {
                        return Pair(R.string.material_icon_name_invite_payment_pending, R.color.secondaryText)
                    }
                    is InviteStatus.ProcessingPayment -> {
                        return Pair(R.string.material_icon_name_invite_payment_sent, R.color.secondaryText)
                    }
                    is InviteStatus.Expired -> {
                        return Pair(R.string.material_icon_name_invite_expired, R.color.primaryRed)
                    }
                    is InviteStatus.Unknown -> {
                        return null
                    }
                }
                return null
            }

            fun getInvitePrice(): Sat? {
                return invite?.price
            }

            override fun hasUnseenMessages(): Boolean {
                return false
            }

            override fun isEncrypted(): Boolean {
                return false
            }
        }
    }
}
