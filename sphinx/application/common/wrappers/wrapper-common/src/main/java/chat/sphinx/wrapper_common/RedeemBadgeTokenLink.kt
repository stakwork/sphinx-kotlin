package chat.sphinx.wrapper_common


@Suppress("NOTHING_TO_INLINE")
inline fun String.toRedeemBadgeTokenLink(): RedeemBadgeTokenLink? =
    try {
        RedeemBadgeTokenLink(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val String.isValidRedeemBadgeTokenLink: Boolean
    get() = isNotEmpty() && matches("^${RedeemBadgeTokenLink.REGEX}\$".toRegex())

@JvmInline
value class RedeemBadgeTokenLink(val value: String) {

    companion object {
        const val REGEX = "sphinx\\.chat:\\/\\/\\?action=save&host=.*key=.*"
        const val LINK_HOST = "host"
        const val LINK_KEY = "key"
    }

    init {
        require(value.isValidRedeemBadgeTokenLink) {
            "Invalid Redeem Badge Token Link"
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

