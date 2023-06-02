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
import chat.sphinx.delete.media.databinding.FragmentDeleteMediaBinding
import chat.sphinx.example.delete_media.adapter.PodcastDeleteAdapter
import chat.sphinx.example.delete_media.viewstate.DeletePodcastViewState
import chat.sphinx.example.delete_media.viewstate.DeleteNotificationViewState
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class DeletePodcastFragment: SideEffectDetailFragment<
        Context,
        DeleteNotifySideEffect,
        DeletePodcastViewState,
        DeletePodcastViewModel,
        FragmentDeleteMediaBinding
        >(R.layout.fragment_delete_media)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val binding: FragmentDeleteMediaBinding by viewBinding(FragmentDeleteMediaBinding::bind)
    override val viewModel: DeletePodcastViewModel by viewModels()

    private var maxTotalSize: Int = 0

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
            includeLayoutDeleteNotificationScreen.apply {
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
            includeManageMediaElementHeader.constraintLayoutDeleteElementContainerTrash.setOnClickListener {
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
                binding.includeLayoutDeleteNotificationScreen.textViewDeleteDescription.text = getString(R.string.manage_storage_delete_description)
                binding.textViewPodcastNoFound.goneIfFalse(viewState.section.isEmpty())


                viewState.totalSizeAllSections?.let { allSectionsTotalSize ->
                    val totalSize = allSectionsTotalSize.split(" ")[0].toDouble().toInt()
                    if (totalSize > 0 && totalSize >= maxTotalSize) {
                        maxTotalSize = totalSize
                        binding.includeLayoutDeleteNotificationScreen.textViewManageStorageFreeSpaceText.text =
                            String.format(
                                getString(R.string.manage_storage_deleted_free_space),
                                viewState.totalSizeAllSections
                            )
                    }
                }

                if (viewState.section.isEmpty()) {
                    binding.includeManageMediaElementHeader.constraintLayoutDeleteElementContainerTrash.gone
                }
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.deleteAllFeedsNotificationViewStateContainer.collect { viewState ->
                binding.includeLayoutDeleteNotificationScreen.apply {
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
                            constraintChooseDeleteContainer.gone
                            constraintDeleteProgressContainer.visible
                            constraintDeleteSuccessfullyContainer.gone
                        }
                        is DeleteNotificationViewState.SuccessfullyDeleted -> {
                            constraintChooseDeleteContainer.gone
                            constraintDeleteProgressContainer.gone
                            constraintDeleteSuccessfullyContainer.visible
                        }
                    }
                }
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
