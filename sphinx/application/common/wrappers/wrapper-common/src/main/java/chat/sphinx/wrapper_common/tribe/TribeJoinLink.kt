package chat.sphinx.wrapper_common.tribe

@Suppress("NOTHING_TO_INLINE")
inline fun String.toTribeJoinLink(): TribeJoinLink? =
    try {
        TribeJoinLink(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val String.isValidTribeJoinLink: Boolean
    get() = isNotEmpty() && matches("^${TribeJoinLink.REGEX}\$".toRegex())

@JvmInline
value class TribeJoinLink(val value: String) {

    companion object {
        const val REGEX = "sphinx\\.chat:\\/\\/\\?action=tribe(V2)?&.*"
        const val TRIBE_HOST = "host"
        const val TRIBE_PUBKEY = "pubkey"
    }

    init {
        require(value.matches(REGEX.toRegex())) {
            "Invalid Tribe Join Link"
        }
    }

    inline val tribeHost: String
        get() = (getComponent(TRIBE_HOST) ?: "").trim()

    inline val tribePubkey: String
        get() = (getComponent(TRIBE_PUBKEY) ?: "").trim()

    fun getComponent(k: String): String? {
        val components = value.replace("sphinx.chat://", "").split("&")
        for (component in components) {
            val subComponents = component.split("=")
            val key: String? = if (subComponents.isNotEmpty()) subComponents.elementAtOrNull(0) else null
            val value: String? = if (subComponents.size > 1) subComponents.elementAtOrNull(1) else null

            if (key == k) return value
        }
        return null
    }
}