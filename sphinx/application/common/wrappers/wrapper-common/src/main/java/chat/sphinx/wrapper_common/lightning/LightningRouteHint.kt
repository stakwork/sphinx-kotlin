package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun String.toLightningRouteHint(): LightningRouteHint? =
    try {
        LightningRouteHint(this)
    } catch (e: IllegalArgumentException) {
        null
    }
@Suppress("NOTHING_TO_INLINE")
inline fun retrieveLightningRouteHint(lspPubKey: String?, scid: String?): LightningRouteHint? {
    val routeHint = "${lspPubKey}_${scid}"
    return routeHint.toLightningRouteHint()
}
@Suppress("NOTHING_TO_INLINE")
inline fun LightningRouteHint.getLspPubKey(): String = this.value.substringBefore('_')

@Suppress("NOTHING_TO_INLINE")
inline fun LightningRouteHint.getScid(): String = this.value.substringAfter('_')

inline val String.isValidLightningRouteHint: Boolean
    get() = isNotEmpty() && matches("^${LightningRouteHint.REGEX}\$".toRegex())


@JvmInline
value class LightningRouteHint(val value: String) {

    companion object {
        const val REGEX = "[A-F0-9a-f]{66}(:|_)[0-9]+"
    }

    init {
        require(value.isValidLightningRouteHint) {
            "Invalid Lightning Route Hint"
        }
    }
}
