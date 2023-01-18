package chat.sphinx.discover_tribes.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.discover_tribes.R
import chat.sphinx.discover_tribes.databinding.FragmentDiscoverTribesBinding
import chat.sphinx.discover_tribes.databinding.LayoutDiscoverTribesTagsBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addKeyboardPadding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class DiscoverTribesFragment: BaseFragment<
        DiscoverTribesViewState,
        DiscoverTribesViewModel,
        FragmentDiscoverTribesBinding
        >(R.layout.fragment_discover_tribes) {

    override val viewModel: DiscoverTribesViewModel by viewModels()
    override val binding: FragmentDiscoverTribesBinding by viewBinding(FragmentDiscoverTribesBinding::bind)

    private val discoverTribesTagsBinding: LayoutDiscoverTribesTagsBinding
        get() = binding.includeLayoutDiscoverTribesTags

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        BackPressHandler(viewLifecycleOwner, requireActivity())

        binding.includeDiscoverTribesHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.discover_tribes_header_name)
            textViewDetailScreenClose.gone
            textViewDetailScreenHeaderNavBack.visible
            textViewDetailScreenHeaderNavBack.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
                }
            }
        }

        discoverTribesTagsBinding.apply {
            (requireActivity() as InsetterActivity).addKeyboardPadding(root)
        }

        binding.layoutButtonTag.root.setOnClickListener {
            viewModel.discoverTribesTagsViewStateContainer.updateViewState(DiscoverTribesTagsViewState.Open)
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override suspend fun onViewStateFlowCollect(viewState: DiscoverTribesViewState) {
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.discoverTribesTagsViewStateContainer.collect { viewState ->
                discoverTribesTagsBinding.root.setTransitionDuration(250)
                viewState.transitionToEndSet(discoverTribesTagsBinding.root)
            }
        }
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
            if (viewModel.discoverTribesTagsViewStateContainer.value is DiscoverTribesTagsViewState.Open) {
                viewModel.discoverTribesTagsViewStateContainer.updateViewState(DiscoverTribesTagsViewState.Closed)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}