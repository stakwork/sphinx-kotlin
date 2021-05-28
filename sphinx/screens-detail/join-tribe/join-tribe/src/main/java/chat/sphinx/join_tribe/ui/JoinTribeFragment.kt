package chat.sphinx.join_tribe.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.join_tribe.R
import chat.sphinx.join_tribe.databinding.FragmentJoinTribeBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class JoinTribeFragment: BaseFragment<
        JoinTribeViewState,
        JoinTribeViewModel,
        FragmentJoinTribeBinding
        >(R.layout.fragment_join_tribe)
{
    override val viewModel: JoinTribeViewModel by viewModels()
    override val binding: FragmentJoinTribeBinding by viewBinding(FragmentJoinTribeBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        binding.includeAddFriendHeader.apply {
//            textViewDetailScreenHeaderName.text = getString(R.string.add_friend_header_name)
//            textViewDetailScreenClose.setOnClickListener {
//                lifecycleScope.launch { viewModel.navigator.closeDetailScreen() }
//            }
//        }
//        binding.buttonNewToSphinx.setOnClickListener {
//            lifecycleScope.launch { viewModel.navigator.toCreateInvitationDetail() }
//        }
//        binding.buttonAlreadyOnSphinx.setOnClickListener {
//            lifecycleScope.launch { viewModel.navigator.toAddContactDetail() }
//        }
    }

    override suspend fun onViewStateFlowCollect(viewState: JoinTribeViewState) {
//        TODO("Not yet implemented")
    }
}
