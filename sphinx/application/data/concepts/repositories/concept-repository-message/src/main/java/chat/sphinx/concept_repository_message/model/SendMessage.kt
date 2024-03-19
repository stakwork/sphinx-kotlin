package chat.sphinx.concept_repository_message.model

import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_message.GiphyData
import chat.sphinx.wrapper_message.MessageType
import chat.sphinx.wrapper_message.PodcastClip
import chat.sphinx.wrapper_message.ReplyUUID
import chat.sphinx.wrapper_message.SenderAlias
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
    val groupAction: MessageType.GroupAction?,
    val paidMessagePrice: Sat?,
    val priceToMeet: Sat?,
    val threadUUID: ThreadUUID?,
    val memberPubKey: LightningNodePubKey?,
    val senderAlias: SenderAlias?
) {

    class Builder {
        private var chatId: ChatId?                       = null
        private var contactId: ContactId?                 = null
        private var tribePaymentAmount: Sat?              = null
        private var attachmentInfo: AttachmentInfo?       = null
        private var replyUUID: ReplyUUID?                 = null
        private var text: String?                         = null
        private var giphyData: GiphyData?                 = null
        private var podcastClip: PodcastClip?             = null
        private var isBoost: Boolean                      = false
        private var isCall: Boolean                       = false
        private var isTribePayment: Boolean               = false
        private var groupAction: MessageType.GroupAction? = null
        private var paidMessagePrice: Sat?                = null
        private var priceToMeet: Sat?                     = null
        private var threadUUID: ThreadUUID?               = null
        private var memberPubKey: LightningNodePubKey?    = null
        private var senderAlias: SenderAlias?             = null

        enum class ValidationError {
            EMPTY_PRICE, EMPTY_DESTINATION, EMPTY_CONTENT
        }

        @Synchronized
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
            groupAction = null
            paidMessagePrice = null
            priceToMeet = null
            threadUUID = null
            memberPubKey = null
            senderAlias = null
        }

        @Synchronized
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
                        !isTribePayment &&
                        groupAction == null
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
        fun setTribePaymentAmount(tribePaymentAmount: Sat?): Builder {
            this.tribePaymentAmount = tribePaymentAmount
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
        fun setPodcastClip(podcastClip: PodcastClip?): Builder {
            this.podcastClip = podcastClip
            return this
        }

        @Synchronized
        fun setIsBoost(isBoost: Boolean): Builder {
            this.isBoost = isBoost
            return this
        }

        @Synchronized
        fun setIsCall(isCall: Boolean): Builder {
            this.isCall = isCall
            return this
        }

        @Synchronized
        fun setIsTribePayment(isTribePayment: Boolean): Builder {
            this.isTribePayment = isTribePayment
            return this
        }

        @Synchronized
        fun setGroupAction(groupAction: MessageType.GroupAction): Builder {
            this.groupAction = groupAction
            return this
        }

        @Synchronized
        fun setPriceToMeet(priceToMeet: Sat?): Builder {
            this.priceToMeet = priceToMeet
            return this
        }

        @Synchronized
        fun setPaidMessagePrice(paidMessagePrice: Sat?): Builder {
            this.paidMessagePrice = paidMessagePrice
            return this
        }

        @Synchronized
        fun setThreadUUID(threadUUID: ThreadUUID?): Builder {
            this.threadUUID = threadUUID
            return this
        }

        @Synchronized
        fun setMemberPubKey(memberPubKey: LightningNodePubKey): Builder {
            this.memberPubKey = memberPubKey
            return this
        }

        @Synchronized
        fun setSenderAlias(senderAlias: SenderAlias): Builder {
            this.senderAlias = senderAlias
            return this
        }

        @Synchronized
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
                        groupAction,
                        paidMessagePrice,
                        priceToMeet,
                        threadUUID,
                        memberPubKey,
                        senderAlias
                    ), null
                )
            }
        }
    }
}

fun Long.formatAmount(): String {
    return """{"amt": $this}"""
}
