package chat.sphinx.wrapper_common

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("NOTHING_TO_INLINE")
@Throws(ParseException::class)
inline fun String.toDateTime(): DateTime =
    DateTime(DateTime.getFormat().parse(this))

@Suppress("NOTHING_TO_INLINE")
inline fun Long.toDateTime(): DateTime =
    DateTime(Date(this))

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
 * */
inline class DateTime(val value: Date) {

    companion object {
        private var format: SimpleDateFormat? = null
        fun getFormat(): SimpleDateFormat =
            format ?: synchronized(this) {
                format ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    .also { format = it }
            }
    }

    override fun toString(): String {
        return getFormat().format(value)
    }
}
