package chat.sphinx.dashboard.ui.adapter

import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_common.time
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_message.Message

/**
 * [DashboardChat]s are separated into 2 categories:
 *  - [Active]: An active chat
 *  - [Inactive]: A contact without a conversation yet, or an Invite
 * */
sealed class DashboardChat {

    abstract val chatName: String?
    abstract val sortBy: Long

    sealed class Active: DashboardChat() {
        abstract val chat: Chat
        abstract val message: Message?

        abstract fun getMessageSender(message: Message): String

        override val sortBy: Long
            get() = message?.date?.time ?: chat.createdAt.time

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

            override fun getMessageSender(message: Message): String {
                if (message.sender == chat.contactIds.firstOrNull()) {
                    return "you: "
                }

                return contact.alias?.let { alias ->
                    "${alias.value}: "
                } ?: ""
            }
        }

        class GroupOrTribe(
            override val chat: Chat,
            override val message: Message?,
        ): Active() {
            override val chatName: String?
                get() = chat.name?.value

            override fun getMessageSender(message: Message): String {
                if (message.sender == chat.contactIds.firstOrNull()) {
                    return "you: "
                }

                return message.senderAlias?.let { alias ->
                    "${alias.value}: "
                } ?: ""
            }
        }
    }

    /**
     * Inactive chats are for newly added contacts that are awaiting
     * messages to be sent (the Chat has not been created yet)
     * */
    sealed class Inactive: DashboardChat() {

        class Conversation(
            val contact: Contact
        ): Inactive() {
            override val chatName: String?
                get() = contact.alias?.value

            override val sortBy: Long
                get() = contact.createdAt.time
        }
    }
}
