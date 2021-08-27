package chat.sphinx.wrapper_common


@Suppress("NOTHING_TO_INLINE")
inline fun String.toPeopleConnectLink(): PeopleConnectLink? =
    try {
        PeopleConnectLink(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val String.isValidPeopleConnectLink: Boolean
    get() = isNotEmpty() && matches("^${PeopleConnectLink.REGEX}\$".toRegex())

@JvmInline
value class PeopleConnectLink(val value: String) {

    companion object {
        const val REGEX = "sphinx\\.chat:\\/\\/\\?action=person&host=.*pubkey=.*"
        const val LINK_HOST = "host"
        const val LINK_PUB_KEY = "pubkey"
    }

    init {
        require(value.isValidPeopleConnectLink) {
            "Invalid People Connect Link"
        }
    }

    inline val host : String
        get() = getComponent(LINK_HOST) ?: ""

    inline val publicKey : String
        get() = getComponent(LINK_PUB_KEY) ?: ""

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