package chat.sphinx.menu_bottom_profile_pic

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import chat.sphinx.menu_bottom.databinding.LayoutMenuBottomBinding
import chat.sphinx.menu_bottom.model.MenuBottomOption
import chat.sphinx.menu_bottom.ui.BottomMenu
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor

class BottomMenuPicture(
    fragment: Fragment,
    onStopSupervisor: OnStopSupervisor,
    private val pictureMenuViewModel: PictureMenuViewModel,
): BottomMenu(
    pictureMenuViewModel.pictureMenuHandler,
    onStopSupervisor,
    pictureMenuViewModel.pictureMenuHandler.viewStateContainer,
) {

    private val contentChooserContract: ActivityResultLauncher<String> =
        fragment.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            pictureMenuViewModel.pictureMenuHandler.updatePictureFromPhotoLibrary(uri)
        }

    fun initialize(
        @StringRes
        headerText: Int,

        binding: LayoutMenuBottomBinding,
        lifecycleOwner: LifecycleOwner
    ) {
        super.newBuilder(binding, lifecycleOwner)
            .setHeaderText(headerText)
            .setOptions(
                setOf(
                    MenuBottomOption(
                        text = R.string.bottom_menu_profile_pic_option_camera,
                        textColor = R.color.primaryBlueFontColor,
                        onClick = {
                            pictureMenuViewModel.pictureMenuHandler.updatePictureFromCamera()
                        }
                    ),
                    MenuBottomOption(
                        text = R.string.bottom_menu_profile_pic_option_photo_library,
                        textColor = R.color.primaryBlueFontColor,
                        onClick = {
                            contentChooserContract.launch("image/*")
                        }
                    )
                )
            )
            .build()
    }

    override fun newBuilder(
        binding: LayoutMenuBottomBinding,
        lifecycleOwner: LifecycleOwner
    ): Builder {
        throw IllegalStateException("Use the BottomMenuProfilePic.initialize method")
    }
}
