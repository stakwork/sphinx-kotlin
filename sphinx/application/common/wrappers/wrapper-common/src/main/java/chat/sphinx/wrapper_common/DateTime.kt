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
inline class DateTime(val value: Date) {

    companion object {
        private const val UTC = "UTC"

        private const val FORMAT_RELAY = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        private const val FORMAT_TODAY_00 = "yyyy-MM-dd'T00:00:00.000Z'"
        private const val FORMAT_H_MM_A = "h:mm a"
        private const val FORMAT_MMM = "MMM"
        private const val FORMAT_EEE_DD = "EEE dd"

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
    }

    override fun toString(): String {
        return getFormatRelay().format(value)
    }
}
