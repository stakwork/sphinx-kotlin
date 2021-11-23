package chat.sphinx.wrapper_common.feed

@Suppress("NOTHING_TO_INLINE")
inline fun Subscribed.isTrue(): Boolean =
    this is Subscribed.True

@Suppress("NOTHING_TO_INLINE")
inline fun Int.toSubscribed(): Subscribed =
    when (this) {
        Subscribed.SUBSCRIBED -> {
            Subscribed.True
        }
        else -> {
            Subscribed.False
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Boolean.toSubscribed(): Subscribed =
    if (this) Subscribed.True else Subscribed.False

sealed class Subscribed {

    companion object {
        const val SUBSCRIBED = 1
        const val NOT_SUBSCRIBED = 0
    }

    abstract val value: Int

    object True: Subscribed() {
        override val value: Int
            get() = SUBSCRIBED
    }

    object False: Subscribed() {
        override val value: Int
            get() = NOT_SUBSCRIBED
    }
}
