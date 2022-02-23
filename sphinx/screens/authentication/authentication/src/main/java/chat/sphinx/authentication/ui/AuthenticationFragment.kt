package chat.sphinx.authentication.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.authentication.R
import chat.sphinx.authentication_resources.databinding.LayoutAuthenticationBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.resources.SphinxToastUtils
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.invisibleIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.feature_authentication_view.ui.AuthenticationViewModelContainer
import io.matthewnelson.feature_authentication_view.ui.AuthenticationViewState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class AuthenticationFragment: SideEffectFragment<
        FragmentActivity,
        AuthenticationSideEffect,
        AuthenticationViewState,
        AuthenticationViewModel,
        LayoutAuthenticationBinding
        >(R.layout.fragment_authentication)
{
    override val binding: LayoutAuthenticationBinding by viewBinding(LayoutAuthenticationBinding::bind)
    override val viewModel: AuthenticationViewModel by viewModels()
    private val viewModelContainer: AuthenticationViewModelContainer<NavController>
        get() = viewModel.authenticationViewModelContainer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BackPressHandler(binding.root.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())

        binding.layoutConstraintAuthentication.setOnClickListener { viewModel }

        viewModel.currentViewState.pinPadChars.let { chars ->
            binding.layoutPinPad.let { pinPad ->

                (requireActivity() as InsetterActivity)
                    .addNavigationBarPadding(pinPad.layoutConstraintPinPad)

                pinPad.button0.setOnClickListener { viewModelContainer.numPadPress(chars[0]) }
                pinPad.button1.setOnClickListener { viewModelContainer.numPadPress(chars[1]) }
                pinPad.button2.setOnClickListener { viewModelContainer.numPadPress(chars[2]) }
                pinPad.button3.setOnClickListener { viewModelContainer.numPadPress(chars[3]) }
                pinPad.button4.setOnClickListener { viewModelContainer.numPadPress(chars[4]) }
                pinPad.button5.setOnClickListener { viewModelContainer.numPadPress(chars[5]) }
                pinPad.button6.setOnClickListener { viewModelContainer.numPadPress(chars[6]) }
                pinPad.button7.setOnClickListener { viewModelContainer.numPadPress(chars[7]) }
                pinPad.button8.setOnClickListener { viewModelContainer.numPadPress(chars[8]) }
                pinPad.button9.setOnClickListener { viewModelContainer.numPadPress(chars[9]) }
                pinPad.buttonBackspace.setOnClickListener { viewModelContainer.backSpacePress() }

                pinPad.button0.text = chars[0].toString()
                pinPad.button1.text = chars[1].toString()
                pinPad.button2.text = chars[2].toString()
                pinPad.button3.text = chars[3].toString()
                pinPad.button4.text = chars[4].toString()
                pinPad.button5.text = chars[5].toString()
                pinPad.button6.text = chars[6].toString()
                pinPad.button7.text = chars[7].toString()
                pinPad.button8.text = chars[8].toString()
                pinPad.button9.text = chars[9].toString()
            }
        }
    }

    private inner class BackPressHandler(context: Context): CloseAppOnBackPress(context) {
        override fun handleOnBackPressed() {
            @Exhaustive
            when (this@AuthenticationFragment.viewModelContainer.handleDeviceBackPress()) {
                is AuthenticationViewModelContainer.HandleBackPressResponse.Minimize -> {
                    super.handleOnBackPressed()
                }
                is AuthenticationViewModelContainer.HandleBackPressResponse.DoNothing -> {}
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: AuthenticationSideEffect) {
        sideEffect.execute(requireActivity())
    }
    override suspend fun onViewStateFlowCollect(viewState: AuthenticationViewState) {
        binding.layoutPinHint.let { pinHint ->
            pinHint.imageViewPinHintFilled1.invisibleIfFalse(viewState.pinLength > 0)
            pinHint.imageViewPinHintFilled2.invisibleIfFalse(viewState.pinLength > 1)
            pinHint.imageViewPinHintFilled3.invisibleIfFalse(viewState.pinLength > 2)
            pinHint.imageViewPinHintFilled4.invisibleIfFalse(viewState.pinLength > 3)
            pinHint.imageViewPinHintFilled5.invisibleIfFalse(viewState.pinLength > 4)
            pinHint.imageViewPinHintFilled6.invisibleIfFalse(viewState.pinLength > 5)
        }

        binding.progressBar.invisibleIfFalse(viewState.inputLockState.show)

        when (viewState) {
            is AuthenticationViewState.ConfirmPin -> {
                R.string.header_confirm_pin
            }
            is AuthenticationViewState.Idle,
            is AuthenticationViewState.LogIn -> {
                R.string.header_enter_pin
            }
            is AuthenticationViewState.ResetPin.Step1 -> {
                R.string.header_reset_step_1
            }
            is AuthenticationViewState.ResetPin.Step2 -> {
                R.string.header_reset_step_2
            }
        }.let { headerTextId ->
            binding.textViewHeader.text = resources.getString(headerTextId)
        }

        // Check current view state, not viewState that is being collected here
        if (
            viewModel.currentViewState.confirmButtonShow &&
            !viewModel.currentViewState.inputLockState.show
        ) {
            viewModelContainer.confirmPress(produceHapticFeedback = false)
        }
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()
        // Ensures returning responses is only had when view is in foreground
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModelContainer.getAuthenticationFinishedStateFlow().collect { responses ->
                responses?.let { nnResponses ->
                    delay(100L)
                    viewModelContainer.completeAuthentication(nnResponses)
                }
            }
        }
    }
}
