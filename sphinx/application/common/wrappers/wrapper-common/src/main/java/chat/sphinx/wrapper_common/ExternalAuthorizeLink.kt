package chat.sphinx.wrapper_common


@Suppress("NOTHING_TO_INLINE")
inline fun String.toExternalAuthorizeLink(): ExternalAuthorizeLink? =
    try {
        ExternalAuthorizeLink(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val String.isValidExternalAuthorizeLink: Boolean
    get() = isNotEmpty() && matches("^${ExternalAuthorizeLink.REGEX}\$".toRegex())

@JvmInline
value class ExternalAuthorizeLink(val value: String) {

    companion object {
        const val REGEX = "sphinx\\.chat:\\/\\/\\?action=auth&host=.*challenge=.*"
        const val LINK_HOST = "host"
        const val LINK_CHALLENGE = "challenge"
    }

    init {
        require(value.isValidExternalAuthorizeLink) {
            "Invalid External Authorize Link"
        }
    }

    inline val host : String
        get() = getComponent(LINK_HOST) ?: ""

    inline val challenge : String
        get() = getComponent(LINK_CHALLENGE) ?: ""

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