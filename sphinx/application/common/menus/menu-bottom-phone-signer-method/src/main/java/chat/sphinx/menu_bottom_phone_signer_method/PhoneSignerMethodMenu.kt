package chat.sphinx.menu_bottom_phone_signer_method

import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import chat.sphinx.menu_bottom.databinding.LayoutMenuBottomBinding
import chat.sphinx.menu_bottom.model.MenuBottomOption
import chat.sphinx.menu_bottom.ui.BottomMenu
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor

class PhoneSignerMethodMenu(
    onStopSupervisor: OnStopSupervisor,
    private val phoneSignerMethodMenuViewModel: PhoneSignerMethodMenuViewModel,
) : BottomMenu(
    phoneSignerMethodMenuViewModel.dispatchers,
    onStopSupervisor,
    phoneSignerMethodMenuViewModel.phoneSignerMethodMenuHandler.viewStateContainer,
) {

    fun initialize(
        @StringRes
        headerText: Int,

        binding: LayoutMenuBottomBinding,
        lifecycleOwner: LifecycleOwner
    ) {
        super.newBuilder(binding, lifecycleOwner)
            .setHeaderText(headerText)
            .setHeaderSubText(R.string.bottom_menu_phone_signer_method_sub_header_text)
            .setOptions(
                setOf(
                    MenuBottomOption(
                        text = R.string.bottom_menu_phone_signer_method_generate,
                        textColor = R.color.primaryBlueFontColor,
                        onClick = {
                            phoneSignerMethodMenuViewModel.generateSeed()
                            viewStateContainer.updateViewState(MenuBottomViewState.Closed)
                        }
                    ),
                    MenuBottomOption(
                        text = R.string.bottom_menu_phone_signer_method_import,
                        textColor = R.color.primaryBlueFontColor,
                        onClick = {
                            phoneSignerMethodMenuViewModel.importSeed()
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