package chat.sphinx.chat_tribe.ui

import androidx.constraintlayout.widget.ConstraintLayout
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_common.BaseChatFragment
import chat.sphinx.chat_common.ChatViewState
import chat.sphinx.chat_tribe.R
import chat.sphinx.chat_tribe.databinding.FragmentChatTribeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class ChatTribeFragment: BaseChatFragment<
        FragmentChatTribeBinding
        >(R.layout.fragment_chat_tribe)
{
    override val binding: FragmentChatTribeBinding by viewBinding(FragmentChatTribeBinding::bind)
    override val header: ConstraintLayout
        get() = binding.layoutChatHeader.layoutConstraintChatHeader
    override val footer: ConstraintLayout
        get() = binding.layoutChatFooter.layoutConstraintChatFooter

    override suspend fun onViewStateFlowCollect(viewState: ChatViewState) {
//        TODO("Not yet implemented")
    }
}
