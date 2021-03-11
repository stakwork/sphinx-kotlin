package chat.sphinx.add_friend.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.add_friend.R
import chat.sphinx.add_friend.databinding.FragmentAddFriendBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class AddFriendFragment: BaseFragment<
        AddFriendViewState,
        AddFriendViewModel,
        FragmentAddFriendBinding
        >(R.layout.fragment_add_friend)
{
    override val viewModel: AddFriendViewModel by viewModels()
    override val binding: FragmentAddFriendBinding by viewBinding(FragmentAddFriendBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonNewToSphinx.setOnClickListener {
            lifecycleScope.launch { viewModel.navigator.toCreateInvitationDetail() }
        }
        binding.buttonAlreadyOnSphinx.setOnClickListener {
            lifecycleScope.launch { viewModel.navigator.toAddContactDetail() }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: AddFriendViewState) {
//        TODO("Not yet implemented")
    }
}
