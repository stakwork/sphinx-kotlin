package chat.sphinx.add_friend.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.add_friend.R
import chat.sphinx.add_friend.databinding.FragmentAddFriendBinding
import chat.sphinx.screen_detail_fragment.BaseDetailFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class AddFriendFragment: BaseDetailFragment<
        AddFriendViewState,
        AddFriendViewModel,
        FragmentAddFriendBinding
        >(R.layout.fragment_add_friend)
{
    override val viewModel: AddFriendViewModel by viewModels()
    override val binding: FragmentAddFriendBinding by viewBinding(FragmentAddFriendBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.includeAddFriendHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.add_friend_header_name)
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch { viewModel.navigator.closeDetailScreen() }
            }
        }
        binding.buttonNewToSphinx.setOnClickListener {
            lifecycleScope.launch { viewModel.navigator.toInviteFriendDetail() }
        }
        binding.buttonAlreadyOnSphinx.setOnClickListener {
            lifecycleScope.launch { viewModel.navigator.toAddContactDetail() }
        }
    }

    override fun closeDetailsScreen() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.navigator.closeDetailScreen()
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: AddFriendViewState) {
//        TODO("Not yet implemented")
    }
}
