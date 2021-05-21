package chat.sphinx.concept_repository_message

import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_message.ReplyUUID
import java.io.File

class SendMessage private constructor(
    val chatId: ChatId?,
    val contactId: ContactId?,
    val file: File?,
    val replyUUID: ReplyUUID?,
    val text: String?,
) {

    class Builder {
        private var chatId: ChatId?         = null
        private var contactId: ContactId?   = null
        private var file: File?             = null
        private var replyUUID: ReplyUUID?   = null
        private var text: String?           = null

        @Synchronized
        fun clear() {
            chatId = null
            contactId = null
            file = null
            replyUUID = null
            text = null
        }

        @get:Synchronized
        val isValid: Boolean
            get() = (
                        file?.let { if (!it.exists()) return false } != null    ||
                        !text.isNullOrEmpty()
                    )                                                           &&
                    (
                        chatId != null                                          ||
                        contactId != null
                    )

        @Synchronized
        fun setChatId(chatId: ChatId?): Builder {
            this.chatId = chatId
            return this
        }

        @Synchronized
        fun setContactId(contactId: ContactId?): Builder {
            this.contactId = contactId
            return this
        }

        @Synchronized
        fun setFile(file: File?): Builder {
            this.file = file
            return this
        }

        @Synchronized
        fun setReplyUUID(replyUUID: ReplyUUID?): Builder {
            this.replyUUID = replyUUID
            return this
        }

        @Synchronized
        fun setText(text: String?): Builder {
            this.text = text
            return this
        }

        @Synchronized
        fun build(): SendMessage? =
            if (!isValid) {
                null
            } else {
                SendMessage(
                    chatId,
                    contactId,
                    file,
                    replyUUID,
                    text,
                )
            }
    }
}
