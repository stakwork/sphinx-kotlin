package chat.sphinx.concept_repository_message.model

import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_message.GiphyData
import chat.sphinx.wrapper_message.ReplyUUID

class SendMessage private constructor(
    val attachmentInfo: AttachmentInfo?,
    val chatId: ChatId?,
    val contactId: ContactId?,
    val replyUUID: ReplyUUID?,
    val text: String?,
    val giphyData: GiphyData?,
    val isBoost: Boolean,
    val priceToMeet: Sat?
) {

    class Builder {
        private var chatId: ChatId?                 = null
        private var contactId: ContactId?           = null
        private var attachmentInfo: AttachmentInfo? = null
        private var replyUUID: ReplyUUID?           = null
        private var text: String?                   = null
        private var giphyData: GiphyData?           = null
        private var isBoost: Boolean                = false
        private var priceToMeet: Sat?               = null

        @Synchronized
        fun clear() {
            attachmentInfo = null
            chatId = null
            contactId = null
            replyUUID = null
            text = null
            giphyData = null
            isBoost = false
            priceToMeet = null
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
                        !text.isNullOrEmpty()                           ||
                        giphyData != null
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
        fun setGiphyData(giphyData: GiphyData?): Builder {
            this.giphyData = giphyData
            return this
        }

        @Synchronized
        fun setIsBoost(isBoost: Boolean): Builder {
            this.isBoost = isBoost
            return this
        }

        @Synchronized
        fun setPriceToMeet(priceToMeet: Sat?): Builder {
            this.priceToMeet = priceToMeet
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
                    giphyData?.let { GiphyData(it.id, it.url, it.aspect_ratio, text) },
                    isBoost,
                    priceToMeet
                )
            }
    }
}
