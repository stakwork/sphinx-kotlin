package chat.sphinx.feature_repository.model.message

import chat.sphinx.conceptcoredb.MessageMediaDbo
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_message_media.*
import java.io.File

class MessageMediaDboWrapper(val messageMediaDbo: MessageMediaDbo): MessageMedia() {
    override val mediaKey: MediaKey?
        get() = messageMediaDbo.media_key
    override val mediaType: MediaType
        get() = messageMediaDbo.media_type
    override val mediaToken: MediaToken
        get() = messageMediaDbo.media_token
    override val fileName: FileName?
        get() = messageMediaDbo.file_name
    override val chatId: ChatId
        get() = messageMediaDbo.chat_id
    override val messageId: MessageId
        get() = messageMediaDbo.id

    @Volatile
    @Suppress("PropertyName")
    var _localFile: File? = messageMediaDbo.local_file

    override val localFile: File?
        get() = try {
            _localFile?.let { file ->
                if (file.exists() && file.isFile) {
                    file
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }

    @Volatile
    @Suppress("PropertyName")
    var _mediaKeyDecrypted: MediaKeyDecrypted? = messageMediaDbo.media_key_decrypted
    override val mediaKeyDecrypted: MediaKeyDecrypted?
        get() = _mediaKeyDecrypted

    @Volatile
    @Suppress("PropertyName")
    var _mediaKeyDecryptionError: Boolean = false
    override val mediaKeyDecryptionError: Boolean
        get() = _mediaKeyDecryptionError

    @Volatile
    @Suppress("PropertyName")
    var _mediaKeyDecryptionException: Exception? = null
    override val mediaKeyDecryptionException: Exception?
        get() = _mediaKeyDecryptionException
}