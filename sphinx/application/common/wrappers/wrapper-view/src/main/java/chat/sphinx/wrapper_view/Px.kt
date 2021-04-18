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
inline fun Px.divideBy(px: Px): Px =
    Px(value / px.value)

inline class Px(val value: Float)
