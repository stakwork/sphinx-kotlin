package chat.sphinx.wrapper_common

@Suppress("NOTHING_TO_INLINE")
inline fun Push.isTrue(): Boolean =
    this is Push.True

@Suppress("NOTHING_TO_INLINE")
inline fun Int.toPush(): Push =
    when (this) {
        Push.PUSH -> {
            Push.True
        }
        else -> {
            Push.False
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Boolean.toPush(): Push =
    if (this) Push.True else Push.False

/**
 * Comes off the wire as:
 *  - 0 (Not Push)
 *  - 1 (Push)
 *  - true (Push)
 *  - false (Not Push)
 * */
sealed class Push {

    companion object {
        const val PUSH = 1
        const val NOT_PUSH = 0
    }

    abstract val value: Int

    object True: Push() {
        override val value: Int
            get() = PUSH
    }

    object False: Push() {
        override val value: Int
            get() = NOT_PUSH
    }
}
