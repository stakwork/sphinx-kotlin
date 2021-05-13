package chat.sphinx.chat_common.ui.viewstate.messageholder

import androidx.core.view.updateLayoutParams
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.databinding.LayoutMessageHolderBinding
import chat.sphinx.wrapper_view.Px
import io.matthewnelson.android_feature_screens.util.goneIfFalse

sealed class HolderStatusHeader {

    companion object {
        const val SPACE_WIDTH_MULTIPLE: Float = 0.2F
    }

    abstract fun configureInHolder(holderWidth: Px, binding: LayoutMessageHolderBinding)

    object None : HolderStatusHeader() {
        override fun configureInHolder(holderWidth: Px, binding: LayoutMessageHolderBinding) {
            binding.includeMessageStatusHeader.root.goneIfFalse(false)
        }
    }

    sealed class In : HolderStatusHeader() {

        override fun configureInHolder(holderWidth: Px, binding: LayoutMessageHolderBinding) {
            binding.includeMessageStatusHeader.layoutConstraintSentMessageContentContainer.goneIfFalse(false)
            binding.includeMessageStatusHeader.layoutConstraintReceivedMessageContentContainer.goneIfFalse(true)
        }

        object First : In() {
            override fun configureInHolder(holderWidth: Px, binding: LayoutMessageHolderBinding) {
                binding.includeMessageStatusHeader.root.goneIfFalse(true)

                super.configureInHolder(holderWidth, binding)
            }
        }

        object Middle : In() {
            override fun configureInHolder(holderWidth: Px, binding: LayoutMessageHolderBinding) {
                // TODO: Hide header on all but the `FIRST` message in a group
//                binding.includeMessageStatusHeader.root.goneIfFalse(false)
                binding.includeMessageStatusHeader.root.goneIfFalse(true)


                super.configureInHolder(holderWidth, binding)
            }
        }

        object Last : In() {
            override fun configureInHolder(holderWidth: Px, binding: LayoutMessageHolderBinding) {
                // TODO: Hide header on all but the `FIRST` message in a group
//                binding.includeMessageStatusHeader.root.goneIfFalse(false)
                binding.includeMessageStatusHeader.root.goneIfFalse(true)

                super.configureInHolder(holderWidth, binding)
            }
        }
    }

    sealed class Out : HolderStatusHeader() {

        override fun configureInHolder(holderWidth: Px, binding: LayoutMessageHolderBinding) {
            binding.includeMessageStatusHeader.layoutConstraintSentMessageContentContainer.goneIfFalse(true)
            binding.includeMessageStatusHeader.layoutConstraintReceivedMessageContentContainer.goneIfFalse(false)
        }

        object First : Out() {
            override fun configureInHolder(holderWidth: Px, binding: LayoutMessageHolderBinding) {
                binding.includeMessageStatusHeader.root.goneIfFalse(true)

                super.configureInHolder(holderWidth, binding)
            }
        }

        object Middle : Out() {
            override fun configureInHolder(holderWidth: Px, binding: LayoutMessageHolderBinding) {
                // TODO: Hide header on all but the `FIRST` message in a group
//                binding.includeMessageStatusHeader.root.goneIfFalse(false)
                binding.includeMessageStatusHeader.root.goneIfFalse(true)

                super.configureInHolder(holderWidth, binding)
            }
        }

        object Last : Out() {
            override fun configureInHolder(holderWidth: Px, binding: LayoutMessageHolderBinding) {
                // TODO: Hide header on all but the `FIRST` message in a group
//                binding.includeMessageStatusHeader.root.goneIfFalse(false)
                binding.includeMessageStatusHeader.root.goneIfFalse(true)

                super.configureInHolder(holderWidth, binding)
            }
        }
    }
}
