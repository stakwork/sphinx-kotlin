package chat.sphinx.wrapper_contact

@Suppress("NOTHING_TO_INLINE")
inline fun PrivatePhoto.isTrue(): Boolean =
    this is PrivatePhoto.True

@Suppress("NOTHING_TO_INLINE")
inline fun Int?.toPrivatePhoto(): PrivatePhoto =
    when (this) {
        PrivatePhoto.PRIVATE -> {
            PrivatePhoto.True
        }
        else -> {
            PrivatePhoto.False
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Boolean.toPrivatePhoto(): PrivatePhoto =
    if (this) PrivatePhoto.True else PrivatePhoto.False

/**
 * Comes off the wire as:
 *  - null (Not Private)
 *  - 0 (Not Private)
 *  - 1 (Private)
 * */
sealed class PrivatePhoto {

    companion object {
        const val PRIVATE = 1
        const val NOT_PRIVATE = 0
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
