package chat.sphinx.wrapper_subscription

@Suppress("NOTHING_TO_INLINE")
inline fun SubscriptionInterval.isDaily(): Boolean =
    this is SubscriptionInterval.Daily

@Suppress("NOTHING_TO_INLINE")
inline fun SubscriptionInterval.isWeekly(): Boolean =
    this is SubscriptionInterval.Weekly

@Suppress("NOTHING_TO_INLINE")
inline fun SubscriptionInterval.isMonthly(): Boolean =
    this is SubscriptionInterval.Monthly

@Suppress("NOTHING_TO_INLINE")
inline fun SubscriptionInterval.isUnknown(): Boolean =
    this is SubscriptionInterval.Unknown

/**
 * Converts the integer value returned over the wire to an object.
 *
 * @throws [IllegalArgumentException] if the integer is not supported
 * */
@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class)
inline fun String.toSubscriptionInterval(): SubscriptionInterval =
    when (this) {
        SubscriptionInterval.DAILY -> {
            SubscriptionInterval.Daily
        }
        SubscriptionInterval.WEEKLY -> {
            SubscriptionInterval.Weekly
        }
        SubscriptionInterval.MONTHLY -> {
            SubscriptionInterval.Monthly
        }
        else -> {
            SubscriptionInterval.Unknown(this)
        }
    }

/**
 * Comes off the wire as:
 *  - "daily" (Daily)
 *  - "weekly" (Weekly)
 *  - "monthly" (Monthly)
 * */
sealed class SubscriptionInterval {

    companion object {
        const val DAILY = "daily"
        const val WEEKLY = "weekly"
        const val MONTHLY = "monthly"
    }

    abstract val value: String

    object Daily: SubscriptionInterval() {
        override val value: String
            get() = DAILY
    }

    object Weekly: SubscriptionInterval() {
        override val value: String
            get() = WEEKLY
    }

    object Monthly: SubscriptionInterval() {
        override val value: String
            get() = MONTHLY
    }

    data class Unknown(override val value: String): SubscriptionInterval()
}
