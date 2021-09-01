package chat.sphinx.wrapper_common

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Will always format to [DateTime.formatRelay] which is:
 *
 *  - [Locale] = [Locale.ENGLISH]
 *  - [TimeZone] UTC
 * */
@Suppress("NOTHING_TO_INLINE")
@Throws(ParseException::class)
inline fun String.toDateTime(): DateTime =
    DateTime(DateTime.getFormatRelay().parse(this))

@Suppress("NOTHING_TO_INLINE")
@Throws(ParseException::class)
inline fun String.toDateTimeWithFormat(format: SimpleDateFormat): DateTime =
    DateTime(format.parse(this))

@Suppress("NOTHING_TO_INLINE")
inline fun Long.toDateTime(): DateTime =
    DateTime(Date(this))

/**
 * Returns:
 *  - `hh:mm AM or PM` if [this] is after 00:00:00.000 for today's date,
 *  - `EEE dd` if [this] is before 00:00:00.000 for today's date and the same month
 *  - `EEE dd MMM` if [this] is before 00:00:00.000 for today's date and **not** the same month
 *
 * @param [today00] can be retrieved from [DateTime.getToday00] and passed here in order to
 * reduce resource consumption if desired.
 * */
@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun DateTime.hhmmElseDate(today00: DateTime = DateTime.getToday00()): String {
    return if (today00.before(this)) {
        DateTime.getFormathmma().format(value)
    } else {
        val dtMonth: String = DateTime.getFormatMMM().format(value)
        val todayMonth: String = DateTime.getFormatMMM().format(today00.value)

        if (dtMonth == todayMonth) {
            DateTime.getFormatEEEdd().format(value)
        } else {
            DateTime.getFormatEEEdd().format(value) + " $dtMonth"
        }
    }
}

@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun DateTime.chatTimeFormat(
    today00: DateTime = DateTime.getToday00(),
    sixDaysAgo: DateTime = DateTime.getSixDaysAgo()
): String =
    when {
        today00.before(this) -> {
            DateTime.getFormathmma().format(value)
        }
        sixDaysAgo.before(this) -> {
            DateTime.getFormatEEEhmma().format(value)
        }
        else -> {
            DateTime.getFormatddmmmhhmm().format(value)
        }
    }

@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun DateTime.eeemmddhmma(): String =
    DateTime.getFormateeemmddhmma().format(value)

inline val DateTime.time: Long
    get() = value.time

@Suppress("NOTHING_TO_INLINE")
inline fun DateTime.after(dateTime: DateTime): Boolean =
    value.after(dateTime.value)

@Suppress("NOTHING_TO_INLINE")
inline fun DateTime.before(dateTime: DateTime): Boolean =
    value.before(dateTime.value)

/**
 * DateTime format from Relay: 2021-02-26T10:48:20.025Z
 *
 * See https://www.datetimeformatter.com/how-to-format-date-time-in-java-7/#examples
 * */
