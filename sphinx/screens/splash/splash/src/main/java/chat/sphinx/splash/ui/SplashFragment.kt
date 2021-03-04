package chat.sphinx.splash.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.splash.R
import chat.sphinx.splash.databinding.FragmentSplashBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.motionlayout.MotionLayoutFragment
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive

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
    override val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.screenInit()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        OnBackPress(binding.root.context).addCallback(viewLifecycleOwner, requireActivity())
        lifecycleScope.launch { onViewStateFlowCollect(viewModel.currentViewState) }
    }

    private inner class OnBackPress(context: Context): CloseAppOnBackPress(context) {
        override fun handleOnBackPressed() {
            if (viewModel.currentViewState is SplashViewState.Set3_DecryptKeys) {
                setTransitionListener(binding.layoutMotionSplash)
                viewModel.currentViewState.transitionToEndSet(binding.layoutMotionSplash)
            } else {
                super.handleOnBackPressed()
            }
        }
    }

    ////////////////////
    /// Side Effects ///
    ////////////////////
    override suspend fun onSideEffectCollect(sideEffect: SplashSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    /////////////////////
    /// Motion Layout ///
    /////////////////////
    override suspend fun onViewStateFlowCollect(viewState: SplashViewState) {
        @Exhaustive
        when (viewState) {
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
                setTransitionListener(binding.layoutMotionSplash)
                viewState.transitionToEndSet(binding.layoutMotionSplash)
            }

            is SplashViewState.Set2_ShowWelcome -> {
                binding.layoutOnBoard.editTextCodeInput.let { editText ->
                    editText.isEnabled = true

                    (binding.root.context
                        .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    )?.let { imm ->
                        editText.requestFocus()
                        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
                    }

                    editText.setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            viewModel.processUserInput(
                                editText.text?.toString()
                            )
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
                // TODO: Setup authentication view
            }
        }
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
                            (viewModel.currentViewState as SplashViewState.Transition_Set3_DecryptKeys).toDecrypt
                        } catch (e: ClassCastException) {
                            // "Should" never happen, but if it does, this is the only other
                            // view state that contains set3's resource ID.
                            (viewModel.currentViewState as SplashViewState.Set3_DecryptKeys).toDecrypt
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
