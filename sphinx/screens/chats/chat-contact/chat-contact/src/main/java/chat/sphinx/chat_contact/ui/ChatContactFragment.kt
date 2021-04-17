package chat.sphinx.chat_contact.ui

import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_common.BaseChatFragment
import chat.sphinx.chat_contact.R
import chat.sphinx.chat_contact.databinding.FragmentChatContactBinding
import chat.sphinx.concept_image_loader.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class ChatContactFragment: BaseChatFragment<
        FragmentChatContactBinding
        >(R.layout.fragment_chat_contact)
{
    override val binding: FragmentChatContactBinding by viewBinding(FragmentChatContactBinding::bind)

    override val header: ConstraintLayout
        get() = binding.layoutChatHeader.layoutConstraintChatHeader
    override val headerChatPicture: ImageView
        get() = binding.layoutChatHeader.layoutChatInitialHolder.imageViewChatPicture
    override val headerConnectivityIcon: ImageView
        get() = binding.layoutChatHeader.imageViewChatHeaderConnectivity
    override val headerInitials: TextView
        get() = binding.layoutChatHeader.layoutChatInitialHolder.textViewInitials
    override val headerLockIcon: ImageView
        get() = binding.layoutChatHeader.imageViewChatHeaderLock
    override val headerMute: ImageView
        get() = binding.layoutChatHeader.imageViewChatHeaderMuted
    override val headerName: TextView
        get() = binding.layoutChatHeader.textViewChatHeaderName

    override val footer: ConstraintLayout
        get() = binding.layoutChatFooter.layoutConstraintChatFooter

    @Inject
    protected lateinit var imageLoaderInj: ImageLoader<ImageView>

    override val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj
}
