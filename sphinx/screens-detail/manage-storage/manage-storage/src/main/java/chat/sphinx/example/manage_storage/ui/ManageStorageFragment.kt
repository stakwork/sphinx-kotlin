package chat.sphinx.example.manage_storage.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
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

    companion object {
        const val IMAGE_TYPE = "Images"
        const val VIDEO_TYPE = "Videos"
        const val AUDIO_TYPE = "Audios"
        const val FILE_TYPE = "Files"
    }

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
            when {
                (viewModel.changeStorageLimitViewStateContainer.value is ChangeStorageLimitViewState.Open) -> {
                    viewModel.changeStorageLimitViewStateContainer.updateViewState(
                        ChangeStorageLimitViewState.Closed
                    )
                }
                (viewModel.deleteItemNotificationViewStateContainer.value is DeleteTypeNotificationViewState.Open) -> {
                    viewModel.deleteItemNotificationViewStateContainer.updateViewState(
                        DeleteTypeNotificationViewState.Closed
                    )
                }
                else -> {
                    lifecycleScope.launch(viewModel.mainImmediate) {
                        viewModel.navigator.closeDetailScreen()
                    }
                }
            }
        }
    }

    private fun setUpHeader() {
        binding.apply {
            includeManageStorageHeader.textViewHeader.text = getString(R.string.manage_storage)
            includeManageStorageHeader.constraintLayoutDeleteElementContainerTrash.gone
            includeLayoutChangeLimit.includeLayoutChangeStorageLimitDetail.includeManageChangeLimitHeader.apply {
                textViewHeader.text = getString(R.string.manage_storage_limit)
                constraintLayoutDeleteElementContainerTrash.gone
            }
        }
    }

    private fun setClickListeners() {
        binding.apply {

            buttonChangeStorageLimit.setOnClickListener{
                viewModel.retrieveStorageLimitFromPreferences()
            }

            includeManageStorageHeader.textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
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
                viewModel.openDeleteTypePopUp(IMAGE_TYPE)
            }
            constraintLayoutStorageVideoContainer.setOnClickListener {
                viewModel.openDeleteTypePopUp(VIDEO_TYPE)
            }
            constraintLayoutStorageAudioContainer.setOnClickListener {
                viewModel.openDeleteTypePopUp(AUDIO_TYPE)
            }
            constraintLayoutStorageFilesContainer.setOnClickListener {
                viewModel.openDeleteTypePopUp(FILE_TYPE)
            }

            includeLayoutChangeLimit.includeLayoutChangeStorageLimitDetail.apply {
                includeManageChangeLimitHeader.textViewDetailScreenClose.setOnClickListener {
                    viewModel.changeStorageLimitViewStateContainer.updateViewState(
                        ChangeStorageLimitViewState.Closed
                    )
                }
                buttonCancel.setOnClickListener {
                    viewModel.changeStorageLimitViewStateContainer.updateViewState(
                        ChangeStorageLimitViewState.Closed
                    )
                }
                buttonSave.setOnClickListener {
                    viewModel.setStorageLimit(storageLimitSeekBar.progress)
                }
            }

            includeLayoutManageStorageDeleteNotification.includeLayoutManageStorageDeleteDetails.apply {
                buttonDelete.setOnClickListener {
                    (viewModel.deleteItemNotificationViewStateContainer.value as? DeleteTypeNotificationViewState.Open)?.type?.let { type ->
                        viewModel.deleteAllFilesByType(type)
                    }
                }
                buttonCancel.setOnClickListener {
                    viewModel.deleteItemNotificationViewStateContainer.updateViewState(DeleteTypeNotificationViewState.Closed)
                }
            }
        }
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
                            includeLayoutChangeStorageLimitDetail.apply {
                                setupStorageSeekBar()
                                storageLimitSeekBar.progress = viewState.storageLimit.seekBarProgress
                                textViewManageStorageUsedNumber.text = viewState.storageLimit.usedStorage
                                textViewManageStorageMax.text = viewState.storageLimit.freeStorage
                                textViewManageStorageOccupiedNumber.text = viewState.storageLimit.userStorageLimit ?: getString(R.string.manage_storage_zero_gb)
                                setChangeStorageLimitPercentage(includeProfileManageStorageBar.storageProgressUsed, viewState.storageLimit.progressBarPercentage)
                                handleUndersizedLimit(viewState.storageLimit.undersized)
                            }
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
        if (percentage == 0F) {
            view.gone
        }
        else {
            val constraintLayout = binding.includeLayoutManageStorageProgressBar.progressContainer
            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)
            constraintSet.constrainPercentWidth(view.id, percentage)
            constraintSet.applyTo(constraintLayout)
        }
    }

    private fun setChangeStorageLimitPercentage(view: View, percentage: Float) {
        binding.includeLayoutChangeLimit.includeLayoutChangeStorageLimitDetail.includeProfileManageStorageBar.apply {

            val constraintLayout = progressContainer
            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)
            constraintSet.constrainPercentWidth(view.id, percentage)
            constraintSet.applyTo(constraintLayout)

            storageProgressFree.gone
            storageProgressImages.gone
            storageProgressAudio.gone
            storageProgressFiles.gone
            storageProgressVideo.gone
        }
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

            buttonProfileTrashImages.visible
            buttonProfileTrashVideo.visible
            buttonProfileTrashAudio.visible
            buttonProfileTrashFiles.visible

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

    private fun setupStorageSeekBar(){
        binding.includeLayoutChangeLimit.includeLayoutChangeStorageLimitDetail.apply {
            storageLimitSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    viewModel.updateStorageLimitViewState(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }
            })
        }
    }

    private fun handleUndersizedLimit(undersized: String?) {
        binding.includeLayoutChangeLimit.includeLayoutChangeStorageLimitDetail.includeManageChangeLimitHeader.apply {
            if (undersized != null) {
                changeStorageHeaderContainer.gone
                changeStorageHeaderSaveLimitContainer.visible
                textViewWarningUndersized.text = String.format(getString(R.string.manage_storage_limit_warning), undersized)
            }
            else {
                changeStorageHeaderContainer.visible
                changeStorageHeaderSaveLimitContainer.gone
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: StorageNotifySideEffect) {
        sideEffect.execute(binding.root.context)
    }
}