@JvmInline
value class DateTime(val value: Date) {

    companion object {
        const val UTC = "UTC"

        private const val FORMAT_RELAY = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        private const val FORMAT_TODAY_00 = "yyyy-MM-dd'T00:00:00.000Z'"
        private const val FORMAT_H_MM_A = "h:mm a"
        private const val FORMAT_EEE_H_MM_A = "EEE h:mm a"
        private const val FORMAT_MMM = "MMM"
        private const val FORMAT_EEE_DD = "EEE dd"
        private const val FORMAT_EEE_MM_DD_H_MM_A = "EEE MMM dd, h:mm a"
        private const val FORMAT_DD_MMM_HH_MM = "dd MMM, HH:mm"
        private const val FORMAT_MMM_DD_YYYY = "MMM dd, yyyy"

        private const val SIX_DAYS_IN_MILLISECONDS = 518_400_000L

        @Volatile
        private var formatRelay: SimpleDateFormat? = null
        fun getFormatRelay(): SimpleDateFormat =
            formatRelay ?: synchronized(this) {
                formatRelay ?: SimpleDateFormat(FORMAT_RELAY, Locale.ENGLISH)
                    .also {
                        it.timeZone = TimeZone.getTimeZone(UTC)
                        formatRelay = it
                    }
            }

        /**
         * Returns a string value using [FORMAT_RELAY]
         * */
        fun nowUTC(): String =
            getFormatRelay().format(Date(System.currentTimeMillis()))

        @Volatile
        private var formatToday00: SimpleDateFormat? = null
        fun getFormatToday00(): SimpleDateFormat =
            formatToday00?.also {
                it.timeZone = TimeZone.getDefault()
            } ?: synchronized(this) {
                formatToday00?.also {
                    it.timeZone = TimeZone.getDefault()
                } ?: SimpleDateFormat(FORMAT_TODAY_00, Locale.getDefault())
                    .also {
                        it.timeZone = TimeZone.getDefault()
                        formatToday00 = it
                    }
            }

        /**
         * Uses the [getFormatToday00] to create a [DateTime] formatted
         * for the current date, in the local timezone, in local language
         * at 00:00:00.000
         * */
        fun getToday00(): DateTime =
            getFormatToday00()
            .format(
                Date(System.currentTimeMillis())
            )
            .toDateTime()

        /**
         * Create a [DateTime] that is 6 days from the current time
         * */
        fun getSixDaysAgo(): DateTime =
            DateTime(Date(System.currentTimeMillis() - SIX_DAYS_IN_MILLISECONDS))

        @Volatile
        private var formateeemmddhmma: SimpleDateFormat? = null
        @Suppress("SpellCheckingInspection")
        fun getFormateeemmddhmma(): SimpleDateFormat =
            formateeemmddhmma?.also {
                it.timeZone = TimeZone.getDefault()
            } ?: synchronized(this) {
                formateeemmddhmma?.also {
                    it.timeZone = TimeZone.getDefault()
                } ?: SimpleDateFormat(FORMAT_EEE_MM_DD_H_MM_A, Locale.getDefault())
                    .also {
                        it.timeZone = TimeZone.getDefault()
                        formateeemmddhmma = it
                    }
            }

        @Volatile
        private var formatddmmmhhmm: SimpleDateFormat? = null
        @Suppress("SpellCheckingInspection")
        fun getFormatddmmmhhmm(): SimpleDateFormat =
            formatddmmmhhmm?.also {
                it.timeZone = TimeZone.getDefault()
            } ?: synchronized(this) {
                formatddmmmhhmm?.also {
                    it.timeZone = TimeZone.getDefault()
                } ?: SimpleDateFormat(FORMAT_DD_MMM_HH_MM, Locale.getDefault())
                    .also {
                        it.timeZone = TimeZone.getDefault()
                        formatddmmmhhmm = it
                    }
            }

        @Volatile
        private var formathmma: SimpleDateFormat? = null
        @Suppress("SpellCheckingInspection")
        fun getFormathmma(): SimpleDateFormat =
            formathmma?.also {
                it.timeZone = TimeZone.getDefault()
            } ?: synchronized(this) {
                formathmma?.also {
                    it.timeZone = TimeZone.getDefault()
                } ?: SimpleDateFormat(FORMAT_H_MM_A, Locale.getDefault())
                    .also {
                        it.timeZone = TimeZone.getDefault()
                        formathmma = it
                    }
            }

        @Volatile
        private var formateeehmma: SimpleDateFormat? = null
        @Suppress("SpellCheckingInspection")
        fun getFormatEEEhmma(): SimpleDateFormat =
            formateeehmma?.also {
                it.timeZone = TimeZone.getDefault()
            } ?: synchronized(this) {
                formateeehmma?.also {
                    it.timeZone = TimeZone.getDefault()
                } ?: SimpleDateFormat(FORMAT_EEE_H_MM_A, Locale.getDefault())
                    .also {
                        it.timeZone = TimeZone.getDefault()
                        formateeehmma = it
                    }
            }

        @Volatile
        private var formatMMM: SimpleDateFormat? = null
        fun getFormatMMM(): SimpleDateFormat =
            formatMMM?.also {
                it.timeZone = TimeZone.getDefault()
            } ?: synchronized(this) {
                formatMMM?.also {
                    it.timeZone = TimeZone.getDefault()
                } ?: SimpleDateFormat(FORMAT_MMM, Locale.getDefault())
                    .also {
                        it.timeZone = TimeZone.getDefault()
                        formatMMM = it
                    }
            }

        @Volatile
        private var formatEEEdd: SimpleDateFormat? = null
        fun getFormatEEEdd(): SimpleDateFormat =
            formatEEEdd?.also {
                it.timeZone = TimeZone.getDefault()
            } ?: synchronized(this) {
                formatEEEdd?.also {
                    it.timeZone = TimeZone.getDefault()
                } ?: SimpleDateFormat(FORMAT_EEE_DD, Locale.getDefault())
                    .also {
                        it.timeZone = TimeZone.getDefault()
                        formatEEEdd = it
                    }
            }

        @Volatile
        private var formatMMMddyyyy: SimpleDateFormat? = null
        fun getFormatMMMddyyyy(timeZone: TimeZone = TimeZone.getDefault()): SimpleDateFormat =
            formatMMMddyyyy?.also {
                it.timeZone = timeZone
            } ?: synchronized(this) {
                formatMMMddyyyy?.also {
                    it.timeZone = timeZone
                } ?: SimpleDateFormat(FORMAT_MMM_DD_YYYY, Locale.getDefault())
                    .also {
                        it.timeZone = timeZone
                        formatMMMddyyyy = it
                    }
            }
    }

    override fun toString(): String {
        return getFormatRelay().format(value)
    }
}
