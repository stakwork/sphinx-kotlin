package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.resources.R
import chat.sphinx.chat_common.databinding.LayoutMessageHolderBinding
import chat.sphinx.resources.setTextColorExt
import chat.sphinx.wrapper_chat.ChatType
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.isReceived
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.goneIfTrue

sealed class HolderStatusHeader {

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

            if (chatType?.value == ChatType.CONVERSATION) {
                includeMessageStatusHeader.textViewReceivedMessageSenderName.goneIfTrue(true)
            } else {
                includeMessageStatusHeader.textViewReceivedMessageSenderName.goneIfTrue(false)
                includeMessageStatusHeader.textViewReceivedMessageSenderName.text = message.senderAlias?.value ?: ""

                /**
                 *  TODO: Devise a way to derive random color values for sender aliases.
                 *
                 *  See the current iOS implementation: https://github.com/stakwork/sphinx/blob/9ee30302bc95091bcc9562e07ada87d52d27a5ad/sphinx/Scenes/Chat/Helpers/ChatHelper.swift#L12
                 */
                includeMessageStatusHeader.textViewReceivedMessageSenderName.setTextColorExt(R.color.lightPurple)
            }

            includeMessageStatusHeader.textViewReceivedMessageLockIcon.goneIfTrue(
                message.messageContentDecrypted == null
            )

            includeMessageStatusHeader.textViewReceivedMessageTimestamp.text = DateTime.getFormathmma().format(message.date)
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
            includeMessageStatusHeader.layoutConstraintReceivedMessageContentContainer.goneIfTrue(true)

            includeMessageStatusHeader.textViewSentMessageLockIcon.goneIfTrue(message.messageContentDecrypted == null)
            includeMessageStatusHeader.textViewSentMessageBoltIcon.goneIfFalse(message.status.isReceived())
            includeMessageStatusHeader.textViewSentMessageTimestamp.text = DateTime.getFormathmma().format(message.date)
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
