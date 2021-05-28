package chat.sphinx.onboard.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.onboard.R
import chat.sphinx.onboard.databinding.FragmentOnBoardBinding
import chat.sphinx.onboard.navigation.ToOnBoardScreen
import chat.sphinx.wrapper_relay.RelayUrl
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_authentication.coordinator.AuthenticationRequest
import io.matthewnelson.concept_authentication.coordinator.AuthenticationResponse
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.annotation.meta.Exhaustive
import kotlin.random.Random

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

        BackPressHandler(binding.root.context).addCallback(viewLifecycleOwner, requireActivity())

        arguments?.getString(ToOnBoardScreen.USER_INPUT)?.let { input ->
            input.split("::")?.let { decodedSplit ->
                //Token coming from Umbrel for example.
                if (decodedSplit.size == 3) {
                    val inviterNickname = "Sphinx Support"
                    val inviterPubKey = "023d70f2f76d283c6c4e58109ee3a2816eb9d8feb40b23d62469060a2b2867b77f"
                    val inviterMessage = "Welcome to Sphinx"

                    binding.includeWelcomeScreen.invitedToSphinxTextView.text = "A message from your friend.."
                    binding.includeWelcomeScreen.inviterNameTextView.text = inviterNickname
                    binding.includeWelcomeScreen.inviterMessageTextView.text = inviterMessage

                    storeTemporaryInviter(inviterNickname, inviterPubKey, inviterMessage)
                }
            }
        }

        binding.includeWelcomeScreen.buttonContinue.setOnClickListener {
            viewModel.updateViewState(OnBoardViewState.Saving)

            arguments?.getString(ToOnBoardScreen.USER_INPUT)?.let { input ->
                input.split("::")?.let { decodedSplit ->
                    //Token coming from Umbrel for example.
                    if (decodedSplit.size == 3) {
                        if (decodedSplit.elementAt(0) == "ip") {
                            val ip = decodedSplit.elementAt(1)
                            val code = decodedSplit.elementAt(2)

                            viewModel.generateToken(ip, code)
                        }
                    } else {
                        //TODO(gaune): INVITE CODE
                    }
                }
            }
        }
    }

    private fun storeTemporaryInviter(nickname: String, pubKey: String, message: String) {
//        withContext(viewModel.dispatchers.io) {
            context?.getSharedPreferences("sphinx_temp_prefs", Context.MODE_PRIVATE).let {
                    sharedPrefs ->
                sharedPrefs?.edit()?.let { editor ->
                    editor.putString("sphinx_temp_nickname", nickname)
                        .putString("sphinx_temp_pubkey", pubKey)
                        .putString("sphinx_temp_message", message)
                        .let { editor ->
                            if (!editor.commit()) {
                                editor.apply()
                            }
                        }
                }
            }
//        }
    }

    override suspend fun onSideEffectCollect(sideEffect: OnBoardSideEffect) {
        // TODO("Not yet implemented")
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardViewState) {
        @Exhaustive
        when (viewState) {
            is OnBoardViewState.Idle -> {}
            is OnBoardViewState.Saving -> {
                binding.includeWelcomeScreen.welcomeGetStartedProgress.visible
            }
            is OnBoardViewState.Error -> {
                binding.includeWelcomeScreen.welcomeGetStartedProgress.gone
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
