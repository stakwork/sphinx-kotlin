package chat.sphinx.wrapper_message_media

import chat.sphinx.wrapper_message_media.token.MediaHost
import chat.sphinx.wrapper_message_media.token.MediaMUID
import chat.sphinx.wrapper_message_media.token.toMediaHostOrNull
import chat.sphinx.wrapper_message_media.token.toMediaMUIDOrNull
import okio.base64.decodeBase64ToArray

@Suppress("NOTHING_TO_INLINE")
inline fun String.toMediaToken(): MediaToken? =
    try {
        MediaToken(this)
    } catch (e: IllegalArgumentException) {
        null
    }

//Media
@Suppress("NOTHING_TO_INLINE")
inline fun MediaToken.getHostFromMediaToken(): MediaHost? =
    getMediaTokenElementWithIndex(0)?.toMediaHostOrNull()

@Suppress("NOTHING_TO_INLINE")
inline fun MediaToken.getMUIDFromMediaToken(): MediaMUID? =
    getMediaTokenElementWithIndex(1)?.toMediaMUIDOrNull()

@Suppress("NOTHING_TO_INLINE")
inline fun MediaToken.getMediaAttributeWithName(name: String): String? {
    getMediaTokenElementWithIndex(4)?.let { metaData ->
        metaData.split("&").let { metaDataItems ->
            for (item in metaDataItems) {
                if (item.contains("$name=")) {
                    return item.replace("$name=", "")
                }
            }
        }
    }
    return null
}

@Suppress("NOTHING_TO_INLINE")
inline fun MediaToken.getMediaTokenElementWithIndex(index: Int): String? {
    value.split(".").filter { it.isNotBlank() }.let { splits ->
        if (splits.size > index) {
            val element = splits[index]
            return element.decodeBase64ToArray()?.toString(charset("UTF-8"))
        }
    }
    return null
}

@JvmInline
value class MediaToken(val value: String) {

    companion object {
        val PROVISIONAL_TOKEN = MediaToken("ProvisionalMediaToken")
    }

    init {
        require(value.isNotEmpty()) {
            "MediaToken cannot be empty"
        }
    }
}
