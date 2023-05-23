package chat.sphinx.example.manage_storage.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.manage.storage.R
import chat.sphinx.manage.storage.databinding.FragmentManageStorageBinding
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import chat.sphinx.share_qr_code.BottomMenuShareQRCode
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
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

        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.layoutConstraintManageStorage)

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

            textViewManageStorageDeleteTypeText.setTextColor(
                ContextCompat.getColorStateList(root.context, R.color.placeholderText)
            )
            textViewManageStorageVideoText.setTextColor(
                ContextCompat.getColorStateList(root.context, R.color.placeholderText)
            )
            textViewManageStorageAudioText.setTextColor(
                ContextCompat.getColorStateList(root.context, R.color.placeholderText)
            )
            constraintLayoutStorageCustomTypeContainer.gone
            textViewManageStorageImagesNumber.gone
            progressBarImages.visible
            buttonProfileTrashImages.gone
            buttonProfileTrashAudio.gone
            buttonProfileTrashVideo.gone


        }
    }

    override suspend fun onViewStateFlowCollect(viewState: ManageStorageViewState) {
        @Exhaustive
        when (viewState) {
            is ManageStorageViewState.Idle -> {}
            is ManageStorageViewState.Loading -> {
                loadingStorage()
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: StorageNotifySideEffect) {
        sideEffect.execute(binding.root.context)
    }
}
