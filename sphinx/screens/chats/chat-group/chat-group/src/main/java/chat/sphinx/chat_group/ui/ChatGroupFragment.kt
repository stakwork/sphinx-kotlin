package chat.sphinx.chat_group.ui

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_common.BaseChatFragment
import chat.sphinx.chat_common.ChatViewState
import chat.sphinx.chat_group.R
import chat.sphinx.chat_group.databinding.FragmentChatGroupBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class ChatGroupFragment: BaseChatFragment<
        FragmentChatGroupBinding
        >(R.layout.fragment_chat_group)
{
    override val binding: FragmentChatGroupBinding by viewBinding(FragmentChatGroupBinding::bind)
    override val footer: ConstraintLayout
        get() = binding.layoutChatFooter.layoutConstraintChatFooter
    override val header: ConstraintLayout
        get() = binding.layoutChatHeader.layoutConstraintChatHeader

    override suspend fun onViewStateFlowCollect(viewState: ChatViewState) {
//        TODO("Not yet implemented")
    }
}
