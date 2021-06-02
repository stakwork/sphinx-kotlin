package chat.sphinx.onboard.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.onboard.R
import chat.sphinx.onboard.databinding.FragmentOnBoardBinding
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.toAuthorizationToken
import chat.sphinx.wrapper_relay.toRelayUrl
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.updateViewState
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    companion object {
        const val SPHINX_TEMP_PREFS = "sphinx_temp_prefs"

        // Keys
        const val SPHINX_TEMP_INVITER_NICKNAME = "sphinx_temp_inviter_nickname"
        const val SPHINX_TEMP_INVITE_MESSAGE = "sphinx_temp_invite_message"
        const val SPHINX_TEMP_AUTH_TOKEN = "sphinx_temp_auth_token"
        const val SPHINX_TEMP_IP = "sphinx_temp_ip"
    }

    override val viewModel: OnBoardViewModel by viewModels()
    override val binding: FragmentOnBoardBinding by viewBinding(FragmentOnBoardBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeaderAndFooter()

        CloseAppOnBackPress(view.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())

        val prefs = binding.root.context.getSharedPreferences(SPHINX_TEMP_PREFS, Context.MODE_PRIVATE)

        lifecycleScope.launch(viewModel.io) {
            val nickname = prefs.getString(SPHINX_TEMP_INVITER_NICKNAME, "")
            val message = prefs.getString(SPHINX_TEMP_INVITE_MESSAGE, "")

            binding.inviterNameTextView.text = nickname
            binding.inviterMessageTextView.text = message
        }

        binding.buttonContinue.setOnClickListener {
            viewModel.updateViewState(OnBoardViewState.Saving)

            lifecycleScope.launch(viewModel.mainImmediate) {
                val authToken: AuthorizationToken? = withContext(viewModel.io) {
                    prefs.getString(SPHINX_TEMP_AUTH_TOKEN, null)?.toAuthorizationToken()
                }
                val ip: RelayUrl? = withContext(viewModel.io) {
                    prefs.getString(SPHINX_TEMP_IP, null)?.toRelayUrl()
                }

                if (authToken != null && ip != null) {
                    viewModel.presentLoginModal(authToken, ip)
                }
            }
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
