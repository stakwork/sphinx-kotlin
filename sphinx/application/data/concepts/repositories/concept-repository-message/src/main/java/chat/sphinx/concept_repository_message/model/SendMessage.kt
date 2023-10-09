package chat.sphinx.concept_repository_message.model

import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_message.GiphyData
import chat.sphinx.wrapper_message.PodcastClip
import chat.sphinx.wrapper_message.ReplyUUID
import chat.sphinx.wrapper_message.ThreadUUID
import chat.sphinx.wrapper_message_media.isSphinxText
import java.io.File

class SendMessage private constructor(
    val attachmentInfo: AttachmentInfo?,
    val chatId: ChatId?,
    val contactId: ContactId?,
    val tribePaymentAmount: Sat?,
    val replyUUID: ReplyUUID?,
    val text: String?,
    val giphyData: GiphyData?,
    val podcastClip: PodcastClip?,
    val isBoost: Boolean,
    val isCall: Boolean,
    val isTribePayment: Boolean,
    val paidMessagePrice: Sat?,
    val priceToMeet: Sat?,
    val threadUUID: ThreadUUID?
) {

    class Builder {
        private var chatId: ChatId?                 = null
        private var contactId: ContactId?           = null
        private var tribePaymentAmount: Sat?        = null
        private var attachmentInfo: AttachmentInfo? = null
        private var replyUUID: ReplyUUID?           = null
        private var text: String?                   = null
        private var giphyData: GiphyData?           = null
        private var podcastClip: PodcastClip?       = null
        private var isBoost: Boolean                = false
        private var isCall: Boolean                 = false
        private var isTribePayment: Boolean         = false
        private var paidMessagePrice: Sat?          = null
        private var priceToMeet: Sat?               = null
        private var threadUUID: ThreadUUID?         = null

        enum class ValidationError {
            EMPTY_PRICE, EMPTY_DESTINATION, EMPTY_CONTENT
        }

        fun clear() {
            attachmentInfo = null
            chatId = null
            contactId = null
            tribePaymentAmount = null
            replyUUID = null
            text = null
            giphyData = null
            podcastClip = null
            isBoost = false
            isTribePayment = false
            paidMessagePrice = null
            priceToMeet = null
            threadUUID = null
        }

        fun isValid(): Pair<Boolean, ValidationError?> {
            if (chatId == null && contactId == null) {
                return Pair(false, ValidationError.EMPTY_DESTINATION)
            }

            val file: File? = attachmentInfo?.file?.let {
                try {
                    if (it.exists() && it.isFile) {
                        it
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }

            when {
                (file == null) -> {
                    if (
                        text.isNullOrEmpty() &&
                        giphyData == null &&
                        podcastClip == null &&
                        !isTribePayment
                    ) {
                        return Pair(false, ValidationError.EMPTY_CONTENT)
                    }
                }
                else -> {
                    val isPaidTextMessage = attachmentInfo?.mediaType?.isSphinxText == true
                    val paidMessagePrice = paidMessagePrice?.value ?: 0

                    if (isPaidTextMessage && paidMessagePrice == 0.toLong()) {
                        return Pair(false, ValidationError.EMPTY_PRICE)
                    }
                }
            }

            return Pair(true, null)
        }

        fun setAttachmentInfo(attachmentInfo: AttachmentInfo?): Builder {
            this.attachmentInfo = attachmentInfo
            return this
        }

        fun setChatId(chatId: ChatId?): Builder {
            this.chatId = chatId
            return this
        }

        fun setContactId(contactId: ContactId?): Builder {
            this.contactId = contactId
            return this
        }

        fun setTribePaymentAmount(tribePaymentAmount: Sat?): Builder {
            this.tribePaymentAmount = tribePaymentAmount
            return this
        }

        fun setReplyUUID(replyUUID: ReplyUUID?): Builder {
            this.replyUUID = replyUUID
            return this
        }

        fun setText(text: String?): Builder {
            if (text == null || text.isEmpty()) {
                this.text = null
            } else {
                this.text = text
            }
            return this
        }

        fun setGiphyData(giphyData: GiphyData?): Builder {
            this.giphyData = giphyData
            return this
        }

        fun setPodcastClip(podcastClip: PodcastClip?): Builder {
            this.podcastClip = podcastClip
            return this
        }

        fun setIsBoost(isBoost: Boolean): Builder {
            this.isBoost = isBoost
            return this
        }

        fun setIsCall(isCall: Boolean): Builder {
            this.isCall = isCall
            return this
        }

        fun setIsTribePayment(isTribePayment: Boolean): Builder {
            this.isTribePayment = isTribePayment
            return this
        }

        fun setPriceToMeet(priceToMeet: Sat?): Builder {
            this.priceToMeet = priceToMeet
            return this
        }

        fun setPaidMessagePrice(paidMessagePrice: Sat?): Builder {
            this.paidMessagePrice = paidMessagePrice
            return this
        }

        fun setThreadUUID(threadUUID: ThreadUUID?): Builder {
            this.threadUUID = threadUUID
            return this
        }

        fun build(): Pair<SendMessage?, ValidationError?> {
            val isValid = isValid()

            if (!isValid.first) {
                return Pair(null, isValid.second)
            } else {
                return Pair(
                    SendMessage(
                        attachmentInfo,
                        chatId,
                        contactId,
                        tribePaymentAmount,
                        replyUUID,
                        text,
                        giphyData?.let { GiphyData(it.id, it.url, it.aspect_ratio, text) },
                        podcastClip?.let { PodcastClip(text, it.title, it.pubkey, it.url, it.feedID, it.itemID, it.ts) },
                        isBoost,
                        isCall,
                        isTribePayment,
                        paidMessagePrice,
                        priceToMeet,
                        threadUUID
                    ), null
                )
            }
        }
    }
}
