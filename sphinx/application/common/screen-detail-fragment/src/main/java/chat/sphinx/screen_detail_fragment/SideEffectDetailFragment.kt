/*
*  Copyright 2021 Matthew Nelson
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
* */
package chat.sphinx.screen_detail_fragment

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.LayoutRes
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import io.matthewnelson.concept_views.sideeffect.SideEffect
import io.matthewnelson.concept_views.viewstate.ViewState
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.collectSideEffects
import kotlinx.coroutines.launch

/**
 * An abstraction that adds [SideEffect]s to the [BaseFragment].
 *
 * @see [SideEffect]
 * */
abstract class SideEffectDetailFragment<
        T,
        SE: SideEffect<T>,
        VS: ViewState<VS>,
        SEVM: SideEffectViewModel<T, SE, VS>,
        VB: ViewBinding
        >(@LayoutRes layoutId: Int): BaseFragment<
        VS,
        SEVM,
        VB
        >(layoutId)
{

    override fun onStart() {
        super.onStart()
        subscribeToSideEffectSharedFlow()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BackPressHandler(viewLifecycleOwner, requireActivity())
    }

    private inner class BackPressHandler(
        owner: LifecycleOwner,
        activity: FragmentActivity,
    ): OnBackPressedCallback(true) {

        init {
            activity.apply {
                onBackPressedDispatcher.addCallback(
                    owner,
                    this@BackPressHandler,
                )
            }
        }

        override fun handleOnBackPressed() {
            closeDetailsScreen()
        }
    }

    abstract fun closeDetailsScreen()

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
