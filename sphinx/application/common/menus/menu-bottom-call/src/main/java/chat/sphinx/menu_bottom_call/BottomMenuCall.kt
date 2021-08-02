package chat.sphinx.menu_bottom_call

import androidx.lifecycle.LifecycleOwner
import chat.sphinx.menu_bottom.databinding.LayoutMenuBottomBinding
import chat.sphinx.menu_bottom.model.MenuBottomOption
import chat.sphinx.menu_bottom.ui.BottomMenu
import chat.sphinx.resources.getString
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor

class BottomMenuCall(
    onStopSupervisor: OnStopSupervisor,
    private val callMenuViewModel: CallMenuViewModel,
): BottomMenu(
    callMenuViewModel.dispatchers,
    onStopSupervisor,
    callMenuViewModel.callMenuHandler.viewStateContainer,
) {

    fun initialize(
        binding: LayoutMenuBottomBinding,
        lifecycleOwner: LifecycleOwner
    ) {
        val menuBottomOptions = ArrayList<MenuBottomOption>()

        menuBottomOptions.add(
            MenuBottomOption(
                text = R.string.bottom_menu_call_option_audio,
                textColor = R.color.primaryBlueFontColor,
                onClick = {
                    callMenuViewModel.sendCallInvite(true)
                }
            )
        )

        menuBottomOptions.add(
            MenuBottomOption(
                text = R.string.bottom_menu_call_option_video_or_audio,
                textColor = R.color.primaryBlueFontColor,
                onClick = {
                    callMenuViewModel.sendCallInvite(false)
                }
            )
        )

        super.newBuilder(binding, lifecycleOwner)
            .setHeaderText(binding.getString(R.string.bottom_menu_call_header_text))
            .setOptions(
                menuBottomOptions.toSet()
            )
            .build()
    }

    override fun newBuilder(
        binding: LayoutMenuBottomBinding,
        lifecycleOwner: LifecycleOwner
    ): Builder {
        throw IllegalStateException("Use the BottomMenuCall.initialize method")
    }
}
