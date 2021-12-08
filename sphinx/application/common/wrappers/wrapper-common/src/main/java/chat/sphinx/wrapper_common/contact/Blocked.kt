package chat.sphinx.wrapper_common.contact

@Suppress("NOTHING_TO_INLINE")
inline fun Blocked.isTrue(): Boolean =
    this is Blocked.True

@Suppress("NOTHING_TO_INLINE")
inline fun Int.toBlocked(): Blocked =
    when (this) {
        Blocked.BLOCKED -> {
            Blocked.True
        }
        else -> {
            Blocked.False
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Boolean.toBlocked(): Blocked =
    if (this) Blocked.True else Blocked.False

sealed class Blocked {

    companion object {
        const val BLOCKED = 1
        const val NOT_BLOCKED = 0
    }

    abstract val value: Int

    object True: Blocked() {
        override val value: Int
            get() = BLOCKED
    }

    object False: Blocked() {
        override val value: Int
            get() = NOT_BLOCKED
    }
}
