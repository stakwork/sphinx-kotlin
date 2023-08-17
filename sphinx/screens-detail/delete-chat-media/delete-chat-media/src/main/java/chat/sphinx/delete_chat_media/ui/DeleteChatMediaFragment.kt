package chat.sphinx.delete_chat_media.ui

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
import chat.sphinx.delete.chat.media.R
import chat.sphinx.delete.chat.media.databinding.FragmentDeleteChatMediaBinding
import chat.sphinx.delete_chat_media.adapter.DeleteChatAdapter
import chat.sphinx.delete_chat_media.adapter.DeleteChatFooterAdapter
import chat.sphinx.delete_chat_media.viewstate.DeleteChatMediaViewState
import chat.sphinx.delete_chat_media.viewstate.DeleteChatNotificationViewState
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
internal class DeleteChatMediaFragment: SideEffectDetailFragment<
        Context,
        DeleteNotifySideEffect,
        DeleteChatMediaViewState,
        DeleteChatMediaViewModel,
        FragmentDeleteChatMediaBinding
        >(R.layout.fragment_delete_chat_media)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val binding: FragmentDeleteChatMediaBinding by viewBinding(FragmentDeleteChatMediaBinding::bind)
    override val viewModel: DeleteChatMediaViewModel by viewModels()

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
        setupChatDeleteAdapter()

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
            if (viewModel.deleteChatNotificationViewStateContainer.value is DeleteChatNotificationViewState.Open) {
                viewModel.deleteChatNotificationViewStateContainer.updateViewState(DeleteChatNotificationViewState.Closed)
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
            includeManageMediaElementHeader.textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
                }
            }
            includeManageMediaElementHeader.buttonHeaderDelete.setOnClickListener {
                viewModel.deleteChatNotificationViewStateContainer.updateViewState(DeleteChatNotificationViewState.Open)
            }

            includeDeleteNotification.apply {
                buttonDelete.setOnClickListener {
                    viewModel.deleteAllChatFiles()
                }
                buttonGotIt.setOnClickListener {
                    viewModel.deleteChatNotificationViewStateContainer.updateViewState(
                        DeleteChatNotificationViewState.Closed)
                }
                buttonCancel.setOnClickListener {
                    viewModel.deleteChatNotificationViewStateContainer.updateViewState(DeleteChatNotificationViewState.Closed)
                }
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: DeleteChatMediaViewState) {
        @Exhaustive
        when (viewState) {
            is DeleteChatMediaViewState.Loading -> {}

            is DeleteChatMediaViewState.ChatList -> {

                binding.includeManageMediaElementHeader.apply {
                    constraintLayoutDeleteElementContainerTrash.visible
                    textViewManageStorageElementNumber.text = viewState.totalSizeChats
                    constraintLayoutDeleteElementContainerTrash.goneIfFalse(viewState.chats.isNotEmpty())
                }

                binding.textViewPodcastNoFound.goneIfFalse(viewState.chats.isEmpty())
                binding.includeDeleteNotification.textViewDeleteDescription.text = getString(R.string.manage_storage_delete_chats)
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.deleteChatNotificationViewStateContainer.collect { viewState ->

                binding.includeDeleteNotification.apply {
                    when (viewState) {
                        is DeleteChatNotificationViewState.Closed -> {
                            root.gone
                        }
                        is DeleteChatNotificationViewState.Open -> {
                            root.visible
                            constraintChooseDeleteContainer.visible
                            constraintDeleteProgressContainer.gone
                            constraintDeleteSuccessfullyContainer.gone
                        }
                        is DeleteChatNotificationViewState.Deleting -> {
                            root.visible
                            constraintChooseDeleteContainer.gone
                            constraintDeleteProgressContainer.visible
                            constraintDeleteSuccessfullyContainer.gone
                        }
                        is DeleteChatNotificationViewState.SuccessfullyDeleted -> {
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
        super.subscribeToViewStateFlow()
    }

    private fun setupChatDeleteAdapter() {
        val deleteChatFooterAdapter = DeleteChatFooterAdapter(requireActivity() as InsetterActivity)

        binding.recyclerViewStorageElementList.apply {
            val deleteChatAdapter = DeleteChatAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel,
                userColorsHelper
            )
            layoutManager = LinearLayoutManager(binding.root.context)
            adapter = ConcatAdapter(deleteChatAdapter, deleteChatFooterAdapter)
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: DeleteNotifySideEffect) {
        sideEffect.execute(binding.root.context)
    }
}
