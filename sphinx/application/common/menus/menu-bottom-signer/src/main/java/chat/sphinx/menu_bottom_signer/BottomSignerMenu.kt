package chat.sphinx.menu_bottom_signer

import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import chat.sphinx.menu_bottom.databinding.LayoutMenuBottomBinding
import chat.sphinx.menu_bottom.model.MenuBottomDismiss
import chat.sphinx.menu_bottom.model.MenuBottomOption
import chat.sphinx.menu_bottom.ui.BottomMenu
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.resources.getString
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor

class BottomSignerMenu(
    onStopSupervisor: OnStopSupervisor,
    private val signerMenuViewModel: SignerMenuViewModel,
) : BottomMenu(
    signerMenuViewModel.dispatchers,
    onStopSupervisor,
    signerMenuViewModel.signerMenuHandler.viewStateContainer,
) {

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
                        text = R.string.bottom_menu_signer_option_set_hardware,
                        textColor = R.color.primaryBlueFontColor,
                        onClick = {
                            signerMenuViewModel.setupHardwareSigner()
                            viewStateContainer.updateViewState(MenuBottomViewState.Closed)
                        }
                    ),
                    MenuBottomOption(
                        text = R.string.bottom_menu_signer_option_set_phone,
                        textColor = R.color.primaryBlueFontColor,
                        onClick = {
                            signerMenuViewModel.setupPhoneSigner()
                            viewStateContainer.updateViewState(MenuBottomViewState.Closed)
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
        throw IllegalStateException("Use the BottomSignerMenu.initialize method")
    }
}