package chat.sphinx.wrapper_common

@Suppress("NOTHING_TO_INLINE")
inline fun Seen.isTrue(): Boolean =
    this is Seen.True

/**
 * Converts the integer value returned over the wire to an object.
 *
 * @throws [IllegalArgumentException] if the integer is not supported
 * */
@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class)
inline fun Int.toSeen(): Seen =
    when (this) {
        Seen.SEEN -> {
            Seen.True
        }
        Seen.NOT_SEEN -> {
            Seen.False
        }
        else -> {
            throw IllegalArgumentException(
                "Seen for integer '$this' not supported"
            )
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
