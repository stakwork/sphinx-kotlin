package chat.sphinx.share_qr_code

import androidx.lifecycle.LifecycleOwner
import chat.sphinx.menu_bottom.databinding.LayoutMenuBottomBinding
import chat.sphinx.menu_bottom.model.MenuBottomOption
import chat.sphinx.menu_bottom.ui.BottomMenu
import chat.sphinx.resources.getString
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor

class BottomMenuShareQRCode(
    onStopSupervisor: OnStopSupervisor,
    private val shareQRCodeMenuViewModel: ShareQRCodeMenuViewModel,
): BottomMenu(
    shareQRCodeMenuViewModel.dispatchers,
    onStopSupervisor,
    shareQRCodeMenuViewModel.shareQRCodeMenuHandler.viewStateContainer,
) {

    fun initialize(
        binding: LayoutMenuBottomBinding,
        lifecycleOwner: LifecycleOwner
    ) {
        val menuBottomOptions = ArrayList<MenuBottomOption>()

        menuBottomOptions.add(
            MenuBottomOption(
                text = R.string.bottom_menu_share_qr_code_option_share_text,
                textColor = R.color.primaryBlueFontColor,
                onClick = {
                    binding.root.context.startActivity(shareQRCodeMenuViewModel.shareCodeThroughTextIntent())
                }
            )
        )

        menuBottomOptions.add(
            MenuBottomOption(
                text = R.string.bottom_menu_share_qr_code_option_save_and_share_image,
                textColor = R.color.primaryBlueFontColor,
                onClick = {
                    shareQRCodeMenuViewModel.shareCodeThroughImageIntent()?.let {
                        binding.root.context.startActivity(it)
                    }
                }
            )
        )

        super.newBuilder(binding, lifecycleOwner)
            .setHeaderText(binding.getString(R.string.bottom_menu_share_qr_code_header_text))
            .setOptions(
                menuBottomOptions.toSet()
            )
            .build()
    }

    override fun newBuilder(
        binding: LayoutMenuBottomBinding,
        lifecycleOwner: LifecycleOwner
    ): Builder {
        throw IllegalStateException("Use the BottomMenuTribe.initialize method")
    }
}
