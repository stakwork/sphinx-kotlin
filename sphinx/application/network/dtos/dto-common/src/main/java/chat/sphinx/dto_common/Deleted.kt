package chat.sphinx.dto_common

@Suppress("NOTHING_TO_INLINE")
inline fun Deleted.isTrue(): Boolean =
    this is Deleted.True

/**
 * Converts the integer value returned over the wire to an object.
 *
 * @throws [IllegalArgumentException] if the integer is not supported
 * */
@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class)
inline fun Int.toDeleted(): Deleted =
    when (this) {
        Deleted.DELETED -> {
            Deleted.True
        }
        Deleted.NOT_DELETED -> {
            Deleted.False
        }
        else -> {
            throw IllegalArgumentException(
                "Deleted for integer '$this' not supported"
            )
        }
    }

/**
 * Comes off the wire as:
 *  - 0 (Not Deleted)
 *  - 1 (Deleted)
 * */
sealed class Deleted {

    companion object {
        const val DELETED = 1
        const val NOT_DELETED = 0
    }

    abstract val value: Int

    object True: Deleted() {
        override val value: Int
            get() = DELETED
    }

    object False: Deleted(){
        override val value: Int
            get() = NOT_DELETED
    }
}
