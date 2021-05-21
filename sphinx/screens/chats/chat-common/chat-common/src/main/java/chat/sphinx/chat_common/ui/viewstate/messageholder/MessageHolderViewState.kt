package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.wrapper_chat.ChatType
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.isDirectPayment
import chat.sphinx.wrapper_message.isReceived
import chat.sphinx.wrapper_message.*

internal sealed class MessageHolderViewState(
    val message: Message,
    chatType: ChatType?,
    val background: HolderBackground,
    val initialHolder: InitialHolderViewState
) {

    val statusHeader: LayoutState.MessageStatusHeader? by lazy(LazyThreadSafetyMode.NONE) {
        if (
            background is HolderBackground.First ||
            message.status.isDeleted()
        ) {
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

    val directPayment: LayoutState.DirectPayment? by lazy(LazyThreadSafetyMode.NONE) {
        when {
            message.status.isDeleted() -> {
                null
            }
            message.type.isDirectPayment() -> {
                LayoutState.DirectPayment(showSent = this is OutGoing, amount = message.amount)
            }
            else -> {
                null
            }
        }
    }

    val messageTypeMessageContent: LayoutState.MessageTypeMessageContent? by lazy(LazyThreadSafetyMode.NONE) {
        if (message.status.isDeleted()) {
            null
        } else {
            message.messageContentDecrypted?.let {
                // TODO: Handle podcast clips
                message.giphyData?.let { giphyData ->
                    // TODO: show only the giphyData.text when rendering logic is implemented
//                giphyData.text?.let { text ->
//                    LayoutState.MessageTypeMessageContent(text)
//                }
                    LayoutState.MessageTypeMessageContent(
                        messageContent = giphyData.toString(),
                    )
                } ?: /*if (message.podBoost == null) {*/ // TODO: Uncomment once boost layout logic is implemented
                LayoutState.MessageTypeMessageContent(
                    messageContent = it.value,
                )
//            } else {
//                null
//            }
            }
        }
    }

    val deletedMessage: LayoutState.DeletedMessage? by lazy(LazyThreadSafetyMode.NONE) {
        when {
            message.status.isDeleted() ->
                LayoutState.DeletedMessage(
                    isOutGoingMessage = this is OutGoing
                )
            else ->
                null
        }
    }


    class InComing(
        message: Message,
        chatType: ChatType?,
        background: HolderBackground,
        initialHolder: InitialHolderViewState,
    ) : MessageHolderViewState(
        message,
        chatType,
        background,
        initialHolder
    )

    class OutGoing(
        message: Message,
        chatType: ChatType?,
        background: HolderBackground,
    ) : MessageHolderViewState(
        message,
        chatType,
        background,
        InitialHolderViewState.None
    )
}
