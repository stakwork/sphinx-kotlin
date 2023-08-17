package chat.sphinx.example.delete_chat_media_detail.ui

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.delete.chat.media.detail.R
import chat.sphinx.delete.chat.media.detail.databinding.FragmentDeleteChatMediaDetailBinding
import chat.sphinx.example.delete_chat_media_detail.adapter.DeleteChatDetailFilesAdapter
import chat.sphinx.example.delete_chat_media_detail.adapter.DeleteChatDetailFooterAdapter
import chat.sphinx.example.delete_chat_media_detail.adapter.DeleteChatDetailsMediaGridAdapter
import chat.sphinx.example.delete_chat_media_detail.viewstate.DeleteChatDetailNotificationViewState
import chat.sphinx.example.delete_chat_media_detail.viewstate.DeleteChatItemsNotificationViewState
import chat.sphinx.example.delete_chat_media_detail.viewstate.DeleteChatMediaDetailViewState
import chat.sphinx.example.delete_chat_media_detail.viewstate.HeaderSelectionModeViewState
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import chat.sphinx.wrapper_common.calculateSize
import chat.sphinx.wrapper_message_media.MediaType
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class DeleteChatMediaDetailFragment: SideEffectDetailFragment<
        Context,
        DeleteNotifySideEffect,
        DeleteChatMediaDetailViewState,
        DeleteChatMediaDetailViewModel,
        FragmentDeleteChatMediaDetailBinding
        >(R.layout.fragment_delete_chat_media_detail)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val binding: FragmentDeleteChatMediaDetailBinding by viewBinding(FragmentDeleteChatMediaDetailBinding::bind)
    override val viewModel: DeleteChatMediaDetailViewModel by viewModels()

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var userColorsHelper: UserColorsHelper

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
        setupChatDeleteMediaAdapter()
        setUpTabs()
        setupChatDeleteFilesAdapter()

        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.deleteChatMediaDetail)
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
            if (viewModel.deleteChatNotificationViewStateContainer.value is DeleteChatDetailNotificationViewState.Open) {
                viewModel.deleteChatNotificationViewStateContainer.updateViewState(
                    DeleteChatDetailNotificationViewState.Closed
                )
            }
            if (viewModel.headerSelectionModeViewStateContainer.value is HeaderSelectionModeViewState.On) {
                viewModel.deselectAllItems()
            }
            if (viewModel.deleteChatItemsNotificationViewState.value is DeleteChatItemsNotificationViewState.Open) {
                viewModel.deleteChatItemsNotificationViewState.updateViewState(DeleteChatItemsNotificationViewState.Closed)
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
            includeManageMediaElementHeader.textViewHeader.text = getString(R.string.chats)
        }
    }

    private fun setClickListeners() {
        binding.apply {

            includeManageMediaElementHeader.apply {
                textViewDetailScreenClose.setOnClickListener {
                    lifecycleScope.launch(viewModel.mainImmediate) {
                        viewModel.navigator.popBackStack()
                    }
                }
                buttonHeaderDelete.setOnClickListener {
                    viewModel.deleteChatNotificationViewStateContainer.updateViewState(
                        DeleteChatDetailNotificationViewState.Open
                    )
                }
                textViewDetailScreenSelectionClose.setOnClickListener {
                    viewModel.deselectAllItems()
                }
                buttonDeleteContainer.setOnClickListener {
                    viewModel.deleteChatItemsNotificationViewState.updateViewState(
                        DeleteChatItemsNotificationViewState.Open
                    )
                }
            }

            includeDeleteNotification.apply {
                buttonDelete.setOnClickListener {
                    viewModel.deleteAllChatFiles()
                }
                buttonGotIt.setOnClickListener {
                    viewModel.deleteChatNotificationViewStateContainer.updateViewState(
                        DeleteChatDetailNotificationViewState.Closed)
                }
                buttonCancel.setOnClickListener {
                    viewModel.deleteChatNotificationViewStateContainer.updateViewState(DeleteChatDetailNotificationViewState.Closed)
                }
            }

            includeLayoutManageStorageDeleteNotification.apply {
                includeLayoutManageStorageDeleteDetails.buttonDelete.setOnClickListener {
                    viewModel.deleteSelectedFiles()
                }
                includeLayoutManageStorageDeleteDetails.buttonCancel.setOnClickListener {
                    viewModel.deleteChatItemsNotificationViewState.updateViewState(
                        DeleteChatItemsNotificationViewState.Closed
                    )
                }
            }
        }
    }

    private fun setUpTabs() {

        binding.apply {
            tabLayoutDeleteTabs.addTab(tabLayoutDeleteTabs.newTab().setText(getString(R.string.delete_chat_media)))
            tabLayoutDeleteTabs.addTab(tabLayoutDeleteTabs.newTab().setText(getString(R.string.delete_chat_files)))

            tabLayoutDeleteTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    when (tab.position) {
                        0 -> {
                            recyclerViewStorageMediaList.visible
                            recyclerViewStorageFilesList.gone

                            viewModel.deselectAllItems()
                        }

                        1 -> {
                            recyclerViewStorageMediaList.gone
                            recyclerViewStorageFilesList.visible

                            viewModel.deselectAllItems()
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {}

                override fun onTabReselected(tab: TabLayout.Tab) {}
            })

            recyclerViewStorageMediaList.visible
            recyclerViewStorageFilesList.gone
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: DeleteChatMediaDetailViewState) {
        @Exhaustive
        when (viewState) {
            is DeleteChatMediaDetailViewState.Loading -> {}
            is DeleteChatMediaDetailViewState.FileList -> {
                binding.includeManageMediaElementHeader.apply {
                    constraintLayoutDeleteElementContainerTrash.visible
                    textViewManageStorageElementNumber.text = viewState.totalSizeFiles
                }

                binding.includeDeleteNotification.textViewDeleteDescription.text = getString(R.string.manage_storage_delete_chats)
                binding.includeManageMediaElementHeader.constraintLayoutDeleteElementContainerTrash.goneIfFalse(viewState.files.isNotEmpty())
                binding.textViewFilesNoFound.goneIfFalse(viewState.files.isEmpty())
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.deleteChatNotificationViewStateContainer.collect { viewState ->
                binding.includeDeleteNotification.apply {
                    when (viewState) {
                        is DeleteChatDetailNotificationViewState.Closed -> {
                            root.gone
                        }
                        is DeleteChatDetailNotificationViewState.Open -> {
                            root.visible
                            constraintChooseDeleteContainer.visible
                            constraintDeleteProgressContainer.gone
                            constraintDeleteSuccessfullyContainer.gone
                        }
                        is DeleteChatDetailNotificationViewState.Deleting -> {
                            root.visible
                            constraintChooseDeleteContainer.gone
                            constraintDeleteProgressContainer.visible
                            constraintDeleteSuccessfullyContainer.gone
                        }
                        is DeleteChatDetailNotificationViewState.SuccessfullyDeleted -> {
                            root.visible
                            constraintChooseDeleteContainer.gone
                            constraintDeleteProgressContainer.gone
                            constraintDeleteSuccessfullyContainer.visible

                            binding.includeDeleteNotification.textViewManageStorageAllTypeText.text =
                                getString(R.string.manage_storage_deleted_all_files)
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.itemsTotalSizeStateFlow.collect { deletedSize ->
                binding.includeDeleteNotification.textViewManageStorageFreeSpaceText.text =
                    String.format(
                        getString(R.string.manage_storage_deleted_free_space),
                        deletedSize.calculateSize()
                    )
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.headerSelectionModeViewStateContainer.collect { viewState ->
                binding.includeManageMediaElementHeader.apply {
                    when(viewState) {
                        is HeaderSelectionModeViewState.Off -> {
                            changeStorageHeaderContainer.visible
                            changeStorageHeaderSelectionContainer.gone
                        }
                        is HeaderSelectionModeViewState.On -> {
                            imageViewDeleteSelectionIcon.visible
                            changeStorageHeaderSelectionContainer.visible
                            changeStorageHeaderContainer.gone
                            textViewSelectionHeader.text = viewState.itemsNumber
                            textViewManageStorageElementSelectionNumber.text = viewState.sizeToDelete
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.deleteChatItemsNotificationViewState.collect { viewState ->
                binding.includeLayoutManageStorageDeleteNotification.apply {
                    when (viewState) {
                        is DeleteChatItemsNotificationViewState.Closed -> {}
                        is DeleteChatItemsNotificationViewState.Open -> {
                            includeLayoutManageStorageDeleteDetails.textViewStorageDeleteHeader.text = getString(R.string.manage_storage_delete_selected_items)
                        }
                    }
                    root.setTransitionDuration(300)
                    viewState.transitionToEndSet(root)
                }
            }
        }
        super.subscribeToViewStateFlow()
    }

    private fun setupChatDeleteMediaAdapter() {
        val deleteChatFooterAdapter = DeleteChatDetailFooterAdapter(requireActivity() as InsetterActivity)

        binding.recyclerViewStorageMediaList.apply {
            val deleteChatMediaAdapter = DeleteChatDetailsMediaGridAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel
            )
            layoutManager = GridLayoutManager(binding.root.context, 3)
            adapter = ConcatAdapter(deleteChatMediaAdapter, deleteChatFooterAdapter)
        }
    }

    private fun setupChatDeleteFilesAdapter() {
        val deleteChatFooterAdapter = DeleteChatDetailFooterAdapter(requireActivity() as InsetterActivity)

        binding.recyclerViewStorageFilesList.apply {
            val deleteChatFilesAdapter = DeleteChatDetailFilesAdapter(
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel
            )
            layoutManager = LinearLayoutManager(binding.root.context)
            adapter = ConcatAdapter(deleteChatFilesAdapter, deleteChatFooterAdapter )
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: DeleteNotifySideEffect) {
        sideEffect.execute(binding.root.context)
    }
}

