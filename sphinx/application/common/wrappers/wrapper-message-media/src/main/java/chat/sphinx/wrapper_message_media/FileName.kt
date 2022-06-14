package chat.sphinx.wrapper_message_media

@Suppress("NOTHING_TO_INLINE")
inline fun String.toFileName(): FileName? =
    try {
        FileName(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@Suppress("NOTHING_TO_INLINE")
inline fun FileName.getExtension(): String? {
    value.split(".").let { splits ->
        if (splits.isNotEmpty()) {
            return splits.last()
        }
    }
    return null
}

@JvmInline
value class FileName(val value: String) {

    init {
        require(value.isNotEmpty()) {
            "FileName cannot be empty"
        }
    }
}
