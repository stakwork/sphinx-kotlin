package chat.sphinx.chat_group.ui

import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_common.databinding.*
import chat.sphinx.chat_common.ui.ChatFragment
import chat.sphinx.chat_group.R
import chat.sphinx.chat_group.databinding.FragmentChatGroupBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.menu_bottom.databinding.LayoutMenuBottomBinding
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
    override val searchFooterBinding: LayoutChatSearchFooterBinding by viewBinding(
        LayoutChatSearchFooterBinding::bind, R.id.include_chat_group_search_footer
    )
    override val headerBinding: LayoutChatHeaderBinding by viewBinding(
        LayoutChatHeaderBinding::bind, R.id.include_chat_group_header
    )
    override val searchHeaderBinding: LayoutChatSearchHeaderBinding by viewBinding(
        LayoutChatSearchHeaderBinding::bind, R.id.include_chat_group_search_header
    )

    override val recordingAudioContainer: ConstraintLayout
        get() = binding.layoutConstraintRecordingAudioContainer

    override val recordingCircleBinding: LayoutChatRecordingCircleBinding by viewBinding(
        LayoutChatRecordingCircleBinding::bind, R.id.include_chat_recording_circle
    )
    override val replyingMessageBinding: LayoutMessageReplyBinding by viewBinding(
        LayoutMessageReplyBinding::bind, R.id.include_chat_group_message_reply
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

    override val attachmentFullscreenBinding: LayoutAttachmentFullscreenBinding by viewBinding(
        LayoutAttachmentFullscreenBinding::bind, R.id.include_chat_group_attachment_fullscreen
    )

    override val menuBinding: LayoutChatMenuBinding by viewBinding(
        LayoutChatMenuBinding::bind, R.id.include_chat_group_menu
    )

    override val callMenuBinding: LayoutMenuBottomBinding by viewBinding(
        LayoutMenuBottomBinding::bind, R.id.include_layout_menu_bottom_call
    )

    override val moreMenuBinding: LayoutMenuBottomBinding by viewBinding(
        LayoutMenuBottomBinding::bind, R.id.include_layout_menu_bottom_more
    )

    override val scrollDownButtonBinding: LayoutScrollDownButtonBinding by viewBinding(
        LayoutScrollDownButtonBinding::bind, R.id.include_chat_group_scroll_down
    )

    override val shimmerBinding: LayoutShimmerContainerBinding by viewBinding(
        LayoutShimmerContainerBinding::bind, R.id.include_chat_group_shimmer_container
    )

    override val pinHeaderBinding: LayoutChatPinedMessageHeaderBinding?
        get() = null

    override val threadOriginalMessageBinding: LayoutThreadOriginalMessageBinding?
        get() = null

    override val menuEnablePayments: Boolean
        get() = false

    override val recyclerView: RecyclerView
        get() = binding.recyclerViewMessages

    override val viewModel: ChatGroupViewModel by viewModels()

    @Inject
    @Suppress("ProtectedInFinal", "PropertyName")
    protected lateinit var _userColorsHelper: UserColorsHelper
    override val userColorsHelper: UserColorsHelper
        get() = _userColorsHelper

    @Inject
    @Suppress("ProtectedInFinal", "PropertyName")
    protected lateinit var _imageLoader: ImageLoader<ImageView>
    override val imageLoader: ImageLoader<ImageView>
        get() = _imageLoader
}
