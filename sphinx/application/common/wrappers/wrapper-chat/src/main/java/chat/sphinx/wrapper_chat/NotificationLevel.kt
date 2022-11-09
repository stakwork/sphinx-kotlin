package chat.sphinx.wrapper_chat


@Suppress("NOTHING_TO_INLINE")
inline fun NotificationLevel.isSeeAll(): Boolean =
    this is NotificationLevel.SeeAll

@Suppress("NOTHING_TO_INLINE")
inline fun NotificationLevel.isOnlyMentions(): Boolean =
    this is NotificationLevel.OnlyMentions

@Suppress("NOTHING_TO_INLINE")
inline fun NotificationLevel.isMuteChat(): Boolean =
    this is NotificationLevel.MuteChat

@Suppress("NOTHING_TO_INLINE")
inline fun NotificationLevel.isUnknown(): Boolean =
    this is NotificationLevel.Unknown

/**
 * Converts the integer value returned over the wire to an object.
 * */
@Suppress("NOTHING_TO_INLINE")
inline fun Int.toNotificationLevel(): NotificationLevel =
    when (this) {
        NotificationLevel.SEE_ALL -> {
            NotificationLevel.SeeAll
        }
        NotificationLevel.ONLY_MENTIONS -> {
            NotificationLevel.OnlyMentions
        }
        NotificationLevel.MUTE_CHAT -> {
            NotificationLevel.MuteChat
        }
        else -> {
            NotificationLevel.Unknown(this)
        }
    }


/**
 * Comes off the wire as:
 *  - 0 (SEE_ALL)
 *  - 1 (ONLY_MENTIONS)
 *  - 2 (MUTE_CHAT)
 *
 * https://github.com/stakwork/sphinx-relay/blob/7f8fd308101b5c279f6aac070533519160aa4a9f/src/constants.ts#L74
 * */
sealed class NotificationLevel {

    companion object {
        const val SEE_ALL = 0
        const val ONLY_MENTIONS = 1
        const val MUTE_CHAT = 2
    }

    abstract val value: Int

    object SeeAll: NotificationLevel() {
        override val value: Int
            get() = SEE_ALL
    }

    object OnlyMentions: NotificationLevel() {
        override val value: Int
            get() = ONLY_MENTIONS
    }

    object MuteChat: NotificationLevel() {
        override val value: Int
            get() = MUTE_CHAT
    }

    data class Unknown(override val value: Int): NotificationLevel()
}
