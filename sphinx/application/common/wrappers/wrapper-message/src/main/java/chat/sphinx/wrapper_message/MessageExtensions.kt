package chat.sphinx.wrapper_message


val Message.isPaidMessage: Boolean
    get() {
        // TODO: Implement logic for properly detecting paid messages
        return (Math.random() * 5).toInt() < 2
    }

val Message.isAttachment: Boolean
    get() {
        return type.value == MessageType.ATTACHMENT
    }
