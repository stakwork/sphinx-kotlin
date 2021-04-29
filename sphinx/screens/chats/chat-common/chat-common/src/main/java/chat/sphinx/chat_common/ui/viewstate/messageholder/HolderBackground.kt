package chat.sphinx.chat_common.ui.viewstate.messageholder

import androidx.core.view.updateLayoutParams
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.databinding.LayoutMessageHolderBinding
import chat.sphinx.wrapper_view.Px
import io.matthewnelson.android_feature_screens.util.goneIfFalse

sealed class HolderBackground {

    companion object {
        const val SPACE_WIDTH_MULTIPLE: Float = 0.2F
    }

    abstract fun setBackground(holderWidth: Px, binding: LayoutMessageHolderBinding)

    object None: HolderBackground() {
        override fun setBackground(holderWidth: Px, binding: LayoutMessageHolderBinding) {
            binding.includeMessageHolderMessageTypes.root.goneIfFalse(false)
        }
    }

    sealed class In: HolderBackground() {

        override fun setBackground(holderWidth: Px, binding: LayoutMessageHolderBinding) {
            binding.includeMessageHolderMessageTypes.root.goneIfFalse(true)
            binding.spaceMessageHolderLeft.updateLayoutParams {
                width = binding.root.context.resources.getDimensionPixelSize(R.dimen.message_holder_space_width_left)
            }
            binding.spaceMessageHolderRight.updateLayoutParams {
                width = (holderWidth.value * SPACE_WIDTH_MULTIPLE).toInt()
            }
        }

        object First: In() {

            override fun setBackground(holderWidth: Px, binding: LayoutMessageHolderBinding) {
                binding.includeMessageHolderMessageTypes.root
                    .setBackgroundResource(R.drawable.background_message_holder_in_first)

                super.setBackground(holderWidth, binding)
            }
        }

        object Middle: In() {
            override fun setBackground(holderWidth: Px, binding: LayoutMessageHolderBinding) {
                binding.includeMessageHolderMessageTypes.root
                    .setBackgroundResource(R.drawable.background_message_holder_in_middle)

                super.setBackground(holderWidth, binding)
            }
        }

        object Last: In() {
            override fun setBackground(holderWidth: Px, binding: LayoutMessageHolderBinding) {
                binding.includeMessageHolderMessageTypes.root
                    .setBackgroundResource(R.drawable.background_message_holder_in_last)

                super.setBackground(holderWidth, binding)
            }
        }
    }

    sealed class Out: HolderBackground() {

        override fun setBackground(holderWidth: Px, binding: LayoutMessageHolderBinding) {
            binding.includeMessageHolderMessageTypes.root.goneIfFalse(true)
            binding.spaceMessageHolderLeft.updateLayoutParams {
                width = (holderWidth.value * SPACE_WIDTH_MULTIPLE).toInt()
            }
            binding.spaceMessageHolderRight.updateLayoutParams {
                width = 0
            }
        }

        object First: Out() {
            override fun setBackground(holderWidth: Px, binding: LayoutMessageHolderBinding) {
                binding.includeMessageHolderMessageTypes.root
                    .setBackgroundResource(R.drawable.background_message_holder_out_first)

                super.setBackground(holderWidth, binding)
            }
        }

        object Middle: Out() {
            override fun setBackground(holderWidth: Px, binding: LayoutMessageHolderBinding) {
                binding.includeMessageHolderMessageTypes.root
                    .setBackgroundResource(R.drawable.background_message_holder_out_middle)
                super.setBackground(holderWidth, binding)
            }
        }

        object Last: Out() {
            override fun setBackground(holderWidth: Px, binding: LayoutMessageHolderBinding) {
                binding.includeMessageHolderMessageTypes.root
                    .setBackgroundResource(R.drawable.background_message_holder_out_last)

                super.setBackground(holderWidth, binding)
            }
        }
    }
}
