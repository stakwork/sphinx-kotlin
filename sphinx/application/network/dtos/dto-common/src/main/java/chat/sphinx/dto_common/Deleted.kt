package chat.sphinx.dto_common

@Suppress("NOTHING_TO_INLINE")
inline fun Deleted.isTrue(): Boolean =
    this is Deleted.True

/**
 * Comes off the wire as:
 *  - 0 (Not Deleted)
 *  - 1 (Deleted)
 * */
sealed class Deleted {

    companion object {
        private const val DELETED = 1
        private const val NOT_DELETED = 0

        /**
         * Converts the integer value returned over the wire to an object.
         *
         * @throws [IllegalArgumentException] if the [deleted] integer is not supported
         * */
        fun fromInt(deleted: Int): Deleted =
            when (deleted) {
                DELETED -> {
                    True
                }
                NOT_DELETED -> {
                    False
                }
                else -> {
                    throw IllegalArgumentException(
                        "Deleted for integer '$deleted' not supported"
                    )
                }
            }
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
