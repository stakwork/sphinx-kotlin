package chat.sphinx.tribes_discover.ui

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.tribes_discover.R
import chat.sphinx.tribes_discover.adapter.TribesDiscoverAdapter
import chat.sphinx.tribes_discover.databinding.LayoutDiscoverTribesTagsBinding
import chat.sphinx.tribes_discover.viewstate.DiscoverTribesTagsViewState
import chat.sphinx.tribes_discover.viewstate.DiscoverTribesViewState
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.resources.inputMethodManager
import chat.sphinx.tribes_discover.adapter.TribesDiscoverFooterAdapter
import chat.sphinx.tribes_discover.databinding.FragmentTribesDiscoverBinding
import chat.sphinx.tribes_discover.databinding.LayoutButtonTagBinding
import chat.sphinx.tribes_discover.model.DiscoverTribesTag
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.*
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class TribesDiscoverFragment: SideEffectFragment<
        Context,
        TribesDiscoverSideEffect,
        DiscoverTribesViewState,
        TribesDiscoverViewModel,
        FragmentTribesDiscoverBinding
        >(R.layout.fragment_tribes_discover) {

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val viewModel: TribesDiscoverViewModel by viewModels()
    override val binding: FragmentTribesDiscoverBinding by viewBinding(FragmentTribesDiscoverBinding::bind)

    private val discoverTribesTagsBinding: LayoutDiscoverTribesTagsBinding
        get() = binding.includeLayoutDiscoverTribesTags

    private val tagViews: List<LayoutButtonTagBinding> by lazy {
        discoverTribesTagsBinding.includeLayoutDiscoverTribesTagsDetails.let {
            listOf(
                it.includeTag0,
                it.includeTag1,
                it.includeTag2,
                it.includeTag3,
                it.includeTag4,
                it.includeTag5,
                it.includeTag6,
                it.includeTag7,
                it.includeTag8,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        BackPressHandler(viewLifecycleOwner, requireActivity())

        setupDiscoverTribesAdapter()
        setupSearch()
        setupHeader()
        setupFooter()
        setupClickListeners()

        viewModel.getDiscoverTribesList()
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
                viewModel.discoverTribesTagsViewStateContainer.updateViewState(
                    DiscoverTribesTagsViewState.Closed(null)
                )
            } else {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }
    }

    private fun setupClickListeners() {
        discoverTribesTagsBinding.apply {

            tagViews.forEachIndexed { index, element ->
                element.layoutButtonTag.setOnClickListener {
                    viewModel.toggleTagWith(index)
                }
            }

            includeLayoutDiscoverTribesTagsDetails.buttonApplyTags.setOnClickListener {
                viewModel.applyTags(
                    binding.layoutSearchBar.editTextDashboardSearch.text.toString()
                )
            }

            viewDiscoverTribesLock.setOnClickListener {
                viewModel.resetTags()
            }
        }

        binding.layoutButtonTag.root.setOnClickListener {
            viewModel.showTagsView()
        }
    }

    private fun setupFooter() {
        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(discoverTribesTagsBinding.includeLayoutDiscoverTribesTagsDetails.root)
    }

    private fun setupHeader() {
        binding.includeDiscoverTribesHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.discover_tribes_header_name)
            textViewDetailScreenClose.visible
            textViewDetailScreenHeaderNavBack.gone

            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }
    }

    private fun setupDiscoverTribesAdapter() {
        binding.recyclerViewList.apply {
            val tribesDiscoverAdapter = TribesDiscoverAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel
            )

            val tribesDiscoverFooterAdapter = TribesDiscoverFooterAdapter(requireActivity() as InsetterActivity)

            this.setHasFixedSize(false)
            layoutManager = LinearLayoutManager(binding.root.context)
            adapter = ConcatAdapter(tribesDiscoverAdapter, tribesDiscoverFooterAdapter)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {

                        viewModel.loadNextPage(
                            binding.layoutSearchBar.editTextDashboardSearch.text.toString()
                        )
                    }
                }
            })
        }
    }

    private fun setupSearch() {
        binding.layoutSearchBar.apply {

            editTextDashboardSearch.setOnEditorActionListener(object: TextView.OnEditorActionListener {
                override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
                    if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                        binding.root.context.inputMethodManager?.let { imm ->
                            if (imm.isActive(editTextDashboardSearch)) {
                                imm.hideSoftInputFromWindow(editTextDashboardSearch.windowToken, 0)
                                editTextDashboardSearch.clearFocus()
                            }
                        }
                        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                            viewModel.getDiscoverTribesList(
                                editTextDashboardSearch.text.toString()
                            )
                        }
                        return true
                    }
                    return false
                }
            })

            editTextDashboardSearch.addTextChangedListener { editable ->
                buttonDashboardSearchClear.goneIfFalse(
                    editable.toString().isNotEmpty()
                )
            }

            buttonDashboardSearchClear.setOnClickListener {
                editTextDashboardSearch.setText("")
                viewModel.getDiscoverTribesList(null)
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: DiscoverTribesViewState) {
        binding.apply {
            when (viewState) {
                is DiscoverTribesViewState.Tribes -> {
                    progressBarDiscoverTribes.gone

                    textViewNoTribes.goneIfFalse(viewState.tribes.isEmpty())
                    recyclerViewList.goneIfFalse(viewState.tribes.isNotEmpty())
                }
                else -> {
                    progressBarDiscoverTribes.visible
                    textViewNoTribes.gone
                    recyclerViewList.gone
                }
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.discoverTribesTagsViewStateContainer.collect { viewState ->
                when (viewState) {
                    is DiscoverTribesTagsViewState.Closed -> {
                        viewState.tagsCount?.let {
                            updateTagsNumber(it)
                        }
                    }
                    else -> {}
                }

                discoverTribesTagsBinding.root.setTransitionDuration(250)
                viewState.transitionToEndSet(discoverTribesTagsBinding.root)
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.tribeTagsStateFlow.collect { tags ->
                updateTags(
                    tags.toList()
                )
            }
        }
    }

    private fun updateTags(tags: List<DiscoverTribesTag>) {
        tagViews.forEachIndexed { index, element ->
            if (tags.size > index) {
                val selected = tags[index].isSelected

                element.tagName.text = tags[index].name

                element.layoutButtonTag.background = ContextCompat.getDrawable(
                    element.root.context,
                    if (selected) {
                        R.drawable.background_tag_white_rad
                    } else {
                        R.drawable.background_tag_transparent_rad
                    }
                )

                element.tagName.setTextColor(
                    ContextCompat.getColor(
                        element.root.context,
                        if (selected) {
                            R.color.body
                        } else {
                            R.color.primaryText
                        }
                    )
                )
            }
        }
    }

    private fun updateTagsNumber(count: Int) {
        binding.layoutButtonTag.apply {
            val tagsSelected = count > 0

            tagNumber.invisibleIfFalse(tagsSelected)
            tagIcon.invisibleIfFalse(!tagsSelected)

            layoutConstraintButtonSmall.background = ContextCompat.getDrawable(
                root.context,
                if (tagsSelected) {
                    R.drawable.background_tag_white_rad
                } else {
                    R.drawable.background_button_tags
                }
            )

            tagsPopupName.setTextColor(
                ContextCompat.getColor(
                    root.context,
                    if (tagsSelected) {
                        R.color.body
                    } else {
                        R.color.primaryText
                    }
                )
            )

            tagNumber.text = count.toString()
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: TribesDiscoverSideEffect) {}
}