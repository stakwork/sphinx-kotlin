package chat.sphinx.dto_contact.model

@Suppress("NOTHING_TO_INLINE")
inline fun PrivatePhoto.isTrue(): Boolean =
    this is PrivatePhoto.True

/**
 * Comes off the wire as:
 *  - null (Not Private)
 *  - 0 (Not Private)
 *  - 1 (Private)
 * */
sealed class PrivatePhoto {

    companion object {
        private const val PRIVATE = 1
        private const val NOT_PRIVATE = 0

        /**
         * Converts the integer value returned over the wire to an object.
         *
         * @throws [IllegalArgumentException] if the [privatePhoto] integer is not supported
         * */
        fun fromInt(privatePhoto: Int?): PrivatePhoto =
            when (privatePhoto) {
                PRIVATE -> {
                    True
                }
                null, NOT_PRIVATE -> {
                    False
                }
                else -> {
                    throw IllegalArgumentException(
                        "PrivatePhoto for integer '$privatePhoto' not supported"
                    )
                }
            }
    }

    abstract val value: Int

    object True: PrivatePhoto() {
        override val value: Int
            get() = PRIVATE
    }

    object False: PrivatePhoto() {
        override val value: Int
            get() = NOT_PRIVATE
    }
}
