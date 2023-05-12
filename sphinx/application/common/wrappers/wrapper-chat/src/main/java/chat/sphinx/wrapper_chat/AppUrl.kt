package chat.sphinx.wrapper_chat

@Suppress("NOTHING_TO_INLINE")
inline fun String.toAppUrl(): AppUrl? =
    try {
        AppUrl(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class AppUrl(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "AppUrl cannot be empty"
        }
    }
}

inline val AppUrl.protocolLessUrl: String
    get() = this
        .value
        .replaceFirst("http://", "")
        .replaceFirst("https://", "")
