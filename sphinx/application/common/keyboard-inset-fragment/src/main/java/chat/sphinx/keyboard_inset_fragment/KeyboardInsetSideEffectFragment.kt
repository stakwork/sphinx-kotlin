package chat.sphinx.keyboard_inset_fragment

import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.collectSideEffects
import io.matthewnelson.concept_views.sideeffect.SideEffect
import io.matthewnelson.concept_views.viewstate.ViewState
import kotlinx.coroutines.launch

abstract class KeyboardInsetSideEffectFragment<
        T,
        SE: SideEffect<T>,
        VS: ViewState<VS>,
        SEVM: SideEffectViewModel<T, SE, VS>,
        VB: ViewBinding
        >(@LayoutRes layoutId: Int): KeyboardInsetBaseFragment<
        VS,
        SEVM,
        VB
        >(layoutId)
{
    override fun onStart() {
        super.onStart()
        subscribeToSideEffectSharedFlow()
    }

    protected abstract suspend fun onSideEffectCollect(sideEffect: SE)

    /**
     * Called from [onStart] and cancelled in [onStop]
     * */
    protected open fun subscribeToSideEffectSharedFlow() {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.collectSideEffects { sideEffect ->
                onSideEffectCollect(sideEffect)
            }
        }
    }
}