package chat.sphinx.splash.ui

import android.content.Context
import android.os.Bundle
import android.view.View
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
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        OnBackPress(binding.root.context).addCallback(viewLifecycleOwner, requireActivity())
        lifecycleScope.launch { onViewStateFlowCollect(viewModel.currentViewState) }
    }

    ////////////////////
    /// Side Effects ///
    ////////////////////
    override suspend fun onSideEffectCollect(sideEffect: SplashSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    //////////////////
    /// View State ///
    //////////////////
    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()
        lifecycleScope.launchWhenStarted {
            viewModel.layoutViewStateContainer.collect { viewState ->
                onOnBoardLayoutViewStateCollect(viewState)
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: SplashViewState) {
        @Exhaustive
        when (viewState) {
            is SplashViewState.Idle -> {}
            is SplashViewState.StartScene -> {
                setTransitionListener(binding.layoutMotionSplash)
                viewState.transitionToEndSet(binding.layoutMotionSplash)
            }
            is SplashViewState.SceneFinished -> {
                viewModel.layoutViewStateContainer.value.let { layoutViewState ->
                    @Exhaustive
                    when (layoutViewState) {
                        is OnBoardLayoutViewState.Decrypt -> {
                            onOnBoardLayoutViewStateCollect(layoutViewState)
                        }
                        is OnBoardLayoutViewState.Hidden -> {
                            viewModel.layoutViewStateContainer.updateViewState(
                                OnBoardLayoutViewState.InputCode
                            )
                        }
                        is OnBoardLayoutViewState.InputCode -> {
                            onOnBoardLayoutViewStateCollect(layoutViewState)
                        }
                    }
                }
            }
        }
    }

    private suspend fun onOnBoardLayoutViewStateCollect(viewState: OnBoardLayoutViewState) {
        viewState.setInfoText(binding.layoutOnBoard.textViewWelcomeInfo)
        viewState.setScannerButton(viewModel, binding.layoutOnBoard.imageButtonScanner)
        viewState.setEditTextInput(viewModel, binding.layoutOnBoard.editTextCodeInput)
    }

    override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
        if (currentId == SplashViewState.StartScene.endSetId) {
            removeTransitionListener(binding.layoutMotionSplash)
            viewModel.updateViewState(SplashViewState.SceneFinished)
        }
    }

    override fun onViewCreatedRestoreMotionScene(viewState: SplashViewState, binding: FragmentSplashBinding) {
        viewModel.screenInit()
        viewState.restoreMotionScene(binding.layoutMotionSplash)
    }

    override fun getMotionLayouts(): Array<MotionLayout> {
        return arrayOf(binding.layoutMotionSplash)
    }

    private inner class OnBackPress(context: Context): CloseAppOnBackPress(context) {
        override fun handleOnBackPressed() {
            if (viewModel.layoutViewStateContainer.value is OnBoardLayoutViewState.Decrypt) {
                binding.layoutOnBoard.editTextCodeInput.setText("")
                viewModel.layoutViewStateContainer.updateViewState(OnBoardLayoutViewState.InputCode)
            } else {
                super.handleOnBackPressed()
            }
        }
    }
}