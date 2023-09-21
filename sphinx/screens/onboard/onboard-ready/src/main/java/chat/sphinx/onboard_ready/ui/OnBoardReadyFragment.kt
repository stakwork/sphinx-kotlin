package chat.sphinx.onboard_ready.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.onboard_ready.R
import chat.sphinx.onboard_ready.databinding.FragmentOnBoardReadyBinding
import chat.sphinx.onboard_ready.navigation.inviterData
import chat.sphinx.resources.SphinxToastUtils
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive

@AndroidEntryPoint
internal class OnBoardReadyFragment: SideEffectFragment<
        Context,
        OnBoardReadySideEffect,
        OnBoardReadyViewState,
        OnBoardReadyViewModel,
        FragmentOnBoardReadyBinding
        >(R.layout.fragment_on_board_ready)
{
    private val args: OnBoardReadyFragmentArgs by navArgs()
    private val inviterData: OnBoardInviterData by lazy(LazyThreadSafetyMode.NONE) { args.inviterData }

    override val viewModel: OnBoardReadyViewModel by viewModels()
    override val binding: FragmentOnBoardReadyBinding by viewBinding(FragmentOnBoardReadyBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeaderAndFooter()

        CloseAppOnBackPress(view.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())

        binding.balanceTextView.text = getString(R.string.sphinx_ready_loading_balance)

        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.getBalances().collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    LoadResponse.Loading -> {}
                    is Response.Error -> {
                        viewModel.updateViewState(OnBoardReadyViewState.Error)
                        viewModel.submitSideEffect(OnBoardReadySideEffect.CreateInviterFailed)
                    }
                    is Response.Success -> {
                        val balance = loadResponse.value
                        binding.balanceTextView.text = getString(R.string.sphinx_ready_balance_label, balance.localBalance.value, balance.remoteBalance.value)
                    }
                }
            }
        }

        binding.buttonContinue.setOnClickListener {
            viewModel.updateViewState(OnBoardReadyViewState.Saving)

            val nickname = inviterData.nickname
            val pubkey = inviterData.pubkey
            val routeHint = inviterData.routeHint
            val inviteString = inviterData.pin

            if (nickname != null && pubkey != null) {
                viewModel.saveInviterAndFinish(nickname, pubkey.value, routeHint, inviteString)
            } else if (inviteString != null){
                viewModel.finishInvite(inviteString)
            } else {
                viewModel.finishSignup()
            }
        }
    }

    private fun setupHeaderAndFooter() {
        (requireActivity() as InsetterActivity)
            .addStatusBarPadding(binding.layoutConstraintOnBoardReady)
            .addNavigationBarPadding(binding.layoutConstraintOnBoardReady)
    }

    override suspend fun onSideEffectCollect(sideEffect: OnBoardReadySideEffect) {
        sideEffect.execute(binding.root.context)
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardReadyViewState) {
        @Exhaustive
        when (viewState) {
            is OnBoardReadyViewState.Idle -> {}
            is OnBoardReadyViewState.Saving -> {
                binding.onboardFinishProgress.visible
            }
            is OnBoardReadyViewState.Error -> {
                binding.onboardFinishProgress.gone
            }
        }
    }
}
