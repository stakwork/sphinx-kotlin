package chat.sphinx.concept_repository_message.model

import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_message.ReplyUUID
import java.io.File

class SendMessage private constructor(
    val attachmentInfo: AttachmentInfo?,
    val chatId: ChatId?,
    val contactId: ContactId?,
    val replyUUID: ReplyUUID?,
    val text: String?,
) {

    class Builder {
        private var chatId: ChatId?                 = null
        private var contactId: ContactId?           = null
        private var attachmentInfo: AttachmentInfo? = null
        private var replyUUID: ReplyUUID?           = null
        private var text: String?                   = null

        @Synchronized
        fun clear() {
            attachmentInfo = null
            chatId = null
            contactId = null
            replyUUID = null
            text = null
        }

        @get:Synchronized
        val isValid: Boolean
            get() = (
                        attachmentInfo?.file?.let {
                            try {
                                if (!it.exists() || !it.isFile) {
                                    return false
                                }

                                it
                            } catch (e: Exception) {
                                return false
                            }
                        }                                   != null     ||
                        !text.isNullOrEmpty()
                    )                                                   &&
                    (
                        chatId                              != null     ||
                        contactId                           != null
                    )

        @Synchronized
        fun setAttachmentInfo(attachmentInfo: AttachmentInfo?): Builder {
            this.attachmentInfo = attachmentInfo
            return this
        }

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
        fun setReplyUUID(replyUUID: ReplyUUID?): Builder {
            this.replyUUID = replyUUID
            return this
        }

        @Synchronized
        fun setText(text: String?): Builder {
            if (text == null || text.isEmpty()) {
                this.text = null
            } else {
                this.text = text
            }
            return this
        }

        @Synchronized
        fun build(): SendMessage? =
            if (!isValid) {
                null
            } else {
                SendMessage(
                    attachmentInfo,
                    chatId,
                    contactId,
                    replyUUID,
                    text,
                )
            }
    }
}
