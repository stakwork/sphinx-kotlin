package chat.sphinx.example.manage_storage.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.example.manage_storage.viewstate.DeleteTypeNotificationViewState
import chat.sphinx.example.manage_storage.viewstate.ManageStorageViewState
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.manage.storage.R
import chat.sphinx.manage.storage.databinding.FragmentManageStorageBinding
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class ManageStorageFragment: SideEffectDetailFragment<
        Context,
        StorageNotifySideEffect,
        ManageStorageViewState,
        ManageStorageViewModel,
        FragmentManageStorageBinding
        >(R.layout.fragment_manage_storage)
{
    override val binding: FragmentManageStorageBinding by viewBinding(FragmentManageStorageBinding::bind)
    override val viewModel: ManageStorageViewModel by viewModels()


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

        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.layoutConstraintManageStorage)

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
            if (viewModel.changeStorageLimitViewStateContainer.value is ChangeStorageLimitViewState.Open) {
                viewModel.changeStorageLimitViewStateContainer.updateViewState(ChangeStorageLimitViewState.Closed)
            } else {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }
    }

    private fun setUpHeader() {
        binding.apply {
            includeManageStorageHeader.textViewHeader.text = getString(R.string.manage_storage)
            includeLayoutChangeLimit.includeLayoutChangeStorageLimitDetail.includeManageChangeLimitHeader.textViewHeader.text = getString(R.string.manage_storage_limit)
            includeManageStorageHeader.constraintLayoutDeleteElementContainerTrash.gone
        }
    }

    private fun setClickListeners() {
        binding.apply {
            buttonChangeStorageLimit.setOnClickListener{
                viewModel.featureNotImplementedToast()
//                viewModel.changeStorageLimitViewStateContainer.updateViewState(
//                    ChangeStorageLimitViewState.Open
//                )
            }
            includeManageStorageHeader.textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
            includeLayoutChangeLimit.includeLayoutChangeStorageLimitDetail.includeManageChangeLimitHeader.textViewDetailScreenClose.setOnClickListener {
                viewModel.changeStorageLimitViewStateContainer.updateViewState(ChangeStorageLimitViewState.Closed)
            }
            includeLayoutChangeLimit.includeLayoutChangeStorageLimitDetail.buttonCancel.setOnClickListener {
                viewModel.changeStorageLimitViewStateContainer.updateViewState(ChangeStorageLimitViewState.Closed)
            }
            constraintLayoutStorageCustomTypeContainer.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.toDeleteMediaDetail()
                }
            }
            constraintLayoutStorageChatTypeContainer.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.toDeleteChatMedia()
                }
            }

            constraintLayoutStorageImageContainer.setOnClickListener {
//                viewModel.openDeleteTypePopUp(getString(R.string.manage_storage_images))
            }
            constraintLayoutStorageVideoContainer.setOnClickListener {
//                viewModel.openDeleteTypePopUp(getString(R.string.manage_storage_video))
            }
            constraintLayoutStorageAudioContainer.setOnClickListener {
//                viewModel.openDeleteTypePopUp(getString(R.string.manage_storage_audio))
            }
            constraintLayoutStorageFilesContainer.setOnClickListener {
//                viewModel.openDeleteTypePopUp(getString(R.string.manage_storage_files))
            }
        }
    }

    override fun onResume() {
        viewModel.getStorageData()
        super.onResume()
    }

    override suspend fun onViewStateFlowCollect(viewState: ManageStorageViewState) {
        @Exhaustive
        when (viewState) {
            is ManageStorageViewState.Loading -> {
                loadingStorage()
            }
            is ManageStorageViewState.StorageInfo -> {
                bindStorageInfo(viewState)
                setProgressStorageBar(viewState)
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.changeStorageLimitViewStateContainer.collect { viewState ->
                binding.includeLayoutChangeLimit.apply {
                    when (viewState) {
                        is ChangeStorageLimitViewState.Open -> {
                            // bind al the data
                        }
                        else -> {}
                    }
                    root.setTransitionDuration(300)
                    viewState.transitionToEndSet(root)
                }
            }
        }
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.deleteItemNotificationViewStateContainer.collect { viewState ->
                binding.includeLayoutManageStorageDeleteNotification.apply {
                    when (viewState) {
                        is DeleteTypeNotificationViewState.Closed -> {}
                        is DeleteTypeNotificationViewState.Open -> {
                            includeLayoutManageStorageDeleteDetails.
                            textViewStorageDeleteHeader.text = String.format(getString(R.string.manage_storage_delete_item), viewState.type)
                        }
                    }
                    root.setTransitionDuration(300)
                    viewState.transitionToEndSet(root)
                }
            }
        }

        super.subscribeToViewStateFlow()
    }

    private fun setProgressStorageBar(viewState: ManageStorageViewState.StorageInfo) {
        binding.includeLayoutManageStorageProgressBar.apply {
            viewState.storagePercentage.apply {
                setViewSectionPercentage(storageProgressImages, image)
                setViewSectionPercentage(storageProgressAudio, audio)
                setViewSectionPercentage(storageProgressVideo, video)
                setViewSectionPercentage(storageProgressFiles, files)
                setViewSectionPercentage(storageProgressFree, freeStorage)
            }
        }
    }

    private fun setViewSectionPercentage(view: View, percentage: Float) {
        val constraintLayout = binding.includeLayoutManageStorageProgressBar.progressContainer
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.constrainPercentWidth(view.id, percentage)
        constraintSet.applyTo(constraintLayout)
    }

    private fun bindStorageInfo(viewState: ManageStorageViewState.StorageInfo) {
        binding.apply {
            textViewManageStorageOccupiedNumber.text = viewState.storageSize.usedStorage
            textViewManageStorageFreeNumber.text = String.format(getString(R.string.manage_storage_free_space), viewState.storageSize.freeStorage)
            textViewManageStorageImagesNumber.text = viewState.storageSize.image
            textViewManageStorageVideoNumber.text = viewState.storageSize.video
            textViewManageStorageAudioNumber.text = viewState.storageSize.audio
            textViewManageStorageFilesNumber.text = viewState.storageSize.files
            textViewManageStorageCustomChatNumber.text = viewState.storageSize.chats
            textViewManageStorageCustomPodcastNumber.text = viewState.storageSize.podcasts

            textViewManageStorageOccupiedNumber.visible
            textViewManageStorageFreeNumber.visible
            progressBarLoading.gone
            textViewLoading.gone
            buttonChangeStorageLimit.visible

            storageProgressPointImages.backgroundTintList =
                ContextCompat.getColorStateList(root.context, R.color.storageBarBlue)

            storageProgressPointVideo.backgroundTintList =
                ContextCompat.getColorStateList(root.context, R.color.storageBarPurple)

            storageProgressPointAudio.backgroundTintList =
                ContextCompat.getColorStateList(root.context, R.color.storageBarYellow)

            storageProgressPointFiles.backgroundTintList =
                ContextCompat.getColorStateList(root.context, R.color.primaryGreen)

            textViewManageStorageImagesText.setTextColor(
                ContextCompat.getColorStateList(root.context, R.color.primaryText)
            )
            textViewManageStorageVideoText.setTextColor(
                ContextCompat.getColorStateList(root.context, R.color.primaryText)
            )
            textViewManageStorageAudioText.setTextColor(
                ContextCompat.getColorStateList(root.context, R.color.primaryText)
            )
            textViewManageStorageFilesText.setTextColor(
                ContextCompat.getColorStateList(root.context, R.color.primaryText)
            )

            progressBarImages.gone
            progressBarAudio.gone
            progressBarVideo.gone
            progressBarFiles.gone

            buttonProfileTrashImages.gone
            buttonProfileTrashVideo.gone
            buttonProfileTrashAudio.gone
            buttonProfileTrashFiles.gone

            constraintLayoutStorageCustomTypeContainer.visible
        }
    }

    private fun loadingStorage() {
        binding.apply {
            textViewManageStorageOccupiedNumber.gone
            textViewManageStorageFreeNumber.gone
            buttonChangeStorageLimit.gone
            progressBarLoading.visible
            textViewLoading.visible
            buttonChangeStorageLimit.gone
            includeManageStorageHeader.constraintLayoutDeleteElementContainerTrash.gone

            storageProgressPointImages.backgroundTintList =
                ContextCompat.getColorStateList(root.context, R.color.placeholderText)

            storageProgressPointVideo.backgroundTintList =
                ContextCompat.getColorStateList(root.context, R.color.placeholderText)

            storageProgressPointAudio.backgroundTintList =
                ContextCompat.getColorStateList(root.context, R.color.placeholderText)

            storageProgressPointFiles.backgroundTintList =
                ContextCompat.getColorStateList(root.context, R.color.placeholderText)

            textViewManageStorageImagesText.setTextColor(
                ContextCompat.getColorStateList(root.context, R.color.placeholderText)
            )
            textViewManageStorageVideoText.setTextColor(
                ContextCompat.getColorStateList(root.context, R.color.placeholderText)
            )
            textViewManageStorageAudioText.setTextColor(
                ContextCompat.getColorStateList(root.context, R.color.placeholderText)
            )
            textViewManageStorageFilesText.setTextColor(
                ContextCompat.getColorStateList(root.context, R.color.placeholderText)
            )

            constraintLayoutStorageCustomTypeContainer.gone
            progressBarImages.visible
            progressBarAudio.visible
            progressBarVideo.visible
            progressBarFiles.visible

            buttonProfileTrashImages.gone
            buttonProfileTrashAudio.gone
            buttonProfileTrashVideo.gone
            buttonProfileTrashFiles.gone
        }
    }


    override suspend fun onSideEffectCollect(sideEffect: StorageNotifySideEffect) {
        sideEffect.execute(binding.root.context)
    }
}
