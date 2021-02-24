package chat.sphinx.splash.ui

import android.content.Context
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.splash.R
import chat.sphinx.splash.databinding.FragmentSplashBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.motionlayout.MotionLayoutFragment
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_views.sideeffect.SideEffect
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

    ////////////////////
    /// Side Effects ///
    ////////////////////
    override suspend fun onSideEffectCollect(sideEffect: SplashSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    //////////////////
    /// View State ///
    //////////////////
    override suspend fun onViewStateFlowCollect(viewState: SplashViewState) {
        @Exhaustive
        when (viewState) {
            is SplashViewState.Idle -> {
                binding.layoutOnBoard.editTextCodeInput.let { editText ->
                    editText.isEnabled = false
                }
                binding.layoutOnBoard.imageButtonScanner.let { imageButton ->
                    imageButton.isEnabled = false
                    imageButton.setOnClickListener(null)
                }
            }
            is SplashViewState.StartScene -> {
                binding.layoutOnBoard.editTextCodeInput.let { editText ->
                    editText.isEnabled = true
                }
                binding.layoutOnBoard.imageButtonScanner.let { imageButton ->
                    imageButton.isEnabled = true
                    imageButton.setOnClickListener {
                        viewModel.navigateToScanner()
                    }
                }
                setTransitionListener(binding.layoutMotionSplash)
                viewState.transitionToEndSet(binding.layoutMotionSplash)
            }
            is SplashViewState.SceneFinished -> {}
        }
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
}