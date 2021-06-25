package chat.sphinx.chat_contact.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_common.databinding.*
import chat.sphinx.chat_common.ui.ChatFragment
import chat.sphinx.chat_common.navigation.ChatNavigator
import chat.sphinx.chat_common.ui.viewstate.ActionsMenuViewState
import chat.sphinx.chat_contact.R
import chat.sphinx.chat_contact.databinding.FragmentChatContactBinding
import chat.sphinx.chat_contact.navigation.ContactChatNavigator
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.annotation.meta.Exhaustive
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
        LayoutChatFooterBinding::bind, R.id.include_chat_footer
    )
    override val headerBinding: LayoutChatHeaderBinding by viewBinding(
        LayoutChatHeaderBinding::bind, R.id.include_chat_header
    )
    override val menuBinding: LayoutChatActionsMenuBinding by viewBinding(
        LayoutChatActionsMenuBinding::bind, R.id.include_chat_actions_menu
    )
    override val selectedMessageBinding: LayoutSelectedMessageBinding by viewBinding(
        LayoutSelectedMessageBinding::bind, R.id.include_chat_selected_message
    )
    override val selectedMessageHolderBinding: LayoutMessageHolderBinding by viewBinding(
        LayoutMessageHolderBinding::bind, R.id.include_layout_message_holder_selected_message
    )
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
    }

    private fun setupHeader() {
        val insetterActivity = (requireActivity() as InsetterActivity)

        binding.layoutMotionChat.getConstraintSet(R.id.motion_scene_chat_menu_closed)?.let { constraintSet ->
            val height = constraintSet.getConstraint(R.id.include_chat_header).layout.mHeight
            constraintSet.constrainHeight(R.id.include_chat_header, height + insetterActivity.statusBarInsetHeight.top)
        }

        binding.layoutMotionChat.getConstraintSet(R.id.motion_scene_chat_menu_open)?.let { constraintSet ->
            val height = constraintSet.getConstraint(R.id.include_chat_header).layout.mHeight
            constraintSet.constrainHeight(R.id.include_chat_header, height + insetterActivity.statusBarInsetHeight.top)
        }
    }

    override fun goToPaymentSendScreen() {
        viewModel.goToPaymentSendScreen()
    }

    override suspend fun onViewStateFlowCollect(viewState: ActionsMenuViewState) {
        @Exhaustive
        when (viewState) {
            ActionsMenuViewState.Closed -> {
                binding.layoutMotionChat.setTransitionDuration(150)
            }
            ActionsMenuViewState.Open -> {
                binding.layoutMotionChat.setTransitionDuration(300)
            }
        }
        viewState.transitionToEndSet(binding.layoutMotionChat)
    }

    override fun getMotionLayouts(): Array<MotionLayout> {
        return arrayOf(binding.layoutMotionChat)
    }

    override fun onViewCreatedRestoreMotionScene(
        viewState: ActionsMenuViewState,
        binding: FragmentChatContactBinding
    ) {
        viewState.restoreMotionScene(binding.layoutMotionChat)
    }
}
