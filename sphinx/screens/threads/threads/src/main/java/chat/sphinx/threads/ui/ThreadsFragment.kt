package chat.sphinx.threads.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.threads.R
import chat.sphinx.threads.adapter.ThreadsAdapter
import chat.sphinx.threads.adapter.ThreadsFooterAdapter
import chat.sphinx.threads.databinding.FragmentThreadsBinding
import chat.sphinx.threads.viewstate.ThreadsViewState
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class ThreadsFragment: SideEffectFragment<
        Context,
        ThreadsSideEffect,
        ThreadsViewState,
        ThreadsViewModel,
        FragmentThreadsBinding
        >(R.layout.fragment_threads)
{
    override val binding: FragmentThreadsBinding by viewBinding(FragmentThreadsBinding::bind)
    override val viewModel: ThreadsViewModel by viewModels()

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var userColorsHelper: UserColorsHelper

    fun popBackStack() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.navigator.popBackStack()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        BackPressHandler(viewLifecycleOwner, requireActivity())
        setUpHeader()
        setupThreadsAdapter()
        setClickListeners()

        (requireActivity() as InsetterActivity)
            .addStatusBarPadding(binding.layoutConstraintThreads)

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
            popBackStack()
        }
    }

    private fun setUpHeader() {
        binding.apply {
            includeLayoutDetailScreenAlternativeHeader.apply {
                constraintLayoutDeleteElementContainerTrash.gone
                textViewHeader.text = getString(R.string.threads_header)
                textViewDetailScreenClose.setOnClickListener {
                    popBackStack()
                }
            }
        }
    }

    private fun setupThreadsAdapter() {
        val threadsFooterAdapter = ThreadsFooterAdapter(requireActivity() as InsetterActivity)

        binding.recyclerViewThreadsElementList.apply {
            val threadsAdapter = ThreadsAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel,
                userColorsHelper
            )
            layoutManager = LinearLayoutManager(binding.root.context)
            adapter = ConcatAdapter(threadsAdapter, threadsFooterAdapter)
        }
    }

    private fun setClickListeners() {
        binding.apply {}
    }

    override suspend fun onViewStateFlowCollect(viewState: ThreadsViewState) {
        @Exhaustive
        when (viewState) {
            is ThreadsViewState.Idle -> {}
            is ThreadsViewState.ThreadList -> {}
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
