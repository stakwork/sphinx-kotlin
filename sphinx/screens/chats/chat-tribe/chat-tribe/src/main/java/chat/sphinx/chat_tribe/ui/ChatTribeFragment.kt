package chat.sphinx.chat_tribe.ui

import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_tribe.R
import chat.sphinx.chat_tribe.databinding.FragmentChatTribeBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class ChatTribeFragment: BaseFragment<
        ChatTribeViewState,
        ChatTribeViewModel,
        FragmentChatTribeBinding
        >(R.layout.fragment_chat_tribe)
{
    override val viewModel: ChatTribeViewModel by viewModels()
    override val binding: FragmentChatTribeBinding by viewBinding(FragmentChatTribeBinding::bind)

    override suspend fun onViewStateFlowCollect(viewState: ChatTribeViewState) {
//        TODO("Not yet implemented")
    }
}
