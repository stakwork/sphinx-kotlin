package chat.sphinx.chat_contact.ui

import androidx.constraintlayout.widget.ConstraintLayout
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_common.BaseChatFragment
import chat.sphinx.chat_common.ChatViewState
import chat.sphinx.chat_contact.R
import chat.sphinx.chat_contact.databinding.FragmentChatContactBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class ChatContactFragment: BaseChatFragment<
        FragmentChatContactBinding
        >(R.layout.fragment_chat_contact)
{
    override val binding: FragmentChatContactBinding by viewBinding(FragmentChatContactBinding::bind)

    override val footer: ConstraintLayout
        get() = binding.layoutChatFooter.layoutConstraintChatFooter
    override val header: ConstraintLayout
        get() = binding.layoutChatHeader.layoutConstraintChatHeader

    override suspend fun onViewStateFlowCollect(viewState: ChatViewState) {
//        TODO("Not yet implemented")
    }
}
