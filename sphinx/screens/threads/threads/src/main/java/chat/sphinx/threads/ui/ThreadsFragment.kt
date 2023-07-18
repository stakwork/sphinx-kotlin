package chat.sphinx.threads.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import chat.sphinx.threads.R
import chat.sphinx.threads.databinding.FragmentThreadsBinding
import chat.sphinx.threads.viewstate.ThreadsViewState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class ThreadsFragment: SideEffectDetailFragment<
        Context,
        ThreadsSideEffect,
        ThreadsViewState,
        ThreadsViewModel,
        FragmentThreadsBinding
        >(R.layout.fragment_threads)
{
    override val binding: FragmentThreadsBinding by viewBinding(FragmentThreadsBinding::bind)
    override val viewModel: ThreadsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel
    }

    override fun closeDetailsScreen() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.navigator.closeDetailScreen()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        BackPressHandler(viewLifecycleOwner, requireActivity())
        setUpHeader()
        setClickListeners()

        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.layoutConstraintThreads)

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
        }
    }

    private fun setUpHeader() {
        binding.apply {}
    }

    private fun setClickListeners() {
        binding.apply {}
    }

    override suspend fun onViewStateFlowCollect(viewState: ThreadsViewState) {
        @Exhaustive
        when (viewState) {
            is ThreadsViewState.Idle -> {}
        }
    }

    override fun subscribeToViewStateFlow() {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {}
        super.subscribeToViewStateFlow()
    }

    override suspend fun onSideEffectCollect(sideEffect: ThreadsSideEffect) {
        sideEffect.execute(binding.root.context)
    }
}
