package chat.sphinx.wrapper_common


@Suppress("NOTHING_TO_INLINE")
inline fun String.toRedeemSatsLink(): RedeemSatsLink? =
    try {
        RedeemSatsLink(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val String.isValidRedeemSatsLink: Boolean
    get() = isNotEmpty() && matches("^${RedeemSatsLink.REGEX}\$".toRegex())

@JvmInline
value class RedeemSatsLink(val value: String) {

    companion object {
        const val REGEX = "sphinx\\.chat:\\/\\/\\?host=.*&token=.*&amount=.*&action=redeem_sats"
        const val LINK_HOST = "host"
        const val LINK_TOKEN = "token"
        const val LINK_AMOUNT = "amount"
    }

    init {
        require(value.isValidRedeemSatsLink) {
            "Invalid Redeem Sats Link"
        }
    }

    inline val host : String
        get() = getComponent(LINK_HOST) ?: ""

    inline val token : String
        get() = getComponent(LINK_TOKEN) ?: ""

    inline val amount : String
        get() = getComponent(LINK_AMOUNT) ?: ""

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