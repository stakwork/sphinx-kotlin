package chat.sphinx.tribe

import androidx.lifecycle.LifecycleOwner
import chat.sphinx.menu_bottom.databinding.LayoutMenuBottomBinding
import chat.sphinx.menu_bottom.model.MenuBottomOption
import chat.sphinx.menu_bottom.ui.BottomMenu
import chat.sphinx.menu_bottom_tribe.R
import chat.sphinx.resources.getString
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.isTribeOwnedByAccount
import chat.sphinx.wrapper_contact.Contact
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor

class BottomMenuTribe(
    onStopSupervisor: OnStopSupervisor,
    private val tribeMenuViewModel: TribeMenuViewModel,
): BottomMenu(
    tribeMenuViewModel.dispatchers,
    onStopSupervisor,
    tribeMenuViewModel.tribeMenuHandler.viewStateContainer,
) {

    fun initialize(
        chat: Chat,
        accountOwner: Contact,
        binding: LayoutMenuBottomBinding,
        lifecycleOwner: LifecycleOwner
    ) {
        val menuBottomOptions = ArrayList<MenuBottomOption>()

        if (chat.isTribeOwnedByAccount(accountOwner.nodePubKey)) {
            menuBottomOptions.add(
                MenuBottomOption(
                    text = R.string.bottom_menu_tribe_option_share_tribe,
                    textColor = R.color.primaryBlueFontColor,
                    onClick = {
                        tribeMenuViewModel.shareTribe()
                    }
                )
            )

            menuBottomOptions.add(
                MenuBottomOption(
                    text = R.string.bottom_menu_tribe_option_edit_tribe,
                    textColor = R.color.primaryBlueFontColor,
                    onClick = {
                        tribeMenuViewModel.editTribe()
                    }
                )
            )

            menuBottomOptions.add(
                MenuBottomOption(
                    text = R.string.bottom_menu_tribe_option_add_member,
                    textColor = R.color.primaryBlueFontColor,
                    onClick = {
                        tribeMenuViewModel.addTribeMember()
                    }
                )
            )

            menuBottomOptions.add(
                MenuBottomOption(
                    text = R.string.bottom_menu_tribe_option_delete_tribe,
                    textColor = R.color.primaryRed,
                    onClick = {
                        tribeMenuViewModel.deleteTribe()
                    }
                )
            )
        } else {
            menuBottomOptions.add(
                MenuBottomOption(
                    text = R.string.bottom_menu_tribe_option_exit_tribe,
                    textColor = R.color.primaryRed,
                    onClick = {
                        tribeMenuViewModel.exitTribe()
                    }
                )
            )
        }

        super.newBuilder(binding, lifecycleOwner)
            .setHeaderText(binding.getString(R.string.bottom_menu_tribe_header_text))
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
