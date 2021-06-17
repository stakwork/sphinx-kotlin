package chat.sphinx.wrapper_view

@Suppress("NOTHING_TO_INLINE")
inline fun Px.add(px: Px): Px =
    Px(value + px.value)

@Suppress("NOTHING_TO_INLINE")
inline fun Px.subtract(px: Px): Px =
    Px(value - px.value)

@Suppress("NOTHING_TO_INLINE")
inline fun Px.multiplyBy(px: Px): Px =
    Px(value * px.value)

@Suppress("NOTHING_TO_INLINE")
inline fun Px.multiplyBy(float: Float): Px =
    Px(value * float)

@Suppress("NOTHING_TO_INLINE")
inline fun Px.divideBy(px: Px): Px =
    Px(value / px.value)

@Throws(ArithmeticException::class)
@Suppress("NOTHING_TO_INLINE")
inline fun Px.divideBy(float: Float): Px =
    Px(value / float)

@Suppress("NOTHING_TO_INLINE")
inline fun Px.isGreaterThanOrEqualTo(px: Px): Boolean =
    value >= px.value

@Suppress("NOTHING_TO_INLINE")
inline fun Px.isLessThanOrEqualTo(px: Px): Boolean =
    value <= px.value

@JvmInline
value class Px(val value: Float)
