package chat.sphinx.onboard.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.onboard.R
import chat.sphinx.onboard.databinding.FragmentOnBoardBinding
import chat.sphinx.onboard.navigation.authorizationToken
import chat.sphinx.onboard.navigation.inviterData
import chat.sphinx.onboard.navigation.relayUrl
import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.updateViewState
import javax.annotation.meta.Exhaustive

@AndroidEntryPoint
internal class OnBoardFragment: SideEffectFragment<
        Context,
        OnBoardSideEffect,
        OnBoardViewState,
        OnBoardViewModel,
        FragmentOnBoardBinding
        >(R.layout.fragment_on_board)
{
    private val args: OnBoardFragmentArgs by navArgs()
    private val relayUrl: RelayUrl by lazy(LazyThreadSafetyMode.NONE) { args.relayUrl }
    private val authorizationToken: AuthorizationToken by lazy(LazyThreadSafetyMode.NONE) { args.authorizationToken }
    private val inviterData: OnBoardInviterData by lazy(LazyThreadSafetyMode.NONE) { args.inviterData }

    override val viewModel: OnBoardViewModel by viewModels()
    override val binding: FragmentOnBoardBinding by viewBinding(FragmentOnBoardBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeaderAndFooter()

        CloseAppOnBackPress(view.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())

        binding.inviterNameTextView.text = inviterData.nickname ?: ""
        binding.inviterMessageTextView.text = inviterData.message ?: ""

        binding.buttonContinue.setOnClickListener {
            viewModel.updateViewState(OnBoardViewState.Saving)
            viewModel.presentLoginModal(authorizationToken, relayUrl, inviterData)
        }
    }

    private fun setupHeaderAndFooter() {
        (requireActivity() as InsetterActivity)
            .addStatusBarPadding(binding.layoutConstraintOnBoard)
            .addNavigationBarPadding(binding.layoutConstraintOnBoard)
    }

    override suspend fun onSideEffectCollect(sideEffect: OnBoardSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardViewState) {
        @Exhaustive
        when (viewState) {
            is OnBoardViewState.Idle -> {}
            is OnBoardViewState.Saving -> {
                binding.welcomeGetStartedProgress.visible
            }
            is OnBoardViewState.Error -> {
                binding.welcomeGetStartedProgress.gone
            }
        }
    }
}
