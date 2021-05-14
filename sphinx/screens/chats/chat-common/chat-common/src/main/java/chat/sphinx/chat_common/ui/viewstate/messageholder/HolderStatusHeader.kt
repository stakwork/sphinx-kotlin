package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.chat_common.databinding.LayoutMessageHolderBinding
import chat.sphinx.wrapper_chat.ChatType
import chat.sphinx.wrapper_common.Seen
import chat.sphinx.wrapper_message.Message
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.goneIfTrue

sealed class HolderStatusHeader {

    companion object {
        const val SPACE_WIDTH_MULTIPLE: Float = 0.2F
    }

    abstract fun configureInHolder(
        message: Message,
        chatType: ChatType?,
        binding: LayoutMessageHolderBinding
    )

    object None : HolderStatusHeader() {
        override fun configureInHolder(message: Message, chatType: ChatType?, binding: LayoutMessageHolderBinding) {
            binding.includeMessageStatusHeader.root.goneIfFalse(false)
        }
    }

    sealed class In : HolderStatusHeader() {

        override fun configureInHolder(message: Message, chatType: ChatType?, binding: LayoutMessageHolderBinding) {
            val includeMessageStatusHeader = binding.includeMessageStatusHeader

            includeMessageStatusHeader.layoutConstraintSentMessageContentContainer.goneIfFalse(false)
            includeMessageStatusHeader.layoutConstraintReceivedMessageContentContainer.goneIfFalse(true)

            includeMessageStatusHeader.textViewReceivedMessageLockIcon.goneIfFalse(
                message.messageContentDecrypted != null
            )

            includeMessageStatusHeader.textViewReceivedMessageSenderName.text = message.senderAlias?.value ?: ""

            includeMessageStatusHeader.textViewReceivedMessageSenderName.goneIfTrue(
                chatType?.value == ChatType.CONVERSATION
            )
        }

        object First : In() {
            override fun configureInHolder(message: Message, chatType: ChatType?, binding: LayoutMessageHolderBinding) {
                val includeMessageStatusHeader = binding.includeMessageStatusHeader

                includeMessageStatusHeader.root.goneIfFalse(true)

                super.configureInHolder(message, chatType, binding)
            }
        }

        object Middle : In() {
            override fun configureInHolder(message: Message, chatType: ChatType?, binding: LayoutMessageHolderBinding) {
                val includeMessageStatusHeader = binding.includeMessageStatusHeader

                // TODO: Hide header on all but the `FIRST` message in a group
//                includeMessageStatusHeader.root.goneIfFalse(false)
                includeMessageStatusHeader.root.goneIfFalse(true)

                super.configureInHolder(message, chatType, binding)
            }
        }

        object Last : In() {
            override fun configureInHolder(message: Message, chatType: ChatType?, binding: LayoutMessageHolderBinding) {
                val includeMessageStatusHeader = binding.includeMessageStatusHeader

                // TODO: Hide header on all but the `FIRST` message in a group
//                includeMessageStatusHeader.root.goneIfFalse(false)
                includeMessageStatusHeader.root.goneIfFalse(true)

                super.configureInHolder(message, chatType, binding)
            }
        }
    }

    sealed class Out : HolderStatusHeader() {

        override fun configureInHolder(message: Message, chatType: ChatType?, binding: LayoutMessageHolderBinding) {
            val includeMessageStatusHeader = binding.includeMessageStatusHeader

            includeMessageStatusHeader.layoutConstraintSentMessageContentContainer.goneIfFalse(true)
            includeMessageStatusHeader.layoutConstraintReceivedMessageContentContainer.goneIfFalse(false)

            includeMessageStatusHeader.textViewSentMessageLockIcon.goneIfFalse(
                message.messageContentDecrypted != null
            )

            includeMessageStatusHeader.textViewSentMessageBoltIcon.goneIfFalse(
                message.seen.equals(Seen.SEEN)
            )
        }

        object First : Out() {
            override fun configureInHolder(message: Message, chatType: ChatType?, binding: LayoutMessageHolderBinding) {
                binding.includeMessageStatusHeader.root.goneIfFalse(true)

                super.configureInHolder(message, chatType, binding)
            }
        }

        object Middle : Out() {
            override fun configureInHolder(message: Message, chatType: ChatType?, binding: LayoutMessageHolderBinding) {
                // TODO: Hide header on all but the `FIRST` message in a group
//                binding.includeMessageStatusHeader.root.goneIfFalse(false)
                binding.includeMessageStatusHeader.root.goneIfFalse(true)

                super.configureInHolder(message, chatType, binding)
            }
        }

        object Last : Out() {
            override fun configureInHolder(message: Message, chatType: ChatType?, binding: LayoutMessageHolderBinding) {
                // TODO: Hide header on all but the `FIRST` message in a group
//                binding.includeMessageStatusHeader.root.goneIfFalse(false)
                binding.includeMessageStatusHeader.root.goneIfFalse(true)

                super.configureInHolder(message, chatType, binding)
            }
        }
    }
}
