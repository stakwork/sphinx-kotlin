package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun Sat.toMilliSat(): MilliSat =
    MilliSat(value * 1_000L)

inline class Sat(val value: Long) {
    init {
        require(value >= 0L) {
            "Sat must be greater than or equal to 0"
        }
    }
}
