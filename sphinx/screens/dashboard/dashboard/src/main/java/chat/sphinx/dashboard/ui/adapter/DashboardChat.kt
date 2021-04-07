package chat.sphinx.dashboard.ui.adapter

import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_message.Message

sealed class DashboardChat {

    abstract val chat: Chat?
    abstract val chatName: String?
    abstract val message: Message?

    class Conversation(
        override val chat: Chat?,
        override val message: Message?,
        val contact: Contact,
    ): DashboardChat() {

        init {
            require(chat?.type?.isConversation() != false) {
                """
                    DashboardChat.Conversation is strictly for
                    Contacts. Use DashboardChat.GroupOrTribe.
                """.trimIndent()
            }
        }

        override val chatName: String?
            get() = contact.alias?.value
    }

    class GroupOrTribe(
        override val chat: Chat,
        override val message: Message?,
    ): DashboardChat() {
        override val chatName: String?
            get() = chat.name?.value
    }

    // TODO: Invite
}