package chat.sphinx.wrapper_common

@Suppress("NOTHING_TO_INLINE")
inline fun Seen.isTrue(): Boolean =
    this is Seen.True

@Suppress("NOTHING_TO_INLINE")
inline fun Int.toSeen(): Seen =
    when (this) {
        Seen.SEEN -> {
            Seen.True
        }
        else -> {
            Seen.False
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Boolean.toSeen(): Seen =
    if (this) Seen.True else Seen.False

/**
 * Comes off the wire as:
 *  - 0 (Not Seen)
 *  - 1 (Seen)
 *  - true (Seen)
 *  - false (Not Seen)
 * */
sealed class Seen {

    companion object {
        const val SEEN = 1
        const val NOT_SEEN = 0
    }

    abstract val value: Int

    object True: Seen() {
        override val value: Int
            get() = SEEN
    }

    object False: Seen() {
        override val value: Int
            get() = NOT_SEEN
    }
}
