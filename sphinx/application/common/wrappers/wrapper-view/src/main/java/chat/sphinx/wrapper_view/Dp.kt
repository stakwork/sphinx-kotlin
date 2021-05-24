package chat.sphinx.wrapper_view

@Suppress("NOTHING_TO_INLINE")
inline fun Dp.add(dp: Dp): Dp =
    Dp(value + dp.value)

@Suppress("NOTHING_TO_INLINE")
inline fun Dp.subtract(dp: Dp): Dp =
    Dp(value - dp.value)

@Suppress("NOTHING_TO_INLINE")
inline fun Dp.multiplyBy(dp: Dp): Dp =
    Dp(value * dp.value)

@Suppress("NOTHING_TO_INLINE")
inline fun Dp.divideBy(dp: Dp): Dp =
    Dp(value / dp.value)

@JvmInline
value class Dp(val value: Float)
