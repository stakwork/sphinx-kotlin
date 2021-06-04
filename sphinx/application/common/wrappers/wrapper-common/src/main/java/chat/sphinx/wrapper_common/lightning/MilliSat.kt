package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun MilliSat.toSat(): Sat =
    Sat(value / 1_000L)

@Suppress("NOTHING_TO_INLINE")
inline fun Long.toMilliSat(): MilliSat? =
    try {
        MilliSat(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class MilliSat(val value: Long) {
    init {
        require(value >= 0L) {
            "MilliSat must be greater than or equal to 0"
        }
    }
}
