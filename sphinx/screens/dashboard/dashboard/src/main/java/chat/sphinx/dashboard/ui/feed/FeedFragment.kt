package chat.sphinx.dashboard.ui.feed

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_network_query_podcast_search.model.PodcastSearchResultDto
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentFeedBinding
import chat.sphinx.dashboard.ui.DashboardFragmentsAdapter
import chat.sphinx.dashboard.ui.adapter.PodcastSearchAdapter
import chat.sphinx.dashboard.ui.viewstates.FeedViewState
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.resources.inputMethodManager
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("NOTHING_TO_INLINE")
private inline fun FragmentFeedBinding.searchBarClearFocus() {
    layoutSearchBar.editTextDashboardSearch.clearFocus()
}

@AndroidEntryPoint
internal class FeedFragment : SideEffectFragment<
        Context,
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
        BackPressHandler(binding.root.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())

        setupSearch()
        setupFeedViewPager()
        showPodcastSearchAdapter()
    }

    private inner class BackPressHandler(context: Context): CloseAppOnBackPress(context) {
        override fun handleOnBackPressed() {
            if (viewModel.currentViewState !is FeedViewState.Idle) {
                viewModel.updateViewState(FeedViewState.Idle)
            } else {
                binding.searchBarClearFocus()
                super.handleOnBackPressed()
            }
        }
    }

    private fun setupSearch() {
        binding.layoutSearchBar.apply {
            editTextDashboardSearch.addTextChangedListener { editable ->
                buttonDashboardSearchClear.goneIfFalse(editable.toString().isNotEmpty())

                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                    viewModel.searchPodcastBy(
                        editable.toString(),
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
                viewPagerFeedFragments.currentItem = FeedFragmentsAdapter.CHIP_ALL_POSITION
            }

            chipListen.setOnClickListener {
                viewPagerFeedFragments.currentItem = FeedFragmentsAdapter.CHIP_LISTEN_POSITION
            }

            chipWatch.setOnClickListener {
                viewPagerFeedFragments.currentItem = FeedFragmentsAdapter.CHIP_WATCH_POSITION
            }

            chipRead.setOnClickListener {
                viewPagerFeedFragments.currentItem = FeedFragmentsAdapter.CHIP_READ_POSITION
            }

            chipPlay.setOnClickListener {
                viewPagerFeedFragments.currentItem = FeedFragmentsAdapter.CHIP_PLAY_POSITION
            }
        }
    }

    private fun showPodcastSearchAdapter() {
        val searchResultsAdapter = PodcastSearchAdapter(
            imageLoader,
            viewLifecycleOwner,
            onStopSupervisor,
            viewModel
        )

        binding.layoutPodcastSearch.recyclerViewPodcastSearchResults.apply {
            this.setHasFixedSize(false)
            layoutManager = LinearLayoutManager(binding.root.context)
            adapter = searchResultsAdapter
        }
    }

    override fun onPause() {
        super.onPause()
        binding.searchBarClearFocus()
    }

    override suspend fun onSideEffectCollect(sideEffect: FeedSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    companion object {
        fun newInstance(): FeedFragment {
            return FeedFragment()
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: FeedViewState) {
        binding.layoutPodcastSearch.apply {
            @Exhaustive
            when (viewState) {
                is FeedViewState.Idle -> {
                    root.gone
                }
                is FeedViewState.SearchPlaceHolder -> {
                    root.visible
                    layoutConstraintPodcastSearchPlaceholder.visible
                    layoutConstraintLoadingSearchSesults.gone
                    recyclerViewPodcastSearchResults.gone
                }
                is FeedViewState.LoadingSearchResults -> {
                    root.visible
                    layoutConstraintPodcastSearchPlaceholder.gone
                    layoutConstraintLoadingSearchSesults.visible
                    recyclerViewPodcastSearchResults.gone
                }
                is FeedViewState.SearchResults -> {
                    root.visible
                    layoutConstraintPodcastSearchPlaceholder.gone
                    layoutConstraintLoadingSearchSesults.gone
                    recyclerViewPodcastSearchResults.visible
                }
            }
        }
    }
}
