package chat.sphinx.wrapper_common

@Suppress("NOTHING_TO_INLINE")
inline fun Deleted.isTrue(): Boolean =
    this is Deleted.True

@Suppress("NOTHING_TO_INLINE")
inline fun Int.toDeleted(): Deleted =
    when (this) {
        Deleted.DELETED -> {
            Deleted.True
        }
        else -> {
            Deleted.False
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Boolean.toDeleted(): Deleted =
    if (this) Deleted.True else Deleted.False

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
