package chat.sphinx.chat_contact.ui

import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_contact.R
import chat.sphinx.chat_contact.databinding.FragmentChatContactBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class ChatContactFragment: BaseFragment<
        ChatContactViewState,
        ChatContactViewModel,
        FragmentChatContactBinding
        >(R.layout.fragment_chat_contact)
{
    override val viewModel: ChatContactViewModel by viewModels()
    override val binding: FragmentChatContactBinding by viewBinding(FragmentChatContactBinding::bind)

    override suspend fun onViewStateFlowCollect(viewState: ChatContactViewState) {
//        TODO("Not yet implemented")
    }
}
