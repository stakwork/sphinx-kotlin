package chat.sphinx.discover_tribes.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.discover_tribes.R
import chat.sphinx.discover_tribes.adapter.DiscoverTribesAdapter
import chat.sphinx.discover_tribes.databinding.FragmentDiscoverTribesBinding
import chat.sphinx.discover_tribes.databinding.LayoutDiscoverTribesLoadingBinding
import chat.sphinx.discover_tribes.databinding.LayoutDiscoverTribesTagsBinding
import chat.sphinx.discover_tribes.viewstate.DiscoverTribesLoadingViewState
import chat.sphinx.discover_tribes.viewstate.DiscoverTribesTagsViewState
import chat.sphinx.discover_tribes.viewstate.DiscoverTribesViewState
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addKeyboardPadding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.invisible
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class DiscoverTribesFragment: SideEffectFragment<
        Context,
        DiscoverTribesSideEffect,
        DiscoverTribesViewState,
        DiscoverTribesViewModel,
        FragmentDiscoverTribesBinding
        >(R.layout.fragment_discover_tribes) {

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val viewModel: DiscoverTribesViewModel by viewModels()
    override val binding: FragmentDiscoverTribesBinding by viewBinding(FragmentDiscoverTribesBinding::bind)

    private val discoverTribesTagsBinding: LayoutDiscoverTribesTagsBinding
        get() = binding.includeLayoutDiscoverTribesTags

    private val discoverTribesLoadingBinding: LayoutDiscoverTribesLoadingBinding
        get() = binding.includeLayoutDiscoverTribesLoading

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        BackPressHandler(viewLifecycleOwner, requireActivity())
        setupDiscoverTribesAdapter()

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
        binding.includeLayoutDiscoverTribesLoading.viewDiscoverTribesLock.setOnClickListener {
            viewModel.discoverTribesLoadingViewStateContainer.updateViewState(DiscoverTribesLoadingViewState.Closed)
        }
        binding.includeLayoutDiscoverTribesTags.viewDiscoverTribesLock.setOnClickListener {
            viewModel.discoverTribesTagsViewStateContainer.updateViewState(DiscoverTribesTagsViewState.Closed)
        }

        fun updateTagsNumber() {
            viewModel.getTribeSelectedTags()

            binding.layoutButtonTag.apply {
                if (viewModel.tribeSelectedTagsList.value.isNullOrEmpty()) {
                    tagNumber.invisible
                    tagIcon.visible
                    layoutConstraintButtonTags.background = ContextCompat.getDrawable(
                        root.context,
                        R.drawable.background_tag_transparent_rad
                    )
                    tagsPopupName.setTextColor(
                        ContextCompat.getColor(
                            root.context,
                            R.color.primaryText
                        )
                    )
                } else {
                    tagNumber.visible
                    tagIcon.invisible
                    layoutConstraintButtonTags.background =
                        ContextCompat.getDrawable(root.context, R.drawable.background_tag_white_rad)
                    tagsPopupName.setTextColor(ContextCompat.getColor(root.context, R.color.body))
                    tagNumber.text = viewModel.tribeSelectedTagsList.value?.size.toString()
                }
            }
        }

        discoverTribesTagsBinding.includeLayoutDiscoverTribesTagsDetails.apply {

            fun getTagBackground(isSelected: Boolean): Drawable? {
                return if (isSelected) {
                    ContextCompat.getDrawable(root.context, R.drawable.background_tag_white_rad)
                } else ContextCompat.getDrawable(root.context, R.drawable.background_tag_transparent_rad)
            }

            fun getTagTextColor(isSelected: Boolean) : Int {
                return if (isSelected) {
                    ContextCompat.getColor(root.context, R.color.body)
                }
                else ContextCompat.getColor(root.context, R.color.primaryText)
            }

            fun setAllTagsNames() {
                includeTag0.tagName.text = viewModel.tribeTagsStateFlow.value[0].name
                includeTag1.tagName.text = viewModel.tribeTagsStateFlow.value[1].name
                includeTag2.tagName.text = viewModel.tribeTagsStateFlow.value[2].name
                includeTag3.tagName.text = viewModel.tribeTagsStateFlow.value[3].name
                includeTag4.tagName.text = viewModel.tribeTagsStateFlow.value[4].name
                includeTag5.tagName.text = viewModel.tribeTagsStateFlow.value[5].name
                includeTag6.tagName.text = viewModel.tribeTagsStateFlow.value[6].name
                includeTag7.tagName.text = viewModel.tribeTagsStateFlow.value[7].name
                includeTag8.tagName.text = viewModel.tribeTagsStateFlow.value[8].name
            }

            fun bindSelectedTags() {
                includeTag0.layoutButtonTag.background = getTagBackground(viewModel.tribeTagsStateFlow.value[0].isSelected)
                includeTag0.tagName.setTextColor(getTagTextColor(viewModel.tribeTagsStateFlow.value[0].isSelected))
                includeTag1.layoutButtonTag.background = getTagBackground(viewModel.tribeTagsStateFlow.value[1].isSelected)
                includeTag1.tagName.setTextColor(getTagTextColor(viewModel.tribeTagsStateFlow.value[1].isSelected))
                includeTag2.layoutButtonTag.background = getTagBackground(viewModel.tribeTagsStateFlow.value[2].isSelected)
                includeTag2.tagName.setTextColor(getTagTextColor(viewModel.tribeTagsStateFlow.value[2].isSelected))
                includeTag3.layoutButtonTag.background = getTagBackground(viewModel.tribeTagsStateFlow.value[3].isSelected)
                includeTag3.tagName.setTextColor(getTagTextColor(viewModel.tribeTagsStateFlow.value[3].isSelected))
                includeTag4.layoutButtonTag.background = getTagBackground(viewModel.tribeTagsStateFlow.value[4].isSelected)
                includeTag4.tagName.setTextColor(getTagTextColor(viewModel.tribeTagsStateFlow.value[4].isSelected))
                includeTag5.layoutButtonTag.background = getTagBackground(viewModel.tribeTagsStateFlow.value[5].isSelected)
                includeTag5.tagName.setTextColor(getTagTextColor(viewModel.tribeTagsStateFlow.value[5].isSelected))
                includeTag6.layoutButtonTag.background = getTagBackground(viewModel.tribeTagsStateFlow.value[6].isSelected)
                includeTag6.tagName.setTextColor(getTagTextColor(viewModel.tribeTagsStateFlow.value[6].isSelected))
                includeTag7.layoutButtonTag.background = getTagBackground(viewModel.tribeTagsStateFlow.value[7].isSelected)
                includeTag7.tagName.setTextColor(getTagTextColor(viewModel.tribeTagsStateFlow.value[7].isSelected))
                includeTag8.layoutButtonTag.background = getTagBackground(viewModel.tribeTagsStateFlow.value[8].isSelected)
                includeTag8.tagName.setTextColor(getTagTextColor(viewModel.tribeTagsStateFlow.value[8].isSelected))
            }

            setAllTagsNames()
            updateTagsNumber()

            includeTag0.layoutButtonTag.setOnClickListener {
                viewModel.changeSelectTag(0)
                bindSelectedTags()
            }
            includeTag1.layoutButtonTag.setOnClickListener {
                viewModel.changeSelectTag(1)
                bindSelectedTags()
            }
            includeTag2.layoutButtonTag.setOnClickListener {
                viewModel.changeSelectTag(2)
                bindSelectedTags()
            }
            includeTag3.layoutButtonTag.setOnClickListener {
                viewModel.changeSelectTag(3)
                bindSelectedTags()
            }
            includeTag4.layoutButtonTag.setOnClickListener {
                viewModel.changeSelectTag(4)
                bindSelectedTags()
            }
            includeTag5.layoutButtonTag.setOnClickListener {
                viewModel.changeSelectTag(5)
                bindSelectedTags()
            }
            includeTag6.layoutButtonTag.setOnClickListener {
                viewModel.changeSelectTag(6)
                bindSelectedTags()
            }
            includeTag7.layoutButtonTag.setOnClickListener {
                viewModel.changeSelectTag(7)
                bindSelectedTags()
            }
            includeTag8.layoutButtonTag.setOnClickListener {
                viewModel.changeSelectTag(8)
                bindSelectedTags()
            }

            buttonApplyTags.setOnClickListener {
                viewModel.discoverTribesTagsViewStateContainer.updateViewState(
                    DiscoverTribesTagsViewState.Closed)
                updateTagsNumber()
            }
        }

        discoverTribesTagsBinding.apply {
            (requireActivity() as InsetterActivity).addKeyboardPadding(root)
        }

        binding.layoutButtonTag.root.setOnClickListener {
            viewModel.discoverTribesTagsViewStateContainer.updateViewState(
                DiscoverTribesTagsViewState.Open)
        }
    }

    private fun setupDiscoverTribesAdapter() {
        val linearLayoutManager = LinearLayoutManager(context)

        binding.recyclerViewList.apply {
            val discoverTribesAdapter = DiscoverTribesAdapter(
                this,
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel
            )

            this.setHasFixedSize(false)
            layoutManager = linearLayoutManager
            adapter = discoverTribesAdapter
            itemAnimator = null

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if(!recyclerView.canScrollVertically(1)){
                        viewModel.discoverTribesLoadingViewStateContainer.updateViewState(
                            DiscoverTribesLoadingViewState.Open
                        )
//                        binding.loadingProgressBar.visible
                    }
                }
                }
            )
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
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.discoverTribesLoadingViewStateContainer.collect { viewState ->
                discoverTribesLoadingBinding.root.setTransitionDuration(250)
                viewState.transitionToEndSet(discoverTribesLoadingBinding.root)
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
                viewModel.discoverTribesTagsViewStateContainer.updateViewState(
                    DiscoverTribesTagsViewState.Closed)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override suspend fun onSideEffectCollect(sideEffect: DiscoverTribesSideEffect) {
    }

}