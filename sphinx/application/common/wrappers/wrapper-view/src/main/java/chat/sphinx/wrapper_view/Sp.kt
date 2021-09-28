package chat.sphinx.wrapper_view

@Suppress("NOTHING_TO_INLINE")
inline fun Sp.add(sp: Sp): Sp =
    Sp(value + sp.value)

@Suppress("NOTHING_TO_INLINE")
inline fun Sp.subtract(sp: Sp): Sp =
    Sp(value - sp.value)

@Suppress("NOTHING_TO_INLINE")
inline fun Sp.multiplyBy(sp: Sp): Sp =
    Sp(value * sp.value)

@Suppress("NOTHING_TO_INLINE")
inline fun Sp.divideBy(sp: Sp): Sp =
    Sp(value / sp.value)

@JvmInline
value class Sp(val value: Float)
