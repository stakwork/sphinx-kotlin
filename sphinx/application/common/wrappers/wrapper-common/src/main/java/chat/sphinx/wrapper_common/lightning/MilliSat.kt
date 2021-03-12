package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun MilliSat.toSat(): Sat =
    Sat(value / 1_000L)

inline class MilliSat(val value: Long) {
    init {
        require(value >= 0L) {
            "MilliSat must be greater than or equal to 0"
        }
    }
}
