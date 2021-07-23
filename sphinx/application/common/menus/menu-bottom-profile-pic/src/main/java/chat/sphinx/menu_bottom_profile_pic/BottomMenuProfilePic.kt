package chat.sphinx.menu_bottom_profile_pic

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import chat.sphinx.menu_bottom.databinding.LayoutMenuBottomBinding
import chat.sphinx.menu_bottom.model.MenuBottomOption
import chat.sphinx.menu_bottom.ui.BottomMenu
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor

class BottomMenuProfilePic(
    fragment: Fragment,
    onStopSupervisor: OnStopSupervisor,
    private val profilePicMenuViewModel: ProfilePicMenuViewModel,
): BottomMenu(
    profilePicMenuViewModel.dispatchers,
    onStopSupervisor,
    profilePicMenuViewModel.profilePicMenuHandler.viewStateContainer,
) {

    val contentChooserContract: ActivityResultLauncher<String> =
        fragment.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            profilePicMenuViewModel.profilePicMenuHandler.handleActivityResultUri(uri)
        }

    fun initialize(
        binding: LayoutMenuBottomBinding,
        lifecycleOwner: LifecycleOwner
    ) {
        super.newBuilder(binding, lifecycleOwner)
            .setHeaderText(R.string.bottom_menu_profile_pic_header_text)
            .setOptions(
                setOf(
                    MenuBottomOption(
                        text = R.string.bottom_menu_profile_pic_option_camera,
                        textColor = R.color.primaryBlueFontColor,
                        onClick = {
                            profilePicMenuViewModel.profilePicMenuHandler.updateProfilePicCamera()
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
