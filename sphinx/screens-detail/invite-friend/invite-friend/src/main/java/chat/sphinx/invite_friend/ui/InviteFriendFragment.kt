package chat.sphinx.invite_friend.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.invite_friend.R
import chat.sphinx.invite_friend.databinding.FragmentInviteFriendBinding
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import chat.sphinx.wrapper_common.lightning.asFormattedString
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class InviteFriendFragment : SideEffectDetailFragment<
        Context,
        InviteFriendSideEffect,
        InviteFriendViewState,
        InviteFriendViewModel,
        FragmentInviteFriendBinding
        >(R.layout.fragment_invite_friend)
{

    override val viewModel: InviteFriendViewModel by viewModels()
    override val binding: FragmentInviteFriendBinding by viewBinding(FragmentInviteFriendBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.includeInviteFriendHeader.apply {

            textViewDetailScreenHeaderName.text = getString(R.string.invite_friend_header_name)

            textViewDetailScreenHeaderNavBack.apply navBack@ {
                this@navBack.visible
                this@navBack.setOnClickListener {
                    lifecycleScope.launch(viewModel.mainImmediate) {
                        viewModel.navigator.popBackStack()
                    }
                }
            }

            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }


        binding.buttonInviteFriendCreate.setOnClickListener {
            val nickname = binding.editTextInviteFriendNickname.text?.toString()
            val welcomeMessage = binding.editTextInviteFriendMessage.text?.toString()
            val sats = binding.editTextPriceForInvite.text?.toString()?.toLongOrNull()

            viewModel.createNewInvite(nickname, welcomeMessage, sats)

        }

        (requireActivity() as InsetterActivity).addNavigationBarPadding(binding.root)
    }

    override fun closeDetailsScreen() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.navigator.closeDetailScreen()
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: InviteFriendViewState) {
        @Exhaustive
        when (viewState) {
            is InviteFriendViewState.Idle -> {}

            is InviteFriendViewState.InviteFriendLowestPrice -> {
                binding.textViewInviteFriendEstimatedCostAmount.text = viewState.price.asFormattedString()

                binding.layoutConstraintInviteFriendEstimatedCost.apply {
                    alpha = 0.0F
                    visible
                    animate().alpha(1.0F)

                }
            }

            is InviteFriendViewState.InviteCreationLoading -> {
                binding.progressBarInviteFriendCreate.visible
            }
            is InviteFriendViewState.InviteCreationFailed -> {
                binding.progressBarInviteFriendCreate.gone
            }
            is InviteFriendViewState.InviteCreationSucceed -> {
                binding.progressBarInviteFriendCreate.gone

                viewModel.navigator.closeDetailScreen()
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: InviteFriendSideEffect) {
        sideEffect.execute(binding.root.context)
    }
}
