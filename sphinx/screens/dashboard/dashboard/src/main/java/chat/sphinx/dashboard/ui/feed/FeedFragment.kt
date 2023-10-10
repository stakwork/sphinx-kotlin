package chat.sphinx.dashboard.ui.feed

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentFeedBinding
import chat.sphinx.dashboard.ui.DashboardFragment
import chat.sphinx.dashboard.ui.adapter.FeedSearchAdapter
import chat.sphinx.dashboard.ui.viewstates.FeedChipsViewState
import chat.sphinx.dashboard.ui.viewstates.FeedViewState
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.resources.inputMethodManager
import chat.sphinx.wrapper_common.feed.FeedType
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("NOTHING_TO_INLINE")
private inline fun FragmentFeedBinding.searchBarClearFocus() {
    layoutSearchBar.editTextDashboardSearch.clearFocus()
}

@AndroidEntryPoint
internal class FeedFragment : SideEffectFragment<
        FragmentActivity,
        FeedSideEffect,
        FeedViewState,
        FeedViewModel,
        FragmentFeedBinding
        >(R.layout.fragment_feed)
{

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val viewModel: FeedViewModel by viewModels()
    override val binding: FragmentFeedBinding by viewBinding(FragmentFeedBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSearch()
        setupFeedViewPager()
        showFeedSearchAdapter()
    }

    override fun onResume() {
        super.onResume()

        BackPressHandler(binding.root.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())
    }

    private inner class BackPressHandler(context: Context): CloseAppOnBackPress(context) {
        override fun handleOnBackPressed() {
            if (
                parentFragment is DashboardFragment &&
                (parentFragment as DashboardFragment)?.closeDrawerIfOpen() == true
            ) {
                return
            } else if (
                viewModel.currentViewState is FeedViewState.SearchResults ||
                viewModel.currentViewState is FeedViewState.LoadingSearchResults
            ) {
                binding.layoutSearchBar.editTextDashboardSearch.setText("")
            } else if (
                viewModel.currentViewState is FeedViewState.SearchPlaceHolder ||
                viewModel.currentViewState is FeedViewState.SearchPodcastPlaceHolder ||
                viewModel.currentViewState is FeedViewState.SearchVideoPlaceHolder
            ) {
                viewModel.updateViewState(FeedViewState.Idle)
                binding.searchBarClearFocus()
            } else {
                super.handleOnBackPressed()
            }
        }
    }

    private fun getFeedTypeSelected(): FeedType? {
        binding.apply {
            if (chipListen.isChecked) {
                return FeedType.Podcast
            } else if (chipWatch.isChecked) {
                return FeedType.Video
            }
        }
        return null
    }

    private fun setupSearch() {
        binding.layoutSearchBar.apply {
            editTextDashboardSearch.addTextChangedListener { editable ->
                buttonDashboardSearchClear.goneIfFalse(editable.toString().isNotEmpty())

                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                    viewModel.searchFeedsBy(
                        editable.toString(),
                        getFeedTypeSelected(),
                        editTextDashboardSearch.hasFocus()
                    )
                }
            }

            editTextDashboardSearch.setOnFocusChangeListener { _, hasFocus ->
                viewModel.toggleSearchState(hasFocus)
            }

            editTextDashboardSearch.setOnEditorActionListener(object: TextView.OnEditorActionListener {
                override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
                    if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                        editTextDashboardSearch.let { editText ->
                            binding.root.context.inputMethodManager?.let { imm ->
                                if (imm.isActive(editText)) {
                                    imm.hideSoftInputFromWindow(editText.windowToken, 0)
                                    editText.clearFocus()
                                }
                            }
                        }
                        return true
                    }
                    return false
                }
            })

            buttonDashboardSearchClear.setOnClickListener {
                editTextDashboardSearch.setText("")
            }
        }
    }

    private fun setupFeedViewPager() {
        binding.apply {
            val feedFragmentsAdapter = FeedFragmentsAdapter(
                this@FeedFragment
            )

            chipAll.isChecked = true

            viewPagerFeedFragments.adapter = feedFragmentsAdapter
            viewPagerFeedFragments.isUserInputEnabled = false
            viewPagerFeedFragments.currentItem = FeedFragmentsAdapter.CHIP_ALL_POSITION

            chipAll.setOnClickListener {
                viewModel.feedChipsViewStateContainer.updateViewState(
                    FeedChipsViewState.All
                )
            }

            chipListen.setOnClickListener {
                viewModel.feedChipsViewStateContainer.updateViewState(
                    FeedChipsViewState.Listen
                )
            }

            chipWatch.setOnClickListener {
                viewModel.feedChipsViewStateContainer.updateViewState(
                    FeedChipsViewState.Watch
                )
            }

            chipRead.setOnClickListener {
                viewModel.feedChipsViewStateContainer.updateViewState(
                    FeedChipsViewState.Read
                )
            }

//            chipPlay.setOnClickListener {
//                viewModel.feedChipsViewStateContainer.updateViewState(
//                    FeedChipsViewState.Play
//                )
//            }
        }
    }

    private fun showFeedSearchAdapter() {
        val searchResultsAdapter = FeedSearchAdapter(
            imageLoader,
            viewLifecycleOwner,
            onStopSupervisor,
            viewModel
        )

        binding.layoutFeedSearch.recyclerViewFeedSearchResults.apply {
            this.setHasFixedSize(false)
            layoutManager = LinearLayoutManager(binding.root.context)
            adapter = searchResultsAdapter
        }
    }

    fun shouldToggleNavBar(show: Boolean) {
        if (parentFragment is DashboardFragment) {
            (parentFragment as DashboardFragment)?.shouldToggleNavBar(show)
        }
    }

    override fun onPause() {
        super.onPause()
        binding.searchBarClearFocus()
    }

    companion object {
        fun newInstance(): FeedFragment {
            return FeedFragment()
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: FeedViewState) {
        binding.layoutFeedSearch.apply {
            @Exhaustive
            when (viewState) {
                is FeedViewState.Idle -> {
                    root.gone
                }
                is FeedViewState.SearchPlaceHolder -> {
                    root.visible
                    layoutConstraintFeedSearchVideoPlaceholder.gone
                    layoutConstraintFeedSearchPodcastPlaceholder.gone
                    layoutConstraintFeedSearchPlaceholder.visible
                    layoutConstraintLoadingSearchResults.gone
                    recyclerViewFeedSearchResults.gone
                }
                is FeedViewState.SearchPodcastPlaceHolder -> {
                    root.visible
                    layoutConstraintFeedSearchVideoPlaceholder.gone
                    layoutConstraintFeedSearchPodcastPlaceholder.visible
                    layoutConstraintFeedSearchPlaceholder.gone
                    layoutConstraintLoadingSearchResults.gone
                    recyclerViewFeedSearchResults.gone
                }
                is FeedViewState.SearchVideoPlaceHolder -> {
                    root.visible
                    layoutConstraintFeedSearchVideoPlaceholder.visible
                    layoutConstraintFeedSearchPodcastPlaceholder.gone
                    layoutConstraintFeedSearchPlaceholder.gone
                    layoutConstraintLoadingSearchResults.gone
                    recyclerViewFeedSearchResults.gone
                }
                is FeedViewState.LoadingSearchResults -> {
                    root.visible
                    layoutConstraintFeedSearchVideoPlaceholder.gone
                    layoutConstraintFeedSearchPodcastPlaceholder.gone
                    layoutConstraintFeedSearchPlaceholder.gone
                    layoutConstraintLoadingSearchResults.visible
                    recyclerViewFeedSearchResults.gone
                }
                is FeedViewState.SearchResults -> {
                    root.visible
                    layoutConstraintFeedSearchVideoPlaceholder.gone
                    layoutConstraintFeedSearchPodcastPlaceholder.gone
                    layoutConstraintFeedSearchPlaceholder.gone
                    layoutConstraintLoadingSearchResults.gone
                    recyclerViewFeedSearchResults.visible
                }
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            binding.apply {
                viewModel.feedChipsViewStateContainer.collect { viewState ->
                    when (viewState) {
                        is FeedChipsViewState.All -> {
                            viewPagerFeedFragments.currentItem =
                                FeedFragmentsAdapter.CHIP_ALL_POSITION

                            viewModel.syncActions()
                        }
                        is FeedChipsViewState.Listen -> {
                            viewPagerFeedFragments.currentItem =
                                FeedFragmentsAdapter.CHIP_LISTEN_POSITION
                        }
                        is FeedChipsViewState.Watch -> {
                            viewPagerFeedFragments.currentItem =
                                FeedFragmentsAdapter.CHIP_WATCH_POSITION
                        }
                        is FeedChipsViewState.Read -> {
                            viewPagerFeedFragments.currentItem =
                                FeedFragmentsAdapter.CHIP_READ_POSITION
                        }
//                        is FeedChipsViewState.Play -> {
//                            viewPagerFeedFragments.currentItem =
//                                FeedFragmentsAdapter.CHIP_PLAY_POSITION
//                        }
                    }
                }
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: FeedSideEffect) {
        sideEffect.execute(requireActivity())
    }
}
