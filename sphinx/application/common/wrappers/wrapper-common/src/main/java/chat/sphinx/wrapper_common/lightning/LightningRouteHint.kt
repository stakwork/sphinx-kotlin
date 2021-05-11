package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun String.toLightningRouteHint(): LightningRouteHint? =
    try {
        LightningRouteHint(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val LightningRouteHint.isValid: Boolean
    get() {
        return !this.value.isNullOrBlank() &&
                this.value.matches("^[A-F0-9a-f]{66}:[0-9]+\$".toRegex())
    }

inline class LightningRouteHint(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "LightningRouteHint cannot be empty"
        }
    }
}
