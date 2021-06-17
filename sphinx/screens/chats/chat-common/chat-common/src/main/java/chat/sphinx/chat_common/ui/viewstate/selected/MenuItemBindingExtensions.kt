package chat.sphinx.chat_common.ui.viewstate.selected

import androidx.annotation.MainThread
import app.cash.exhaustive.Exhaustive
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.databinding.LayoutSelectedMessageBinding
import chat.sphinx.chat_common.databinding.LayoutSelectedMessageMenuItemBinding
import chat.sphinx.chat_common.ui.viewstate.messageholder.BubbleBackground
import chat.sphinx.chat_common.ui.viewstate.messageholder.MessageHolderViewState
import chat.sphinx.resources.getColor
import chat.sphinx.resources.getString
import chat.sphinx.resources.setBackgroundRandomColor
import chat.sphinx.resources.setTextColorExt
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutSelectedMessageBinding.setMenuColor(viewState: MessageHolderViewState) {
    includeLayoutSelectedMessageMenu.root.apply {
        @Exhaustive
        when (viewState) {
            is MessageHolderViewState.Received -> {
                setBackgroundColor(getColor(R.color.lightDivider))
            }
            is MessageHolderViewState.Sent -> {
                setBackgroundColor(getColor(R.color.primaryBlue))
            }
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutSelectedMessageBinding.setMenuItems(items: List<MenuItemState>?) {
    includeLayoutSelectedMessageMenu.apply {
        includeLayoutSelectedMessageMenuItem1.setMenuItem(items?.elementAtOrNull(0))
        includeLayoutSelectedMessageMenuItem2.setMenuItem(items?.elementAtOrNull(1))
        includeLayoutSelectedMessageMenuItem3.setMenuItem(items?.elementAtOrNull(2))
        includeLayoutSelectedMessageMenuItem4.setMenuItem(items?.elementAtOrNull(3))
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutSelectedMessageMenuItemBinding.setMenuItem(item: MenuItemState?) {
    if (item == null) {
        root.gone
    } else {
        root.visible

        textViewSelectedMessageMenuItemIcon.goneIfFalse(item.showTextIcon)
        imageViewSelectedMessageMenuItemIcon.goneIfFalse(item.showImageIcon)

        textViewSelectedMessageMenuItemIcon.setTextColorExt(R.color.text)
        textViewSelectedMessageMenuItem.setTextColorExt(R.color.text)

        // TODO: Fix Material Icon setting (something's amiss with how they show up on screen)
        when (item) {
            is MenuItemState.Boost -> {
                imageViewSelectedMessageMenuItemIcon.setBackgroundResource(R.drawable.ic_boost_green)
                textViewSelectedMessageMenuItem.text =
                    getString(R.string.selected_message_menu_item_boost)
            }
            is MenuItemState.CopyCallLink -> {
                textViewSelectedMessageMenuItemIcon.text =
                    getString(R.string.material_icon_name_message_action_copy_call_link)
                textViewSelectedMessageMenuItem.text =
                    getString(R.string.selected_message_menu_item_copy_call_link)
            }
            is MenuItemState.CopyLink -> {
                textViewSelectedMessageMenuItemIcon.text =
                    getString(R.string.material_icon_name_message_action_copy_link)
                textViewSelectedMessageMenuItem.text =
                    getString(R.string.selected_message_menu_item_copy_call_link)
            }
            is MenuItemState.CopyText -> {
                textViewSelectedMessageMenuItemIcon.text =
                    getString(R.string.material_icon_name_message_action_copy_text)
                textViewSelectedMessageMenuItem.text =
                    getString(R.string.selected_message_menu_item_copy_call_link)
            }
            is MenuItemState.Delete -> {
                textViewSelectedMessageMenuItemIcon.apply {
                    text = getString(R.string.material_icon_name_message_action_delete)
                    setTextColorExt(R.color.primaryRed)
                }
                textViewSelectedMessageMenuItem.apply {
                    text = getString(R.string.selected_message_menu_item_copy_call_link)
                    setTextColorExt(R.color.primaryRed)
                }
            }
            is MenuItemState.Reply -> {
                textViewSelectedMessageMenuItemIcon.text =
                    getString(R.string.material_icon_name_message_action_reply)
                textViewSelectedMessageMenuItem.text =
                    getString(R.string.selected_message_menu_item_copy_call_link)
            }
            is MenuItemState.SaveFile -> {
                textViewSelectedMessageMenuItemIcon.text =
                    getString(R.string.material_icon_name_message_action_save_file)
                textViewSelectedMessageMenuItem.text =
                    getString(R.string.selected_message_menu_item_copy_call_link)
            }
        }
    }
}
