package chat.sphinx.wrapper_message_media

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
}
