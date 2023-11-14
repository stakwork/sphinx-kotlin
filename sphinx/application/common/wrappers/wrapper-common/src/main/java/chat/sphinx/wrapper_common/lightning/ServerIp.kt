package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun String.toServerIp(): ServerIp? =
    try {
        ServerIp(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class ServerIp(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ServerIp cannot be empty"
        }
    }
}
