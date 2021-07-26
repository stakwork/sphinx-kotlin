package chat.sphinx.splash.ui

import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.authentication_resources.databinding.LayoutAuthenticationBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.resources.inputMethodManager
import chat.sphinx.splash.R
import chat.sphinx.splash.databinding.FragmentSplashBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.R as R_screens
import io.matthewnelson.android_feature_screens.ui.motionlayout.MotionLayoutFragment
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.invisibleIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class SplashFragment: MotionLayoutFragment<
        Any,
        Context,
        SplashSideEffect,
        SplashViewState,
        SplashViewModel,
        FragmentSplashBinding
        >(R.layout.fragment_splash)
{

    override val binding: FragmentSplashBinding by viewBinding(FragmentSplashBinding::bind)
    private val bindingAuthentication: LayoutAuthenticationBinding by viewBinding(LayoutAuthenticationBinding::bind)
    override val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.screenInit()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        OnBackPress(binding.root.context).addCallback(viewLifecycleOwner, requireActivity())
    }

    private var doublePressBackJob: Job? = null
    private inner class OnBackPress(context: Context): CloseAppOnBackPress(context) {
        override fun handleOnBackPressed() {
            viewModel.currentViewState.let { viewState ->
                if (viewState is SplashViewState.Set3_DecryptKeys) {
                    if (viewState.inputLock && doublePressBackJob?.isActive != true) {
                        doublePressBackJob = lifecycleScope.launch(viewModel.mainImmediate) {
                            SphinxToastUtils().show(
                                binding.root.context,
                                R_screens.string.close_app_double_tap_toast_msg
                            )
                            delay(2_000)
                        }
                    } else {
                        viewState.pinWriter.reset()
                        viewState.inputLock = false
                        updateProgressBar(viewState)
                        updatePinHints(viewState)
                        setTransitionListener(binding.layoutMotionSplash)
                        viewState.transitionToEndSet(binding.layoutMotionSplash)
                    }
                } else {
                    super.handleOnBackPressed()
                }
            }
        }
    }

    ////////////////////
    /// Side Effects ///
    ////////////////////
    override suspend fun onSideEffectCollect(sideEffect: SplashSideEffect) {
        if (sideEffect is SplashSideEffect.FromScanner) {
            binding.layoutOnBoard.editTextCodeInput.setText(sideEffect.value.value)
            processConnectionCode(sideEffect.value.value)
        } else {
            sideEffect.execute(binding.root.context)
        }
    }

    /////////////////////
    /// Motion Layout ///
    /////////////////////
    override suspend fun onViewStateFlowCollect(viewState: SplashViewState) {
        @Exhaustive
        when (viewState) {
            is SplashViewState.HideLoadingWheel -> {
                binding.layoutOnBoard.signUpProgressBar.goneIfFalse(false)
            }
            is SplashViewState.Start_ShowIcon -> {
                binding.layoutOnBoard.imageButtonScanner.let { imageButton ->
                    imageButton.isEnabled = false
                    imageButton.setOnClickListener(null)
                }
                binding.layoutOnBoard.editTextCodeInput.let { editText ->
                    editText.isEnabled = false
                }
            }

            is SplashViewState.Transition_Set2_ShowWelcome -> {

                // TODO: temporary until InsetterActivity gets properly built out.
                (requireActivity() as InsetterActivity)
                    .addNavigationBarPadding(
                        bindingAuthentication.layoutPinPad.layoutConstraintPinPad
                    )

                setTransitionListener(binding.layoutMotionSplash)
                viewState.transitionToEndSet(binding.layoutMotionSplash)
            }

            is SplashViewState.Set2_ShowWelcome -> {

                binding.layoutOnBoard.editTextCodeInput.let { editText ->
                    editText.isEnabled = true

                    editText.setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            processConnectionCode(editText.text?.toString())
                            true
                        } else {
                            false
                        }
                    }
                }
                binding.layoutOnBoard.imageButtonScanner.let { imageButton ->
                    imageButton.isEnabled = true
                    imageButton.setOnClickListener {
                        viewModel.navigateToScanner()
                    }
                }
                bindingAuthentication.layoutPinPad.let { pinPad ->
                    pinPad.button0.setOnClickListener(null)
                    pinPad.button1.setOnClickListener(null)
                    pinPad.button2.setOnClickListener(null)
                    pinPad.button3.setOnClickListener(null)
                    pinPad.button4.setOnClickListener(null)
                    pinPad.button5.setOnClickListener(null)
                    pinPad.button6.setOnClickListener(null)
                    pinPad.button7.setOnClickListener(null)
                    pinPad.button8.setOnClickListener(null)
                    pinPad.button9.setOnClickListener(null)
                    pinPad.buttonBackspace.setOnClickListener(null)
                    pinPad.button0.isEnabled = false
                    pinPad.button1.isEnabled = false
                    pinPad.button2.isEnabled = false
                    pinPad.button3.isEnabled = false
                    pinPad.button4.isEnabled = false
                    pinPad.button5.isEnabled = false
                    pinPad.button6.isEnabled = false
                    pinPad.button7.isEnabled = false
                    pinPad.button8.isEnabled = false
                    pinPad.button9.isEnabled = false
                    pinPad.buttonBackspace.isEnabled = false
                }
            }

            is SplashViewState.Transition_Set3_DecryptKeys -> {
                binding.layoutOnBoard.editTextCodeInput.let { editText ->
                    editText.isEnabled = false
                    editText.setOnEditorActionListener(null)
                }
                binding.layoutOnBoard.imageButtonScanner.let { imageButton ->
                    imageButton.isEnabled = false
                    imageButton.setOnClickListener(null)
                }
                delay(500L) // keyboard close
                setTransitionListener(binding.layoutMotionSplash)
                viewState.transitionToEndSet(binding.layoutMotionSplash)
            }

            is SplashViewState.Set3_DecryptKeys -> {
                updatePinHints(viewState)
                updateProgressBar(viewState)

                bindingAuthentication.layoutPinPad.let { pinPad ->
                    pinPad.button0.isEnabled = true
                    pinPad.button1.isEnabled = true
                    pinPad.button2.isEnabled = true
                    pinPad.button3.isEnabled = true
                    pinPad.button4.isEnabled = true
                    pinPad.button5.isEnabled = true
                    pinPad.button6.isEnabled = true
                    pinPad.button7.isEnabled = true
                    pinPad.button8.isEnabled = true
                    pinPad.button9.isEnabled = true
                    pinPad.buttonBackspace.isEnabled = true
                    pinPad.button0.setOnClickListener { addCharacter(viewState, '0') }
                    pinPad.button1.setOnClickListener { addCharacter(viewState, '1') }
                    pinPad.button2.setOnClickListener { addCharacter(viewState, '2') }
                    pinPad.button3.setOnClickListener { addCharacter(viewState, '3') }
                    pinPad.button4.setOnClickListener { addCharacter(viewState, '4') }
                    pinPad.button5.setOnClickListener { addCharacter(viewState, '5') }
                    pinPad.button6.setOnClickListener { addCharacter(viewState, '6') }
                    pinPad.button7.setOnClickListener { addCharacter(viewState, '7') }
                    pinPad.button8.setOnClickListener { addCharacter(viewState, '8') }
                    pinPad.button9.setOnClickListener { addCharacter(viewState, '9') }
                    pinPad.buttonBackspace.setOnClickListener {
                        produceHapticFeedback()
                        if (viewState.pinWriter.size() > 0 && !viewState.inputLock) {
                            val before = viewState.pinWriter.toCharArray()
                            viewState.pinWriter.reset()

                            for ((index, c) in before.withIndex()) {
                                if (index != before.size - 1) {
                                    viewState.pinWriter.append(c)
                                }
                            }

                            before.fill('0')
                            updatePinHints(viewState)
                        }
                    }
                }
            }
        }
    }

    private fun processConnectionCode(code: String?) {
        binding.root.context.inputMethodManager?.let { imm ->
            binding.layoutOnBoard.apply {
                if (imm.isActive(editTextCodeInput)) {
                    imm.hideSoftInputFromWindow(editTextCodeInput.windowToken, 0)
                }
                signUpProgressBar.visible

                viewModel.processConnectionCode(code)
            }
        }
    }

    private fun addCharacter(viewState: SplashViewState.Set3_DecryptKeys, c: Char) {
        produceHapticFeedback()
        if (viewState.pinWriter.size() < 7 && !viewState.inputLock) {
            viewState.pinWriter.append(c)
            updatePinHints(viewState)
            if (viewState.pinWriter.size() == 6) {
                viewState.inputLock = true
                updateProgressBar(viewState)
                viewModel.decryptInput(viewState)
            }
        }
    }

    private fun produceHapticFeedback() {
        requireActivity().window.decorView.performHapticFeedback(
            HapticFeedbackConstants.LONG_PRESS,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    private fun updatePinHints(viewState: SplashViewState.Set3_DecryptKeys) {
        val pinSize = viewState.pinWriter.size()
        bindingAuthentication.layoutPinHint.let { pinHint ->
            pinHint.imageViewPinHintFilled1.invisibleIfFalse(pinSize > 0)
            pinHint.imageViewPinHintFilled2.invisibleIfFalse(pinSize > 1)
            pinHint.imageViewPinHintFilled3.invisibleIfFalse(pinSize > 2)
            pinHint.imageViewPinHintFilled4.invisibleIfFalse(pinSize > 3)
            pinHint.imageViewPinHintFilled5.invisibleIfFalse(pinSize > 4)
            pinHint.imageViewPinHintFilled6.invisibleIfFalse(pinSize > 5)
        }
    }

    private fun updateProgressBar(viewState: SplashViewState.Set3_DecryptKeys) {
        bindingAuthentication.progressBar.invisibleIfFalse(viewState.inputLock)
    }

    override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
        when (currentId) {
            SplashViewState.Transition_Set2_ShowWelcome.endSetId -> {
                removeTransitionListener(binding.layoutMotionSplash)
                viewModel.updateViewState(SplashViewState.Set2_ShowWelcome)
            }
            SplashViewState.Transition_Set3_DecryptKeys.END_SET_ID -> {
                removeTransitionListener(binding.layoutMotionSplash)
                viewModel.updateViewState(
                    SplashViewState.Set3_DecryptKeys(
                        try {
                            (viewModel.currentViewState as SplashViewState.Transition_Set3_DecryptKeys).restoreCode
                        } catch (e: ClassCastException) {
                            // "Should" never happen, but if it does, this is the only other
                            // view state that contains set3's resource ID.
                            (viewModel.currentViewState as SplashViewState.Set3_DecryptKeys).restoreCode
                        }
                    )
                )
            }
        }
    }

    override fun onViewCreatedRestoreMotionScene(viewState: SplashViewState, binding: FragmentSplashBinding) {
        viewState.restoreMotionScene(binding.layoutMotionSplash)
    }

    override fun getMotionLayouts(): Array<MotionLayout> {
        return arrayOf(binding.layoutMotionSplash)
    }
}
