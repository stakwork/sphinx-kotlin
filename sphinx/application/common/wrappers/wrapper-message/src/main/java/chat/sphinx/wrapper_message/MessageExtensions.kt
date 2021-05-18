package chat.sphinx.wrapper_message


val Message.isPaidMessage: Boolean
    get() {
        return type.isPayment()
    }

val Message.isAttachment: Boolean
    get() {
        return type.value == MessageType.ATTACHMENT
    }
