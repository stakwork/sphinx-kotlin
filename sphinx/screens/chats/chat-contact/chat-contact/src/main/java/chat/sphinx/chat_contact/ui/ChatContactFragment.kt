package chat.sphinx.chat_contact.ui

import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_common.databinding.LayoutChatFooterBinding
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
    override val viewModel: ChatContactViewModel by viewModels()

    override val header: ConstraintLayout
        get() = binding.layoutChatHeader.layoutConstraintChatHeader
    override val headerChatPicture: ImageView
        get() = binding.layoutChatHeader.layoutChatInitialHolder.imageViewChatPicture
    override val headerConnectivityIcon: TextView
        get() = binding.layoutChatHeader.textViewChatHeaderConnectivity
    override val headerInitials: TextView
        get() = binding.layoutChatHeader.layoutChatInitialHolder.textViewInitials
    override val headerLockIcon: TextView
        get() = binding.layoutChatHeader.textViewChatHeaderLock
    override val headerMute: ImageView
        get() = binding.layoutChatHeader.imageViewChatHeaderMuted
    override val headerName: TextView
        get() = binding.layoutChatHeader.textViewChatHeaderName
    override val headerNavBack: TextView
        get() = binding.layoutChatHeader.textViewChatHeaderNavBack

    override val footer: ConstraintLayout
        get() = binding.layoutChatFooter.layoutConstraintChatFooter

    override val recyclerView: RecyclerView
        get() = binding.recyclerViewMessages

    @Inject
    protected lateinit var imageLoaderInj: ImageLoader<ImageView>
    override val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj

    @Inject
    protected lateinit var chatNavigatorInj: ContactChatNavigator
    override val chatNavigator: ChatNavigator
        get() = chatNavigatorInj
}
