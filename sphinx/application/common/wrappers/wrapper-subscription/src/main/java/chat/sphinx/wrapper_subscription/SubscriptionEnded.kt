package chat.sphinx.wrapper_subscription

@Suppress("NOTHING_TO_INLINE")
inline fun SubscriptionEnded.isTrue(): Boolean =
    this is SubscriptionEnded.True

@Suppress("NOTHING_TO_INLINE")
inline fun Int.toSubscriptionEnded(): SubscriptionEnded =
    when (this) {
        SubscriptionEnded.ENDED -> {
            SubscriptionEnded.True
        }
        else -> {
            SubscriptionEnded.False
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Boolean.toSubscriptionEnded(): SubscriptionEnded =
    if (this) SubscriptionEnded.True else SubscriptionEnded.False

/**
 * Comes off the wire as:
 *  - 0 (Not Ended)
 *  - 1 (Ended)
 *  - true (Ended)
 *  - false (Not Ended)
 * */
sealed class SubscriptionEnded {

    companion object {
        const val ENDED = 1
        const val NOT_ENDED = 0
    }

    abstract val value: Int

    object True: SubscriptionEnded() {
        override val value: Int
            get() = ENDED
    }

    object False: SubscriptionEnded() {
        override val value: Int
            get() = NOT_ENDED
    }
}
