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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_views.viewstate.ViewState
import kotlinx.coroutines.launch

abstract class BaseDetailFragment<
        VS: ViewState<VS>,
        BVM: BaseViewModel<VS>,
        VB: ViewBinding
        >(@LayoutRes layoutId: Int): Fragment(layoutId)
{
    protected abstract val viewModel: BVM
    protected abstract val binding: VB
    protected val onStopSupervisor: OnStopSupervisor = OnStopSupervisor()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onStopSupervisor.observe(viewLifecycleOwner)

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

    override fun onStart() {
        super.onStart()
        subscribeToViewStateFlow()
    }

    protected abstract suspend fun onViewStateFlowCollect(viewState: VS)
    protected var currentViewState: VS? = null

    /**
     * Called from [onStart] and cancelled from [onStop]
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

    override fun onDestroyView() {
        super.onDestroyView()
        currentViewState = null
    }
}
