package chat.sphinx.wrapper_message_media

import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_message_media.token.*
import java.io.File

abstract class MessageMedia {
    abstract val mediaKey: MediaKey?
    abstract val mediaType: MediaType
    abstract val mediaToken: MediaToken
    abstract val localFile: File?

    abstract val mediaKeyDecrypted: MediaKeyDecrypted?
    abstract val mediaKeyDecryptionError: Boolean
    abstract val mediaKeyDecryptionException: Exception?

    override fun equals(other: Any?): Boolean {
        return  other                               is MessageMedia                &&
                other.mediaKey                      == mediaKey                     &&
                other.mediaType                     == mediaType                    &&
                other.mediaToken                    == mediaToken                   &&
                other.localFile                     == localFile                    &&
                other.mediaKeyDecrypted             == mediaKeyDecrypted            &&
                other.mediaKeyDecryptionError       == mediaKeyDecryptionError
    }

    companion object {
        @Suppress("ObjectPropertyName")
        private const val _17 = 17
        @Suppress("ObjectPropertyName")
        private const val _31 = 31

        private const val AMT = "amt"
    }

    override fun hashCode(): Int {
        var result = _17
        result = _31 * result + mediaKey.hashCode()
        result = _31 * result + mediaType.hashCode()
        result = _31 * result + mediaToken.hashCode()
        result = _31 * result + localFile.hashCode()
        result = _31 * result + mediaKeyDecrypted.hashCode()
        result = _31 * result + mediaKeyDecryptionError.hashCode()
        return result
    }

    override fun toString(): String {
        return  "MessageMedia(mediaKey=$mediaKey,mediaType=$mediaType,mediaToken=$mediaToken," +
                "localFile=$localFile,mediaKeyDecrypted=$mediaKeyDecrypted," +
                "mediaKeyDecryptionError=$mediaKeyDecryptionError," +
                "mediaKeyDecryptionException=$mediaKeyDecryptionException)"
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
