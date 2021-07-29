package chat.sphinx.chat_contact.ui

import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_common.databinding.*
import chat.sphinx.chat_common.ui.ChatFragment
import chat.sphinx.chat_contact.R
import chat.sphinx.chat_contact.databinding.FragmentChatContactBinding
import chat.sphinx.concept_image_loader.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class ChatContactFragment: ChatFragment<
        FragmentChatContactBinding,
        ChatContactFragmentArgs,
        ChatContactViewModel,
        >(R.layout.fragment_chat_contact)
{
    override val binding: FragmentChatContactBinding by viewBinding(FragmentChatContactBinding::bind)
    override val footerBinding: LayoutChatFooterBinding by viewBinding(
        LayoutChatFooterBinding::bind, R.id.include_chat_contact_footer
    )
    override val headerBinding: LayoutChatHeaderBinding by viewBinding(
        LayoutChatHeaderBinding::bind, R.id.include_chat_contact_header
    )
    override val replyingMessageBinding: LayoutMessageReplyBinding by viewBinding(
        LayoutMessageReplyBinding::bind, R.id.include_chat_contact_message_reply
    )
    override val selectedMessageBinding: LayoutSelectedMessageBinding by viewBinding(
        LayoutSelectedMessageBinding::bind, R.id.include_chat_contact_selected_message
    )
    override val selectedMessageHolderBinding: LayoutMessageHolderBinding by viewBinding(
        LayoutMessageHolderBinding::bind, R.id.include_layout_message_holder_selected_message
    )
    override val attachmentSendBinding: LayoutAttachmentSendPreviewBinding by viewBinding(
        LayoutAttachmentSendPreviewBinding::bind, R.id.include_chat_contact_attachment_send_preview
    )
    override val menuBinding: LayoutChatMenuBinding by viewBinding(
        LayoutChatMenuBinding::bind, R.id.include_chat_contact_menu
    )

    override val memberRemovalBinding: LayoutMessageTypeGroupActionMemberRemovalBinding by viewBinding(
        LayoutMessageTypeGroupActionMemberRemovalBinding::bind, R.id.include_message_type_group_action_member_removal
    )

    override val menuEnablePayments: Boolean
        get() = true

    override val recyclerView: RecyclerView
        get() = binding.recyclerViewMessages

    override val viewModel: ChatContactViewModel by viewModels()

    @Inject
    protected lateinit var imageLoaderInj: ImageLoader<ImageView>
    override val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj
}
