package chat.sphinx.chat_common.ui

import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatMuted
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_contact.Contact

sealed class ChatData {

    abstract val chat: Chat?

    abstract val chatName: String?
    abstract val photoUrl: PhotoUrl?
    abstract val muted: ChatMuted

    class Conversation(override val chat: Chat?, val contact: Contact): ChatData() {
        override val chatName: String?
            get() = contact.alias?.value
        override val photoUrl: PhotoUrl?
            get() = contact.photoUrl
        override val muted: ChatMuted
            get() = chat?.isMuted ?: ChatMuted.False
    }

    class Group(override val chat: Chat): ChatData() {
        override val chatName: String?
            get() = chat.name?.value
        override val photoUrl: PhotoUrl?
            get() = chat.photoUrl
        override val muted: ChatMuted
            get() = chat.isMuted
    }

    class Tribe(override val chat: Chat): ChatData() {
        override val chatName: String?
            get() = chat.name?.value
        override val photoUrl: PhotoUrl?
            get() = chat.photoUrl
        override val muted: ChatMuted
            get() = chat.isMuted
    }
}
