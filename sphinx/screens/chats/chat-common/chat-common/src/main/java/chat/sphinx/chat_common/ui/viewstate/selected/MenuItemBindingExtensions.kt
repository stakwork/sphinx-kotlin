package chat.sphinx.chat_common.ui.viewstate.selected

import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import app.cash.exhaustive.Exhaustive
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.databinding.LayoutSelectedMessageBinding
import chat.sphinx.chat_common.databinding.LayoutSelectedMessageMenuItemBinding
import chat.sphinx.chat_common.ui.viewstate.messageholder.MessageHolderViewState
import chat.sphinx.resources.getString
import chat.sphinx.resources.setTextColorExt
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutSelectedMessageBinding.setMenuColor(viewState: MessageHolderViewState) {
    includeLayoutSelectedMessageMenu.apply {
        @Exhaustive
        when (viewState) {
            is MessageHolderViewState.Received -> {
                layoutConstraintSelectedMessageMenuItemContainer
                    .setBackgroundResource(R.drawable.background_selected_received_message_menu)
                imageViewSelectedMessageMenuArrowTopCenter
                    .setBackgroundResource(R.drawable.selected_received_message_top_arrow)
                imageViewSelectedMessageMenuArrowBottomCenter
                    .setBackgroundResource(R.drawable.selected_received_message_bottom_arrow)
                imageViewSelectedMessageMenuArrowTopRight
                    .setBackgroundResource(R.drawable.selected_received_message_top_arrow)
                imageViewSelectedMessageMenuArrowBottomRight
                    .setBackgroundResource(R.drawable.selected_received_message_bottom_arrow)
                imageViewSelectedMessageMenuArrowTopLeft
                    .setBackgroundResource(R.drawable.selected_received_message_top_arrow)
                imageViewSelectedMessageMenuArrowBottomLeft
                    .setBackgroundResource(R.drawable.selected_received_message_bottom_arrow)
            }
            is MessageHolderViewState.Sent -> {
                layoutConstraintSelectedMessageMenuItemContainer
                    .setBackgroundResource(R.drawable.background_selected_sent_message_menu)
                imageViewSelectedMessageMenuArrowTopCenter
                    .setBackgroundResource(R.drawable.selected_sent_message_top_arrow)
                imageViewSelectedMessageMenuArrowBottomCenter
                    .setBackgroundResource(R.drawable.selected_sent_message_bottom_arrow)
                imageViewSelectedMessageMenuArrowTopRight
                    .setBackgroundResource(R.drawable.selected_sent_message_top_arrow)
                imageViewSelectedMessageMenuArrowBottomRight
                    .setBackgroundResource(R.drawable.selected_sent_message_bottom_arrow)
                imageViewSelectedMessageMenuArrowTopLeft
                    .setBackgroundResource(R.drawable.selected_sent_message_top_arrow)
                imageViewSelectedMessageMenuArrowBottomLeft
                    .setBackgroundResource(R.drawable.selected_sent_message_bottom_arrow)
            }
            is MessageHolderViewState.Separator -> { }

            is MessageHolderViewState.ThreadHeader -> { }
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutSelectedMessageBinding.setMenuItems(items: List<MenuItemState>?) {
    includeLayoutSelectedMessageMenu.apply {
        val itemsSize = items?.size ?: 0
        includeLayoutSelectedMessageMenuItem1.setMenuItem(items?.elementAtOrNull(0), itemsSize == 1)
        includeLayoutSelectedMessageMenuItem2.setMenuItem(items?.elementAtOrNull(1), itemsSize == 2)
        includeLayoutSelectedMessageMenuItem3.setMenuItem(items?.elementAtOrNull(2), itemsSize == 3)
        includeLayoutSelectedMessageMenuItem4.setMenuItem(items?.elementAtOrNull(3), itemsSize == 4)
        includeLayoutSelectedMessageMenuItem5.setMenuItem(items?.elementAtOrNull(4), itemsSize == 5)
        includeLayoutSelectedMessageMenuItem6.setMenuItem(items?.elementAtOrNull(5), itemsSize == 6)
        includeLayoutSelectedMessageMenuItem7.setMenuItem(items?.elementAtOrNull(6), itemsSize == 7)
        includeLayoutSelectedMessageMenuItem8.setMenuItem(items?.elementAtOrNull(7), itemsSize == 8)
        includeLayoutSelectedMessageMenuItem9.setMenuItem(items?.elementAtOrNull(8), itemsSize == 9)
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutSelectedMessageMenuItemBinding.setMenuItem(item: MenuItemState?, isLastItem: Boolean) {
    if (item == null) {
        root.gone
    } else {
        root.visible

        imageViewSelectedMessageMenuItemDivider.goneIfFalse(!isLastItem)

        textViewSelectedMessageMenuItemIcon.goneIfFalse(item.showTextIcon)
        imageViewSelectedMessageMenuItemIcon.goneIfFalse(item.showImageIcon)

        textViewSelectedMessageMenuItemIcon.setTextColorExt(R.color.text)
        textViewSelectedMessageMenuItem.setTextColorExt(R.color.text)

        // TODO: Fix Material Icon setting (something's amiss with how they show up on screen)
        when (item) {
            is MenuItemState.Boost -> {
                imageViewSelectedMessageMenuItemIcon.setImageDrawable(
                    ContextCompat.getDrawable(root.context, R.drawable.ic_circular_boost_green)
                )
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
                    getString(R.string.selected_message_menu_item_copy_link)
            }
            is MenuItemState.CopyText -> {
                textViewSelectedMessageMenuItemIcon.text =
                    getString(R.string.material_icon_name_message_action_copy_text)
                textViewSelectedMessageMenuItem.text =
                    getString(R.string.selected_message_menu_item_copy_text)
            }
            is MenuItemState.Delete -> {
                textViewSelectedMessageMenuItemIcon.apply {
                    text = getString(R.string.material_icon_name_message_action_delete)
                    setTextColorExt(R.color.primaryRed)
                }
                textViewSelectedMessageMenuItem.apply {
                    text = getString(R.string.selected_message_menu_item_delete)
                    setTextColorExt(R.color.primaryRed)
                }
            }
            is MenuItemState.Reply -> {
                textViewSelectedMessageMenuItemIcon.text =
                    getString(R.string.material_icon_name_message_action_reply)
                textViewSelectedMessageMenuItem.text =
                    getString(R.string.selected_message_menu_item_reply)
            }
            is MenuItemState.SaveFile -> {
                textViewSelectedMessageMenuItemIcon.text =
                    getString(R.string.material_icon_name_message_action_save_file)
                textViewSelectedMessageMenuItem.text =
                    getString(R.string.selected_message_menu_item_save_file)
            }
            is MenuItemState.Resend -> {
                textViewSelectedMessageMenuItemIcon.text =
                    getString(R.string.material_icon_name_message_action_resend)
                textViewSelectedMessageMenuItem.text =
                    getString(R.string.selected_message_menu_item_resend)
            }
            is MenuItemState.Flag -> {
                textViewSelectedMessageMenuItemIcon.text =
                    getString(R.string.material_icon_name_message_action_flag)
                textViewSelectedMessageMenuItem.text =
                    getString(R.string.selected_message_menu_item_flag)
            }
            is MenuItemState.PinMessage -> {
                imageViewSelectedMessageMenuItemIcon.setImageDrawable(
                    ContextCompat.getDrawable(root.context, R.drawable.ic_pin)
                )
                textViewSelectedMessageMenuItem.text =
                    getString(R.string.selected_message_menu_item_pin)
            }
            is MenuItemState.UnpinMessage -> {
                imageViewSelectedMessageMenuItemIcon.setImageDrawable(
                    ContextCompat.getDrawable(root.context, R.drawable.ic_pin)
                )
                textViewSelectedMessageMenuItem.text =
                    getString(R.string.selected_message_menu_item_unpin)
            }
        }
    }
}
