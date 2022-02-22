package chat.sphinx.keyboard_inset_fragment

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.viewbinding.ViewBinding
import io.matthewnelson.android_concept_views.MotionLayoutViewState
import io.matthewnelson.android_feature_viewmodel.MotionLayoutViewModel
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.concept_views.sideeffect.SideEffect

abstract class KeyboardInsetMotionLayoutFragment<
        MSC: Any,
        T,
        SE: SideEffect<T>,
        MLVS: MotionLayoutViewState<MLVS>,
        MLVM: MotionLayoutViewModel<MSC, T, SE, MLVS>,
        VB: ViewBinding
        >(@LayoutRes layoutId: Int): KeyboardInsetSideEffectFragment<
        T,
        SE,
        MLVS,
        MLVM,
        VB
        >(layoutId), MotionLayout.TransitionListener
{
    override fun onTransitionTrigger(motionLayout: MotionLayout?, triggerId: Int, positive: Boolean, progress: Float) {}
    override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {}
    override fun onTransitionChange(motionLayout: MotionLayout?, startId: Int, endId: Int, progress: Float) {}
    override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {}

    protected open fun setTransitionListener(motionLayout: MotionLayout) {
        motionLayout.setTransitionListener(this)
    }
    protected open fun removeTransitionListener(motionLayout: MotionLayout) {
        motionLayout.removeTransitionListener(this)
    }

    /**
     * Call [MotionLayoutViewState.restoreMotionScene] for the current [viewState].
     * */
    protected abstract fun onViewCreatedRestoreMotionScene(viewState: MLVS, binding: VB)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentViewState = viewModel.currentViewState
        onViewCreatedRestoreMotionScene(viewModel.currentViewState, binding)
    }

    /**
     * Ensures removal of listeners from **all** [MotionLayout]s.
     * */
    protected abstract fun getMotionLayouts(): Array<MotionLayout>
    override fun onDestroyView() {
        super.onDestroyView()
        getMotionLayouts().forEach {
            removeTransitionListener(it)
        }
    }
}