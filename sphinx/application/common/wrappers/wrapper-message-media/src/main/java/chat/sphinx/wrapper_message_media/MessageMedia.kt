package chat.sphinx.wrapper_message_media

import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_message_media.token.*

data class MessageMedia(
    val mediaKey: MediaKey?,
    val mediaKeyDecrypted: MediaKeyDecrypted?,
    val mediaType: MediaType,
    val mediaToken: MediaToken,
) {
    @Volatile
    var mediaKeyDecryptionError: Boolean = false
        private set
    @Volatile
    var mediaKeyDecryptionException: Exception? = null

    fun setDecryptionError(e: Exception?): MessageMedia {
        mediaKeyDecryptionError = true
        mediaKeyDecryptionException = e
        return this
    }

    companion object {
        private const val AMT = "amt"
    }

    val price: Sat by lazy {
        mediaToken.getMediaAttributeWithName(AMT)
            ?.toLongOrNull()
            ?.toSat()
            ?: Sat(0)
    }

    val host: MediaHost? by lazy {
        mediaToken.getHostFromMediaToken()
    }

    val url: MediaUrl? by lazy {
        host?.toMediaUrl(mediaToken)
    }

    @Suppress("SpellCheckingInspection")
    val muid: MediaMUID? by lazy {
        mediaToken.getMUIDFromMediaToken()
    }
}
