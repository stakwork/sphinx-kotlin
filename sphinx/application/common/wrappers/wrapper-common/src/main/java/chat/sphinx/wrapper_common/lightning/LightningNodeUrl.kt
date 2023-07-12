package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun String.toLightningNodeLink(): LightningNodeLink? =
    try {
        LightningNodeLink(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val String.isValidLightningNodeLink: Boolean
    get() = isNotEmpty() && matches("^${LightningNodeLink.REGEX}\$".toRegex())

@Suppress("NOTHING_TO_INLINE")
inline fun String.isBitcoinNetwork(): Boolean =
    this == "bitcoin"


@JvmInline
value class LightningNodeLink(val value: String) {

    companion object {
        const val REGEX = "sphinx\\.chat:\\/\\/\\?action=glyph&mqtt=.*&network=.*"
        const val LIGHTNING_MQTT = "mqtt"
        const val LIGHTNING_NETWORK = "network"
    }

    init {
        require(value.isValidLightningNodeLink) {
            "Invalid Lightning Node Link"
        }
    }

    inline val lightningMqtt : String
        get() = (getComponent(LIGHTNING_MQTT) ?: "").trim()

    inline val lightningNetwork : String
        get() = (getComponent(LIGHTNING_NETWORK) ?: "").trim()

    fun getComponent(k: String): String? {
        val components = value.replace("sphinx.chat://", "").split("&")
        for (component in components) {
            val subComponents = component.split("=")
            val key:String? = if (subComponents.isNotEmpty()) subComponents.elementAtOrNull(0) else null
            val value:String? = if (subComponents.size > 1) subComponents.elementAtOrNull(1) else null

            if (key == k) return value
        }
        return null
    }
}
