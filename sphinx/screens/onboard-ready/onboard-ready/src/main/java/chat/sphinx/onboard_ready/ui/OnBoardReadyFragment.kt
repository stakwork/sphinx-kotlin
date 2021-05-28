package chat.sphinx.onboard_ready.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.onboard_ready.R
import chat.sphinx.onboard_ready.databinding.FragmentOnBoardReadyBinding
import chat.sphinx.wrapper_common.lightning.asFormattedString
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
    override val viewModel: OnBoardReadyViewModel by viewModels()
    override val binding: FragmentOnBoardReadyBinding by viewBinding(FragmentOnBoardReadyBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BackPressHandler(binding.root.context).addCallback(viewLifecycleOwner, requireActivity())

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.getBalances().collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    LoadResponse.Loading ->
                        viewModel.updateViewState(OnBoardReadyViewState.Saving)
                    is Response.Error -> {
                        viewModel.updateViewState(OnBoardReadyViewState.Error)
                        viewModel.submitSideEffect(OnBoardReadySideEffect.CreateInviterFailed)
                    }
                    is Response.Success -> {
                        val balance = loadResponse.value

                        binding.balanceTextView.text = "You can send messages,\\nspend ${balance.localBalance} sats, or receive\\up to ${balance.remoteBalance} sats."
                    }
                }
            }
        }

        binding.buttonContinue.setOnClickListener {
            viewModel.updateViewState(OnBoardReadyViewState.Saving)

            context?.getSharedPreferences("sphinx_temp_prefs", Context.MODE_PRIVATE)?.let { sharedPrefs ->
                val nickname = sharedPrefs.getString("sphinx_temp_nickname", "")
                val pubkey = sharedPrefs.getString("sphinx_temp_pubkey", "")

                nickname?.let { nickname ->
                    pubkey?.let { pubkey ->
                        viewModel.saveInviterAndFinish(nickname, pubkey)
                    }
                }
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: OnBoardReadySideEffect) {
        // TODO("Not yet implemented")
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

    private inner class BackPressHandler(context: Context): CloseAppOnBackPress(context) {
        override fun handleOnBackPressed() {
            return
        }
    }
}
