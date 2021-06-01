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
import chat.sphinx.onboard.navigation.ToOnBoardScreen
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.crypto_common.extensions.decodeToString
import kotlinx.coroutines.launch
import okio.base64.decodeBase64ToArray
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
    override val viewModel: OnBoardViewModel by viewModels()
    override val binding: FragmentOnBoardBinding by viewBinding(FragmentOnBoardBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeaderAndFooter()

        BackPressHandler(binding.root.context).addCallback(viewLifecycleOwner, requireActivity())

        binding.invitedToSphinxTextView.text = "A message from your friend.."

        arguments?.getString(ToOnBoardScreen.USER_INPUT)?.let { input ->
            if (input.length == 40) {
                context?.getSharedPreferences("sphinx_temp_prefs", Context.MODE_PRIVATE)?.let { sharedPrefs ->
                    val nickname = sharedPrefs.getString("sphinx_temp_nickname", "")
                    val message = sharedPrefs.getString("sphinx_temp_message", "")

                    binding.inviterNameTextView.text = nickname
                    binding.inviterMessageTextView.text = message
                }
            } else {
                input.decodeBase64ToArray()?.decodeToString()?.split("::")?.let { decodedSplit ->
                    //Token coming from Umbrel for example.
                    if (decodedSplit.size == 3) {
                        // This is hardcoded in the iOS app as well, the most important part is the
                        // pubkey which later is being used to create the only contact this user
                        // will have.
                        val inviterNickname = "Sphinx Support"
                        val inviterPubKey = "023d70f2f76d283c6c4e58109ee3a2816eb9d8feb40b23d62469060a2b2867b77f"
                        val inviterMessage = "Welcome to Sphinx"

                        binding.inviterNameTextView.text = inviterNickname
                        binding.inviterMessageTextView.text = inviterMessage

                        viewModel.storeTemporaryInviter(inviterNickname, inviterPubKey, inviterMessage)
                    }
                }
            }
        }

        binding.buttonContinue.setOnClickListener {
            viewModel.updateViewState(OnBoardViewState.Saving)

            context?.getSharedPreferences("sphinx_temp_prefs", Context.MODE_PRIVATE)?.let { sharedPrefs ->
                val authToken = sharedPrefs.getString("sphinx_temp_auth_token", "")
                val ip = sharedPrefs.getString("sphinx_temp_ip", "")

                authToken?.let {
                    ip?.let {
                        viewModel.presentLoginModal(AuthorizationToken(authToken), RelayUrl(ip))
                    }
                }
            }
        }
    }

    private fun setupHeaderAndFooter() {
        val insetterActivity = (requireActivity() as InsetterActivity)
        insetterActivity.addStatusBarPadding(binding.layoutConstraintOnBoard)
        insetterActivity.addNavigationBarPadding(binding.layoutConstraintOnBoard)
    }

    override suspend fun onSideEffectCollect(sideEffect: OnBoardSideEffect) {
        // TODO("Not yet implemented")
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

    private inner class BackPressHandler(context: Context): CloseAppOnBackPress(context) {
        override fun handleOnBackPressed() {
            lifecycleScope.launch {
                viewModel.navigator.popBackStack()
            }
        }
    }
}
