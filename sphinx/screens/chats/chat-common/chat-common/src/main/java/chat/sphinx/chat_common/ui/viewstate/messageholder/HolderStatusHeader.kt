package chat.sphinx.chat_common.ui.viewstate.messageholder

import androidx.core.view.updateLayoutParams
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.databinding.LayoutMessageHolderBinding
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_view.Px
import io.matthewnelson.android_feature_screens.util.goneIfFalse

sealed class HolderStatusHeader {

    companion object {
        const val SPACE_WIDTH_MULTIPLE: Float = 0.2F
    }

    abstract fun configureInHolder(
        message: Message,
        binding: LayoutMessageHolderBinding
    )

    object None : HolderStatusHeader() {
        override fun configureInHolder(message: Message, binding: LayoutMessageHolderBinding) {
            binding.includeMessageStatusHeader.root.goneIfFalse(false)
        }
    }

    sealed class In : HolderStatusHeader() {

        override fun configureInHolder(message: Message, binding: LayoutMessageHolderBinding) {
            val includeMessageStatusHeader = binding.includeMessageStatusHeader

            includeMessageStatusHeader.layoutConstraintSentMessageContentContainer.goneIfFalse(false)
            includeMessageStatusHeader.layoutConstraintReceivedMessageContentContainer.goneIfFalse(true)

            includeMessageStatusHeader.textViewReceivedMessageLockIcon.goneIfFalse(
                message.messageContentDecrypted != null
            )
        }

        object First : In() {
            override fun configureInHolder(message: Message, binding: LayoutMessageHolderBinding) {
                binding.includeMessageStatusHeader.root.goneIfFalse(true)

                super.configureInHolder(message, binding)
            }
        }

        object Middle : In() {
            override fun configureInHolder(message: Message, binding: LayoutMessageHolderBinding) {
                // TODO: Hide header on all but the `FIRST` message in a group
//                binding.includeMessageStatusHeader.root.goneIfFalse(false)
                binding.includeMessageStatusHeader.root.goneIfFalse(true)


                super.configureInHolder(message, binding)
            }
        }

        object Last : In() {
            override fun configureInHolder(message: Message, binding: LayoutMessageHolderBinding) {
                // TODO: Hide header on all but the `FIRST` message in a group
//                binding.includeMessageStatusHeader.root.goneIfFalse(false)
                binding.includeMessageStatusHeader.root.goneIfFalse(true)

                super.configureInHolder(message, binding)
            }
        }
    }

    sealed class Out : HolderStatusHeader() {

        override fun configureInHolder(message: Message, binding: LayoutMessageHolderBinding) {
            val includeMessageStatusHeader = binding.includeMessageStatusHeader

            includeMessageStatusHeader.layoutConstraintSentMessageContentContainer.goneIfFalse(true)
            includeMessageStatusHeader.layoutConstraintReceivedMessageContentContainer.goneIfFalse(false)

            includeMessageStatusHeader.textViewSentMessageLockIcon.goneIfFalse(
                message.messageContentDecrypted != null
            )
        }

        object First : Out() {
            override fun configureInHolder(message: Message, binding: LayoutMessageHolderBinding) {
                binding.includeMessageStatusHeader.root.goneIfFalse(true)

                super.configureInHolder(message, binding)
            }
        }

        object Middle : Out() {
            override fun configureInHolder(message: Message, binding: LayoutMessageHolderBinding) {
                // TODO: Hide header on all but the `FIRST` message in a group
//                binding.includeMessageStatusHeader.root.goneIfFalse(false)
                binding.includeMessageStatusHeader.root.goneIfFalse(true)

                super.configureInHolder(message, binding)
            }
        }

        object Last : Out() {
            override fun configureInHolder(message: Message, binding: LayoutMessageHolderBinding) {
                // TODO: Hide header on all but the `FIRST` message in a group
//                binding.includeMessageStatusHeader.root.goneIfFalse(false)
                binding.includeMessageStatusHeader.root.goneIfFalse(true)

                super.configureInHolder(message, binding)
            }
        }
    }
}
