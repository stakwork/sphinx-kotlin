package chat.sphinx.wrapper_message

@Suppress("NOTHING_TO_INLINE")
inline fun Flagged.isTrue(): Boolean =
    this is Flagged.True

@Suppress("NOTHING_TO_INLINE")
inline fun Int.toFlagged(): Flagged =
    when (this) {
        Flagged.FLAGGED -> {
            Flagged.True
        }
        else -> {
            Flagged.False
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Boolean.toFlagged(): Flagged =
    if (this) Flagged.True else Flagged.False

sealed class Flagged {

    companion object {
        const val FLAGGED = 1
        const val NOT_FLAGGED = 0
    }

    abstract val value: Int

    object True: Flagged() {
        override val value: Int
            get() = FLAGGED
    }

    object False: Flagged() {
        override val value: Int
            get() = NOT_FLAGGED
    }
}
