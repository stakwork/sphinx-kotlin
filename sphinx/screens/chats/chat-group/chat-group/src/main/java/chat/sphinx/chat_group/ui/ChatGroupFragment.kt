package chat.sphinx.chat_group.ui

import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_group.R
import chat.sphinx.chat_group.databinding.FragmentChatGroupBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class ChatGroupFragment: BaseFragment<
        ChatGroupViewState,
        ChatGroupViewModel,
        FragmentChatGroupBinding
        >(R.layout.fragment_chat_group)
{
    override val viewModel: ChatGroupViewModel by viewModels()
    override val binding: FragmentChatGroupBinding by viewBinding(FragmentChatGroupBinding::bind)

    override suspend fun onViewStateFlowCollect(viewState: ChatGroupViewState) {
//        TODO("Not yet implemented")
    }
}
