package chat.sphinx.dto_contact.model

import java.lang.IllegalArgumentException

@Suppress("NOTHING_TO_INLINE")
inline fun Owner.isTrue(): Boolean =
    this is Owner.True

sealed class Owner {

    companion object {
        private const val OWNER = 1
        private const val NOT_OWNER = 0

        /**
         * Converts the integer value returned over the wire to an object.
         *
         * @throws [IllegalArgumentException] if the [owner] integer is not supported
         * */
        fun fromInt(owner: Int): Owner =
            when (owner) {
                OWNER -> {
                    True
                }
                NOT_OWNER -> {
                    False
                }
                else -> {
                    throw IllegalArgumentException(
                        "Owner for integer '$owner' not supported"
                    )
                }
            }
    }

    abstract val value: Int

    object True: Owner() {
        override val value: Int
            get() = OWNER
    }

    object False: Owner() {
        override val value: Int
            get() = NOT_OWNER
    }
}
