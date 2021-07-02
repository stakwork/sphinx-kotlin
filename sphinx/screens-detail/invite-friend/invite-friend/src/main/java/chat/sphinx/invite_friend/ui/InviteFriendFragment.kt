package chat.sphinx.invite_friend.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.invite_friend.R
import chat.sphinx.invite_friend.databinding.FragmentInviteFriendBinding
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.unit
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class InviteFriendFragment : SideEffectFragment<
        Context,
        InviteFriendSideEffect,
        InviteFriendViewState,
        InviteFriendViewModel,
        FragmentInviteFriendBinding
        >(R.layout.fragment_invite_friend)
{

    override val viewModel: InviteFriendViewModel by viewModels()
    override val binding: FragmentInviteFriendBinding by viewBinding(FragmentInviteFriendBinding::bind)

    override suspend fun onViewStateFlowCollect(viewState: InviteFriendViewState) {
        @Exhaustive
        when (viewState) {
            is InviteFriendViewState.Idle -> {}

            is InviteFriendViewState.InviteFriendLowestPrice -> {
                binding.apply {
                    textViewEstimatedCostAmount.text = viewState.price.asFormattedString()

                    layoutConstraintEstimatedCost.alpha = 0.0f
                    layoutConstraintEstimatedCost.visible

                    layoutConstraintEstimatedCost.animate()
                        .alpha(1.0f)
                }
            }

            is InviteFriendViewState.InviteCreationLoading -> {
                binding.createInviteProgressBar.visible
            }
            is InviteFriendViewState.InviteCreationFailed -> {
                binding.createInviteProgressBar.gone
            }
            is InviteFriendViewState.InviteCreationSucceed -> {
                binding.createInviteProgressBar.gone

                viewModel.navigator.closeDetailScreen()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            includeInviteFriendHeader.apply {

                textViewDetailScreenHeaderName.text = getString(R.string.invite_friend_header_name)

                textViewDetailScreenHeaderNavBack.apply {
                    visible
                    setOnClickListener {
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

            buttonCreateInvitation.setOnClickListener {
                val nickname = editTextNicknameInput.text.toString()
                val welcomeMessage = editTextMessageInput.text.toString()

                viewModel.createNewInvite(nickname, welcomeMessage)

            }
        }

        (requireActivity() as InsetterActivity).addNavigationBarPadding(binding.layoutConstraintInviteFriend)
    }

    override suspend fun onSideEffectCollect(sideEffect: InviteFriendSideEffect) {
        sideEffect.execute(binding.root.context)
    }
}
