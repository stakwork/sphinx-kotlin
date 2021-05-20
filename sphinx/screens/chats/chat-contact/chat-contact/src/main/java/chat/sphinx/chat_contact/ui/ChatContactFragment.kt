package chat.sphinx.chat_contact.ui

import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_common.databinding.LayoutChatFooterBinding
import chat.sphinx.chat_common.databinding.LayoutChatHeaderBinding
import chat.sphinx.chat_common.ui.BaseChatFragment
import chat.sphinx.chat_common.navigation.ChatNavigator
import chat.sphinx.chat_contact.R
import chat.sphinx.chat_contact.databinding.FragmentChatContactBinding
import chat.sphinx.chat_contact.navigation.ContactChatNavigator
import chat.sphinx.concept_image_loader.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class ChatContactFragment: BaseChatFragment<
        FragmentChatContactBinding,
        ChatContactFragmentArgs,
        ChatContactViewModel,
        >(R.layout.fragment_chat_contact)
{
    override val binding: FragmentChatContactBinding by viewBinding(FragmentChatContactBinding::bind)
    override val footerBinding: LayoutChatFooterBinding by viewBinding(LayoutChatFooterBinding::bind, R.id.include_chat_contact_footer)
    override val headerBinding: LayoutChatHeaderBinding by viewBinding(LayoutChatHeaderBinding::bind, R.id.include_chat_contact_header)
    override val recyclerView: RecyclerView
        get() = binding.recyclerViewMessages

    override val viewModel: ChatContactViewModel by viewModels()

    @Inject
    protected lateinit var imageLoaderInj: ImageLoader<ImageView>
    override val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj

    @Inject
    protected lateinit var chatNavigatorInj: ContactChatNavigator
    override val chatNavigator: ChatNavigator
        get() = chatNavigatorInj
}
