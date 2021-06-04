package chat.sphinx.activitymain.ui

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.navigation.NavController
import androidx.viewbinding.ViewBinding
import io.matthewnelson.android_concept_views.MotionLayoutViewState
import io.matthewnelson.android_feature_activity.NavigationActivity
import io.matthewnelson.android_feature_activity.NavigationViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.feature_navigation.NavigationDriver
import kotlinx.coroutines.launch

abstract class MotionLayoutNavigationActivity<
        MLVS: MotionLayoutViewState<MLVS>,
        BVM: BaseViewModel<MLVS>,
        D: NavigationDriver<NavController>,
        NVM: NavigationViewModel<D>,
        VB: ViewBinding
        >(@LayoutRes layoutId: Int): NavigationActivity<
        BVM,
        D,
        NVM,
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
    protected abstract fun onCreatedRestoreMotionScene(viewState: MLVS, binding: VB)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentViewState = viewModel.currentViewState
        onCreatedRestoreMotionScene(viewModel.currentViewState, binding)
    }

    override fun onStart() {
        super.onStart()
        subscribeToViewStateFlow()
    }

    protected abstract suspend fun onViewStateFlowCollect(viewState: MLVS)
    protected var currentViewState: MLVS? = null

    /**
     * Called from [onStart]. Must be mindful if overriding to lazily start things
     * using lifecycleScope.launchWhenStarted
     * */
    protected open fun subscribeToViewStateFlow() {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.collectViewState { viewState ->
                if (currentViewState != viewState) {
                    currentViewState = viewState
                    onViewStateFlowCollect(viewState)
                }
            }
        }
    }

    /**
     * Ensures removal of listeners from **all** [MotionLayout]s.
     * */
    protected abstract fun getMotionLayouts(): Array<MotionLayout>
    override fun onDestroy() {
        super.onDestroy()
        getMotionLayouts().forEach {
            removeTransitionListener(it)
        }
    }
}