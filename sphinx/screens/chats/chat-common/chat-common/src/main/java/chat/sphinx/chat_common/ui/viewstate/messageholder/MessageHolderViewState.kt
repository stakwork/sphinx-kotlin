package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.wrapper_chat.ChatType
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.isDirectPayment
import chat.sphinx.wrapper_message.isReceived

sealed class MessageHolderViewState {

    abstract val message: Message
    abstract val chatType: ChatType?
    abstract val background: HolderBackground
    abstract val initialHolder: InitialHolderViewState

    abstract val directPayment: LayoutState.DirectPayment?

    val messageTypeMessageContent: LayoutState.MessageTypeMessageContent? by lazy(LazyThreadSafetyMode.NONE) {
        message.messageContentDecrypted?.let {
            message.giphyData?.let { giphyData ->
                // TODO: show only the giphyData.text when rendering logic is implemented
//                giphyData.text?.let { text ->
//                    LayoutState.MessageTypeMessageContent(text)
//                }
                LayoutState.MessageTypeMessageContent(giphyData.toString())
            } ?: /*if (message.podBoost == null) {*/ // TODO: Uncomment once boost layout logic is implemented
                LayoutState.MessageTypeMessageContent(it.value)
//            } else {
//                null
//            }
        }
    }

    val statusHeader: LayoutState.MessageStatusHeader? by lazy(LazyThreadSafetyMode.NONE) {
        if (background is HolderBackground.First) {
            LayoutState.MessageStatusHeader(
                if (chatType?.isConversation() != false) null else message.senderAlias?.value,
                this is OutGoing,
                this is OutGoing && message.status.isReceived(),
                message.messageContentDecrypted != null,
                DateTime.getFormathmma().format(message.date.value),
            )
        } else {
            null
        }
    }


    class InComing(
        override val message: Message,
        override val chatType: ChatType?,
        override val background: HolderBackground,
        override val initialHolder: InitialHolderViewState,
    ) : MessageHolderViewState() {

        override val directPayment: LayoutState.DirectPayment? by lazy(LazyThreadSafetyMode.NONE) {
            if (message.type.isDirectPayment()) {
                LayoutState.DirectPayment(showSent = false, amount = message.amount)
            } else {
                null
            }
        }
    }

    class OutGoing(
        override val message: Message,
        override val chatType: ChatType?,
        override val background: HolderBackground,
    ) : MessageHolderViewState() {
        override val initialHolder: InitialHolderViewState
            get() = InitialHolderViewState.None

        override val directPayment: LayoutState.DirectPayment? by lazy(LazyThreadSafetyMode.NONE) {
            if (message.type.isDirectPayment()) {
                LayoutState.DirectPayment(showSent = true, amount = message.amount)
            } else {
                null
            }
        }
    }
}
