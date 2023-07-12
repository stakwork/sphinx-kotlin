package chat.sphinx.example.delete_media.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.delete.media.R
import chat.sphinx.delete.media.databinding.FragmentDeletePodcastBinding
import chat.sphinx.example.delete_media.adapter.PodcastDeleteAdapter
import chat.sphinx.example.delete_media.viewstate.DeletePodcastViewState
import chat.sphinx.example.delete_media.viewstate.DeleteNotificationViewState
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import chat.sphinx.wrapper_common.calculateSize
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
internal class DeletePodcastFragment: SideEffectDetailFragment<
        Context,
        DeleteNotifySideEffect,
        DeletePodcastViewState,
        DeletePodcastViewModel,
        FragmentDeletePodcastBinding
        >(R.layout.fragment_delete_podcast)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val binding: FragmentDeletePodcastBinding by viewBinding(FragmentDeletePodcastBinding::bind)
    override val viewModel: DeletePodcastViewModel by viewModels()

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
        setupMediaSectionAdapter()
        setUpHeader()
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
            if (viewModel.deleteAllFeedsNotificationViewStateContainer.value is DeleteNotificationViewState.Open) {
                viewModel.deleteAllFeedsNotificationViewStateContainer.updateViewState(
                    DeleteNotificationViewState.Closed
                )
            }
            else {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
                }
            }
        }
    }

    private fun setUpHeader() {
        binding.apply {
            includeManageMediaElementHeader.textViewHeader.text = getString(R.string.podcasts)
        }
    }

    private fun setClickListeners() {
        binding.apply {

            includeManageMediaElementHeader.textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
                }
            }

            includeDeleteNotification.apply {
                buttonDelete.setOnClickListener {
                    viewModel.deleteAllDownloadedFeeds()
                }
                buttonGotIt.setOnClickListener {
                    viewModel.deleteAllFeedsNotificationViewStateContainer.updateViewState(DeleteNotificationViewState.Closed)
                }
                buttonCancel.setOnClickListener {
                    viewModel.deleteAllFeedsNotificationViewStateContainer.updateViewState(DeleteNotificationViewState.Closed)
                }
            }

            includeManageMediaElementHeader.buttonHeaderDelete.setOnClickListener {
                viewModel.deleteAllFeedsNotificationViewStateContainer.updateViewState(DeleteNotificationViewState.Open)
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: DeletePodcastViewState) {
        @Exhaustive
        when (viewState) {
            is DeletePodcastViewState.Loading -> {}
            is DeletePodcastViewState.SectionList -> {

                binding.includeManageMediaElementHeader.apply {
                    constraintLayoutDeleteElementContainerTrash.visible
                    textViewManageStorageElementNumber.text = viewState.totalSizeAllSections
                }

                binding.textViewPodcastNoFound.goneIfFalse(viewState.section.isEmpty())
                binding.includeDeleteNotification.textViewDeleteDescription.text = getString(R.string.manage_storage_delete_description)

                if (viewState.section.isEmpty()) {
                    binding.includeManageMediaElementHeader.constraintLayoutDeleteElementContainerTrash.gone
                }
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.deleteAllFeedsNotificationViewStateContainer.collect { viewState ->

                binding.includeDeleteNotification.apply {
                    when (viewState) {
                        is DeleteNotificationViewState.Closed -> {
                            root.gone
                        }
                        is DeleteNotificationViewState.Open -> {
                            root.visible
                            constraintChooseDeleteContainer.visible
                            constraintDeleteProgressContainer.gone
                            constraintDeleteSuccessfullyContainer.gone
                        }
                        is DeleteNotificationViewState.Deleting -> {
                            root.visible
                            constraintChooseDeleteContainer.gone
                            constraintDeleteProgressContainer.visible
                            constraintDeleteSuccessfullyContainer.gone
                        }
                        is DeleteNotificationViewState.SuccessfullyDeleted -> {
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

                binding.includeDeleteNotification.textViewManageStorageFreeSpaceText.text =
                    String.format(
                        getString(R.string.manage_storage_deleted_free_space),
                        deleteSize.calculateSize()
                    )
            }
        }
        super.subscribeToViewStateFlow()
    }

    private fun setupMediaSectionAdapter() {
        binding.recyclerViewStorageElementList.apply {

            val mediaSectionAdapter = PodcastDeleteAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel
            )
            layoutManager = LinearLayoutManager(binding.root.context)
            adapter = mediaSectionAdapter
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: DeleteNotifySideEffect) {
        sideEffect.execute(binding.root.context)
    }
}
