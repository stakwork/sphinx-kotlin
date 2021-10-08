package chat.sphinx.wrapper_message_media.token

import chat.sphinx.wrapper_message_media.MediaToken

@Suppress("NOTHING_TO_INLINE")
inline fun String.toMediaHostOrNull(): MediaHost? =
    try {
        MediaHost(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@Suppress("NOTHING_TO_INLINE")
inline fun MediaHost.toMediaUrl(mediaToken: MediaToken): MediaUrl =
    MediaUrl("https://$value/file/${mediaToken.value}")

@Suppress("NOTHING_TO_INLINE")
inline fun MediaHost.toTemplateUrl(mediaMUID: MediaMUID): MediaUrl =
    MediaUrl("https://$value/template/${mediaMUID.value}")

@JvmInline
value class MediaHost(val value: String) {

    companion object {
        val DEFAULT = MediaHost("memes.sphinx.chat")
    }

    init {
        require(value.isNotEmpty()) {
            "MediaHost cannot be empty"
        }
        require(!value.startsWith("http")) {
            "MediaHost cannot contain a scheme (http/https)"
        }
    }
}
