package chat.sphinx.chat_group.ui

import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_common.databinding.*
import chat.sphinx.chat_common.ui.ChatFragment
import chat.sphinx.chat_common.navigation.ChatNavigator
import chat.sphinx.chat_group.R
import chat.sphinx.chat_group.databinding.FragmentChatGroupBinding
import chat.sphinx.chat_group.navigation.GroupChatNavigator
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_network_query_chat.model.PodcastDto
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class ChatGroupFragment: ChatFragment<
        FragmentChatGroupBinding,
        ChatGroupFragmentArgs,
        ChatGroupViewModel,
        >(R.layout.fragment_chat_group)
{
    override val binding: FragmentChatGroupBinding by viewBinding(FragmentChatGroupBinding::bind)
    override val footerBinding: LayoutChatFooterBinding by viewBinding(
        LayoutChatFooterBinding::bind, R.id.include_chat_group_footer
    )
    override val headerBinding: LayoutChatHeaderBinding by viewBinding(
        LayoutChatHeaderBinding::bind, R.id.include_chat_group_header
    )
    override val selectedMessageBinding: LayoutSelectedMessageBinding by viewBinding(
        LayoutSelectedMessageBinding::bind, R.id.include_chat_group_selected_message
    )
    override val selectedMessageHolderBinding: LayoutMessageHolderBinding by viewBinding(
        LayoutMessageHolderBinding::bind, R.id.include_layout_message_holder_selected_message
    )
    override val attachmentSendBinding: LayoutAttachmentSendPreviewBinding by viewBinding(
        LayoutAttachmentSendPreviewBinding::bind, R.id.include_chat_group_attachment_send_preview
    )

    override val recyclerView: RecyclerView
        get() = binding.recyclerViewMessages

    override val viewModel: ChatGroupViewModel by viewModels()

    @Inject
    protected lateinit var imageLoaderInj: ImageLoader<ImageView>
    override val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj

    @Inject
    protected lateinit var chatNavigatorInj: GroupChatNavigator
    override val chatNavigator: ChatNavigator
        get() = chatNavigatorInj
}
