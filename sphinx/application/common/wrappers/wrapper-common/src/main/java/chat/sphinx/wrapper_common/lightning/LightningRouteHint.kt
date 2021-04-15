package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun String.toLightningRouteHint(): LightningRouteHint? =
    try {
        LightningRouteHint(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline class LightningRouteHint(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "LightningRouteHint cannot be empty"
        }
    }
}
