package chat.sphinx.wrapper_chat


@Suppress("NOTHING_TO_INLINE")
inline fun ChatType.isConversation(): Boolean =
    this is ChatType.Conversation

@Suppress("NOTHING_TO_INLINE")
inline fun ChatType.isGroup(): Boolean =
    this is ChatType.Group

@Suppress("NOTHING_TO_INLINE")
inline fun ChatType.isTribe(): Boolean =
    this is ChatType.Tribe

@Suppress("NOTHING_TO_INLINE")
inline fun ChatType.isUnknown(): Boolean =
    this is ChatType.Unknown

/**
 * Converts the integer value returned over the wire to an object.
 * */
@Suppress("NOTHING_TO_INLINE")
inline fun Int.toChatType(): ChatType =
    when (this) {
        ChatType.CONVERSATION -> {
            ChatType.Conversation
        }
        ChatType.GROUP -> {
            ChatType.Group
        }
        ChatType.TRIBE -> {
            ChatType.Tribe
        }
        else -> {
            ChatType.Unknown(this)
        }
    }


/**
 * Comes off the wire as:
 *  - 0 (Conversation)
 *  - 1 (Group)
 *  - 2 (Tribe)
 *
 * https://github.com/stakwork/sphinx-relay/blob/7f8fd308101b5c279f6aac070533519160aa4a9f/src/constants.ts#L74
 * */
sealed class ChatType {

    companion object {
        const val CONVERSATION = 0
        const val GROUP = 1
        const val TRIBE = 2
    }

    abstract val value: Int

    object Conversation: ChatType() {
        override val value: Int
            get() = CONVERSATION
    }

    object Group: ChatType() {
        override val value: Int
            get() = GROUP
    }

    object Tribe: ChatType() {
        override val value: Int
            get() = TRIBE
    }

    data class Unknown(override val value: Int): ChatType()
}
