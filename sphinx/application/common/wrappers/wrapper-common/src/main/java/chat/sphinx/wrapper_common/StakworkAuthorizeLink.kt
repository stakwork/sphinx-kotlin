package chat.sphinx.wrapper_common


@Suppress("NOTHING_TO_INLINE")
inline fun String.toStakworkAuthorizeLink(): StakworkAuthorizeLink? =
    try {
        StakworkAuthorizeLink(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val String.isValidStakworkAuthorizeLink: Boolean
    get() = isNotEmpty() && matches("^${StakworkAuthorizeLink.REGEX}\$".toRegex())

@JvmInline
value class StakworkAuthorizeLink(val value: String) {

    companion object {
        const val REGEX = "sphinx\\.chat:\\/\\/\\?id=.*&challenge=.*&host=.*&scope=all&action=challenge"
        const val LINK_HOST = "host"
        const val LINK_CHALLENGE = "challenge"
        const val LINK_ID = "id"
    }

    init {
        require(value.isValidStakworkAuthorizeLink) {
            "Invalid Stakwork Authorize Link"
        }
    }

    inline val host : String
        get() = getComponent(LINK_HOST) ?: ""

    inline val challenge : String
        get() = getComponent(LINK_CHALLENGE) ?: ""

    inline val id : String
        get() = getComponent(LINK_ID) ?: ""

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