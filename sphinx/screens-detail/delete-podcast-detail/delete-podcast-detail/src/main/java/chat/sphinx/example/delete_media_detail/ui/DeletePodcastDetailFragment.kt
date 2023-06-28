package chat.sphinx.example.delete_media_detail.ui

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
import chat.sphinx.delete.media.detail.R
import chat.sphinx.delete.media.detail.databinding.FragmentDeletePodcastDetailsBinding
import chat.sphinx.example.delete_media_detail.adapter.DeletePodcastDetailAdapter
import chat.sphinx.example.delete_media_detail.adapter.DeletePodcastFooterAdapter
import chat.sphinx.example.delete_media_detail.viewstate.DeleteAllNotificationViewStateContainer
import chat.sphinx.example.delete_media_detail.viewstate.DeleteItemNotificationViewState
import chat.sphinx.example.delete_media_detail.viewstate.DeleteMediaDetailViewState
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class DeletePodcastDetailFragment: SideEffectDetailFragment<
        Context,
        DeleteDetailNotifySideEffect,
        DeleteMediaDetailViewState,
        DeletePodcastDetailViewModel,
        FragmentDeletePodcastDetailsBinding
        >(R.layout.fragment_delete_podcast_details)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val binding: FragmentDeletePodcastDetailsBinding by viewBinding(FragmentDeletePodcastDetailsBinding::bind)
    override val viewModel: DeletePodcastDetailViewModel by viewModels()

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
        setupDeleteMediaDetailsAdapter()
        setClickListeners()

        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.deleteMediaScreen)
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
            if (viewModel.deleteAllNotificationViewStateContainer.value is DeleteAllNotificationViewStateContainer.Open) {
                viewModel.deleteAllNotificationViewStateContainer.updateViewState(
                    DeleteAllNotificationViewStateContainer.Closed
                )
            }
            if (viewModel.deleteItemNotificationViewStateContainer.value is DeleteItemNotificationViewState.Open) {
                viewModel.deleteItemNotificationViewStateContainer.updateViewState(
                    DeleteItemNotificationViewState.Closed
                )
            }
            else {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
                }
            }
        }
    }

    private fun setClickListeners() {
        binding.apply {

            includeManageMediaElementHeaderDetails.textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
                }
            }

            includeManageMediaElementHeaderDetails.buttonHeaderDelete.setOnClickListener {
                viewModel.deleteAllNotificationViewStateContainer.updateViewState(
                    DeleteAllNotificationViewStateContainer.Open
                )
            }

            includeLayoutDeleteAllNotificationScreen.apply {

                buttonDelete.setOnClickListener { viewModel.deleteAllDownloadedFeedItems() }

                buttonGotIt.setOnClickListener {
                    lifecycleScope.launch(viewModel.mainImmediate) {
                        viewModel.navigator.popBackStack()
                    }
                }

                buttonCancel.setOnClickListener {
                    viewModel.deleteAllNotificationViewStateContainer.updateViewState(
                        DeleteAllNotificationViewStateContainer.Closed
                    )
                }
            }

            includeLayoutManageStorageDeleteNotification.apply {

                includeLayoutManageStorageDeleteDetails.buttonCancel.setOnClickListener {
                viewModel.closeDeleteItemPopup()
            }

                includeLayoutManageStorageDeleteDetails.buttonDelete.setOnClickListener {
                    (viewModel.deleteItemNotificationViewStateContainer.value as? DeleteItemNotificationViewState.Open)?.let { viewState ->
                        viewModel.deleteDownloadedFeedItem(viewState.feedItem)
                    }
                }

                includeLayoutManageStorageDeleteDetails.buttonCancel
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: DeleteMediaDetailViewState) {
        @Exhaustive
        when (viewState) {
            is DeleteMediaDetailViewState.Idle -> {}
            is DeleteMediaDetailViewState.EpisodeList -> {
                binding.apply {
                    includeManageMediaElementHeaderDetails.apply {

                        textViewHeader.text = viewState.feedName
                        textViewManageStorageElementNumber.visible
                        constraintLayoutDeleteElementContainerTrash.visible
                        textViewManageStorageElementNumber.text = viewState.totalSize
                        textViewPodcastNoFound.goneIfFalse(viewState.episodes.isEmpty())
                        constraintLayoutDeleteElementContainerTrash.goneIfFalse(viewState.episodes.isNotEmpty())

                        includeLayoutDeleteAllNotificationScreen.textViewDeleteDescription.text =
                            getString(R.string.manage_storage_delete_description)

                        if (viewState.episodes.isEmpty()) {
                            constraintLayoutDeleteElementContainerTrash.gone
                        }
                    }
                }
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.deleteItemNotificationViewStateContainer.collect { viewState ->

                binding.includeLayoutManageStorageDeleteNotification.apply {
                    when (viewState) {
                        is DeleteItemNotificationViewState.Closed -> {}
                        is DeleteItemNotificationViewState.Open -> {
                            includeLayoutManageStorageDeleteDetails.textViewStorageDeleteHeader.text = String.format(getString(R.string.manage_storage_delete_item), viewState.feedItem.titleToShow)
                        }
                    }
                    root.setTransitionDuration(300)
                    viewState.transitionToEndSet(root)
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.deleteAllNotificationViewStateContainer.collect { viewState ->

                binding.includeLayoutDeleteAllNotificationScreen.apply {
                    when (viewState) {
                        is DeleteAllNotificationViewStateContainer.Closed -> {
                            root.gone
                        }
                        is DeleteAllNotificationViewStateContainer.Open -> {
                            root.visible
                        }
                        is DeleteAllNotificationViewStateContainer.Deleting -> {
                            root.visible
                            constraintChooseDeleteContainer.gone
                            constraintDeleteProgressContainer.visible
                        }
                        is DeleteAllNotificationViewStateContainer.Deleted -> {
                            root.visible
                            constraintChooseDeleteContainer.gone
                            constraintDeleteProgressContainer.gone
                            constraintDeleteSuccessfullyContainer.visible
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.itemsTotalSizeStateFlow.collect { deleteSize ->

                binding.includeLayoutDeleteAllNotificationScreen.textViewManageStorageFreeSpaceText.text =
                    String.format(
                        getString(R.string.manage_storage_deleted_free_space),
                        deleteSize
                    )
            }
        }
        super.subscribeToViewStateFlow()
    }

    private fun setupDeleteMediaDetailsAdapter() {
        val deletePodcastFooterAdapter = DeletePodcastFooterAdapter(requireActivity() as InsetterActivity)

        binding.recyclerViewStorageElementList.apply {
            val deleteMediaAdapter = DeletePodcastDetailAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel
            )
            layoutManager = LinearLayoutManager(binding.root.context)
            adapter = ConcatAdapter(deleteMediaAdapter, deletePodcastFooterAdapter)
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: DeleteDetailNotifySideEffect) {
        sideEffect.execute(binding.root.context)
    }
}
