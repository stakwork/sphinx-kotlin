package chat.sphinx.wrapper_common


@Suppress("NOTHING_TO_INLINE")
inline fun String.toExternalRequestLink(): ExternalRequestLink? =
    try {
        ExternalRequestLink(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val String.isValidExternalRequestLink: Boolean
    get() = isNotEmpty() && matches("^${ExternalRequestLink.REGEX}\$".toRegex())

@JvmInline
value class ExternalRequestLink(val value: String) {

    companion object {
        const val REGEX = "sphinx\\.chat:\\/\\/\\?action=save&host=.*key=.*"
        const val LINK_HOST = "host"
        const val LINK_KEY = "key"
    }

    init {
        require(value.isValidExternalRequestLink) {
            "Invalid Save Profile Link"
        }
    }

    inline val host : String
        get() = getComponent(LINK_HOST) ?: ""

    inline val key : String
        get() = getComponent(LINK_KEY) ?: ""

    fun getComponent(k: String): String? {
        val components = value.replace("sphinx.chat://?", "").split("&")
        for (component in components) {
            val key:String? = component.substringBefore("=")

            if (key != null) {
                val value: String? = component.replace("$key=", "")
                if (key == k) return value
            }
        }
        return null
    }

}