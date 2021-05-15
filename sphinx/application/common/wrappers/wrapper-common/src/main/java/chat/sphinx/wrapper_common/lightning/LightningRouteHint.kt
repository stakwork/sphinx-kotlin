package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun String.toLightningRouteHint(): LightningRouteHint? =
    try {
        LightningRouteHint(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val String.isValidLightningRouteHint: Boolean
    get() = isNotEmpty() && matches("^${LightningRouteHint.REGEX}\$".toRegex())

inline class LightningRouteHint(val value: String) {

    companion object {
        const val REGEX = "[A-F0-9a-f]{66}:[0-9]+"
    }

    init {
        require(value.isValidLightningRouteHint) {
            "Invalid Lightning Route Hint"
        }
    }
}
