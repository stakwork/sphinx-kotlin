package chat.sphinx.menu_bottom_scanner

import androidx.lifecycle.LifecycleOwner
import chat.sphinx.menu_bottom.databinding.LayoutMenuBottomBinding
import chat.sphinx.menu_bottom.model.MenuBottomDismiss
import chat.sphinx.menu_bottom.model.MenuBottomOption
import chat.sphinx.menu_bottom.ui.BottomMenu
import chat.sphinx.resources.getString
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor

class BottomScannerMenu(
    onStopSupervisor: OnStopSupervisor,
    private val scannerMenuViewModel: ScannerMenuViewModel,
) : BottomMenu(
    scannerMenuViewModel.dispatchers,
    onStopSupervisor,
    scannerMenuViewModel.scannerMenuHandler.viewStateContainer,
) {

    fun initialize(
        binding: LayoutMenuBottomBinding,
        lifecycleOwner: LifecycleOwner
    ) {
        val menuBottomOptions = arrayListOf(
            MenuBottomOption(
                text = R.string.bottom_menu_scanner_option_set_create_contact,
                textColor = R.color.primaryBlueFontColor,
                onClick = {
                    scannerMenuViewModel.createContact()
                }
            ),
            MenuBottomOption(
                text = R.string.bottom_menu_scanner_option_set_direct_payment,
                textColor = R.color.primaryBlueFontColor,
                onClick = {
                    scannerMenuViewModel.sendDirectPayment()
                }
            )
        )

        val menuDismiss = MenuBottomDismiss(
            R.string.menu_bottom_cancel,
            R.color.primaryRed) {
            scannerMenuViewModel.scannerMenuDismiss()
        }

        super.newBuilder(binding, lifecycleOwner)
            .setHeaderText(binding.getString(R.string.bottom_menu_scanner_header_text))
            .setOptions(menuBottomOptions.toSet())
            .setDismissOption(menuDismiss)
            .build()
    }

    override fun newBuilder(
        binding: LayoutMenuBottomBinding,
        lifecycleOwner: LifecycleOwner
    ): Builder {
        throw IllegalStateException("Use the BottomScannerMenu.initialize method")
    }
}