package chat.sphinx.wrapper_common.lightning

import java.text.NumberFormat
import java.util.*

@Suppress("NOTHING_TO_INLINE")
inline fun Sat.toMilliSat(): MilliSat =
    MilliSat(value * 1_000L)

@Suppress("NOTHING_TO_INLINE")
inline fun Long.toSat(): Sat? =
    try {
        Sat(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val Sat.unit: String
    get() = if (value > 1) "sats" else "sat"

/**
 * Will format the value using the [separator] passed.
 *
 * ex:
 *  (separator = ',') 1000000 -> 1,000,000
 *  (separator = ' ') 1000000 -> 1 000 000
 * */
@Suppress("NOTHING_TO_INLINE")
inline fun Sat.asFormattedString(separator: Char = ' ') =
    NumberFormat.getInstance(Locale.ENGLISH).format(value).replace(',', separator)

@JvmInline
value class Sat(val value: Long) {
    init {
        require(value >= 0L) {
            "Sat must be greater than or equal to 0"
        }
    }
}
