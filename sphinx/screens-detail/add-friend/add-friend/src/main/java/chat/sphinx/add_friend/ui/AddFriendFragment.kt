package chat.sphinx.add_friend.ui

import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.add_friend.R
import chat.sphinx.add_friend.databinding.FragmentAddFriendBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class AddFriendFragment: BaseFragment<
        AddFriendViewState,
        AddFriendViewModel,
        FragmentAddFriendBinding
        >(R.layout.fragment_add_friend)
{
    override val viewModel: AddFriendViewModel by viewModels()
    override val binding: FragmentAddFriendBinding by viewBinding(FragmentAddFriendBinding::bind)

    override suspend fun onViewStateFlowCollect(viewState: AddFriendViewState) {
//        TODO("Not yet implemented")
    }
}
