package chat.sphinx.wrapper_common.chat

@Suppress("NOTHING_TO_INLINE")
inline fun String.toPushNotificationLink(): PushNotificationLink? =
    try {
        PushNotificationLink(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val String.isValidPushNotificationLink: Boolean
    get() = isNotEmpty() && matches("^${PushNotificationLink.REGEX}\$".toRegex())

@JvmInline
value class PushNotificationLink(val value: String) {

    companion object {
        const val REGEX = "sphinx\\.chat:\\/\\/\\?action=push&chatId=.*"
        const val CHAT_ID = "chatId"
    }

    init {
        require(value.isValidPushNotificationLink) {
            "Invalid Push Notification Link"
        }
    }

    inline val chatId : Long?
        get() = (getComponent(CHAT_ID) ?: "").trim().toLongOrNull()

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