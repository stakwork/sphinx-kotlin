package chat.sphinx.chat_tribe.ui

import android.content.ContentValues.TAG
import android.util.Log
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_common.databinding.LayoutChatFooterBinding
import chat.sphinx.chat_common.databinding.LayoutChatHeaderBinding
import chat.sphinx.chat_common.ui.ChatFragment
import chat.sphinx.chat_common.navigation.ChatNavigator
import chat.sphinx.chat_tribe.R
import chat.sphinx.chat_tribe.databinding.FragmentChatTribeBinding
import chat.sphinx.chat_tribe.navigation.TribeChatNavigator
import chat.sphinx.concept_image_loader.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class ChatTribeFragment: ChatFragment<
        FragmentChatTribeBinding,
        ChatTribeFragmentArgs,
        ChatTribeViewModel,
        >(R.layout.fragment_chat_tribe)
{
    override val binding: FragmentChatTribeBinding by viewBinding(FragmentChatTribeBinding::bind)
    override val footerBinding: LayoutChatFooterBinding by viewBinding(LayoutChatFooterBinding::bind, R.id.include_chat_tribe_footer)
    override val headerBinding: LayoutChatHeaderBinding by viewBinding(LayoutChatHeaderBinding::bind, R.id.include_chat_tribe_header)
    override val recyclerView: RecyclerView
        get() = binding.recyclerViewMessages

    override val viewModel: ChatTribeViewModel by viewModels()

    @Inject
    protected lateinit var imageLoaderInj: ImageLoader<ImageView>
    override val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj

    @Inject
    protected lateinit var chatNavigatorInj: TribeChatNavigator
    override val chatNavigator: ChatNavigator
        get() = chatNavigatorInj

    override fun onStart() {
        super.onStart()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.updateTribeInfo().collect { chat ->
                Log.d(TAG, "Price per message: ${chat.pricePerMessage.toString()}")
                Log.d(TAG, "Amount to Stake: ${chat.escrowAmount.toString()}")
                Log.d(TAG, "Name: ${chat.name.toString()}")
                Log.d(TAG, "Img: ${chat.photoUrl.toString()}")
            }
        }
    }
}
