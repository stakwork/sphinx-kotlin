package chat.sphinx.wrapper_chat

sealed class ChatActionType {

    companion object {
        const val CAMERA = 0
        const val PHOTO_LIBRARY = 1
        const val GIF = 2
        const val FILE = 3
        const val PAID_MESSAGE = 4
        const val REQUEST_AMOUNT = 5
        const val SEND_AMOUNT = 6
        const val CANCEL = 6
    }

    abstract val value: Int

    object OpenCamera: ChatActionType() {
        override val value: Int
            get() = CAMERA
    }

    object OpenPhotoLibrary: ChatActionType() {
        override val value: Int
            get() = PHOTO_LIBRARY
    }

    object OpenGifSearch: ChatActionType() {
        override val value: Int
            get() = GIF
    }

    object OpenFileLibrary: ChatActionType() {
        override val value: Int
            get() = FILE
    }

    object OpenPaidMessageScreen: ChatActionType() {
        override val value: Int
            get() = PAID_MESSAGE
    }

    object RequestAmount: ChatActionType() {
        override val value: Int
            get() = REQUEST_AMOUNT
    }

    object SendPayment: ChatActionType() {
        override val value: Int
            get() = SEND_AMOUNT
    }

    object CancelAction: ChatActionType() {
        override val value: Int
            get() = CANCEL
    }

    data class Unknown(override val value: Int): ChatType()
}