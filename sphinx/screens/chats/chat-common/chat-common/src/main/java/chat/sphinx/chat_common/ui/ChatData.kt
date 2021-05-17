package chat.sphinx.chat_common.ui

import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatMuted
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_contact.Contact

sealed class ChatData {

    abstract var chat: Chat?

    abstract val chatName: String?
    abstract val photoUrl: PhotoUrl?
    abstract val muted: ChatMuted

    fun updateChat(chat: Chat?) {
        this.chat = chat
    }

    class Conversation(override var chat: Chat?, val contact: Contact): ChatData() {
        override val chatName: String?
            get() = contact.alias?.value
        override val photoUrl: PhotoUrl?
            get() = contact.photoUrl
        override val muted: ChatMuted
            get() = chat?.isMuted ?: ChatMuted.False
    }

    class Group(override var chat: Chat?): ChatData() {
        override val chatName: String?
            get() = chat?.name?.value
        override val photoUrl: PhotoUrl?
            get() = chat?.photoUrl
        override val muted: ChatMuted
            get() = chat?.isMuted ?: ChatMuted.False
    }

    class Tribe(override var chat: Chat?): ChatData() {
        override val chatName: String?
            get() = chat?.name?.value
        override val photoUrl: PhotoUrl?
            get() = chat?.photoUrl
        override val muted: ChatMuted
            get() = chat?.isMuted ?: ChatMuted.False
    }
}
