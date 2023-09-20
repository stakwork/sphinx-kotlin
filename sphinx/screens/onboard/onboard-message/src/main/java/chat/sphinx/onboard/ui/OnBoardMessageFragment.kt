package chat.sphinx.onboard.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_signer_manager.SignerManager
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.onboard.R
import chat.sphinx.onboard.databinding.FragmentOnBoardMessageBinding
import chat.sphinx.onboard.navigation.*
import chat.sphinx.onboard.navigation.authorizationToken
import chat.sphinx.onboard.navigation.inviterData
import chat.sphinx.onboard.navigation.relayUrl
import chat.sphinx.onboard.navigation.transportKey
import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayHMacKey
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_rsa.RsaPublicKey
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class OnBoardMessageFragment: SideEffectFragment<
        Context,
        OnBoardMessageSideEffect,
        OnBoardMessageViewState,
        OnBoardMessageViewModel,
        FragmentOnBoardMessageBinding
        >(R.layout.fragment_on_board_message)
{
    private val args: OnBoardMessageFragmentArgs by navArgs()

    private val relayUrl: RelayUrl by lazy(LazyThreadSafetyMode.NONE) { args.relayUrl }
    private val authorizationToken: AuthorizationToken by lazy(LazyThreadSafetyMode.NONE) { args.authorizationToken }
    private val transportKey: RsaPublicKey? by lazy(LazyThreadSafetyMode.NONE) { args.transportKey }
    private val hMacKey: RelayHMacKey? by lazy(LazyThreadSafetyMode.NONE) { args.hMacKey }
    private val inviterData: OnBoardInviterData by lazy(LazyThreadSafetyMode.NONE) { args.inviterData }

    override val viewModel: OnBoardMessageViewModel by viewModels()
    override val binding: FragmentOnBoardMessageBinding by viewBinding(FragmentOnBoardMessageBinding::bind)

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var signerManager: SignerManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeaderAndFooter()
        setupSignerManager()

        CloseAppOnBackPress(view.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())

        binding.inviterNameTextView.text = inviterData.nickname ?: ""
        binding.inviterMessageTextView.text = inviterData.message ?: ""

        binding.buttonContinue.setOnClickListener {
            viewModel.presentLoginModal(
                relayUrl,
                authorizationToken,
                transportKey,
                hMacKey,
                inviterData
            )
        }
    }

    private fun setupSignerManager(){
        viewModel.setSignerManager(signerManager)
    }

    private fun setupHeaderAndFooter() {
        (requireActivity() as InsetterActivity)
            .addStatusBarPadding(binding.layoutConstraintOnBoard)
            .addNavigationBarPadding(binding.layoutConstraintOnBoard)
    }

    override suspend fun onSideEffectCollect(sideEffect: OnBoardMessageSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardMessageViewState) {
        @Exhaustive
        when (viewState) {
            is OnBoardMessageViewState.Idle -> {
                binding.welcomeGetStartedProgress.gone
            }
            is OnBoardMessageViewState.Saving -> {
                binding.welcomeGetStartedProgress.visible
            }
            is OnBoardMessageViewState.Error -> {
                binding.welcomeGetStartedProgress.gone
            }
        }
    }
}
