package chat.sphinx.chat_group.ui

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
import chat.sphinx.chat_group.R
import chat.sphinx.chat_group.databinding.FragmentChatGroupBinding
import chat.sphinx.chat_group.navigation.GroupChatNavigator
import chat.sphinx.chat_group.ui.viewstate.ChatGroupActionsMenuViewState
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_network_query_chat.model.PodcastDto
import chat.sphinx.insetter_activity.InsetterActivity
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_viewmodel.updateViewState
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class ChatGroupFragment: ChatFragment<
        FragmentChatGroupBinding,
        ChatGroupActionsMenuViewState,
        ChatGroupFragmentArgs,
        ChatGroupViewModel,
        >(R.layout.fragment_chat_group)
{
    override val binding: FragmentChatGroupBinding by viewBinding(FragmentChatGroupBinding::bind)
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

    override val viewModel: ChatGroupViewModel by viewModels()

    @Inject
    protected lateinit var imageLoaderInj: ImageLoader<ImageView>
    override val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj

    @Inject
    protected lateinit var chatNavigatorInj: GroupChatNavigator
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

    override suspend fun onViewStateFlowCollect(viewState: ChatGroupActionsMenuViewState) {
        @Exhaustive
        when (viewState) {
            ChatGroupActionsMenuViewState.Closed -> {
                binding.layoutMotionChat.setTransitionDuration(150)
            }
            ChatGroupActionsMenuViewState.Open -> {
                binding.layoutMotionChat.setTransitionDuration(300)
            }
        }
        viewState.transitionToEndSet(binding.layoutMotionChat)
    }

    override fun getMotionLayouts(): Array<MotionLayout> {
        return arrayOf(binding.layoutMotionChat)
    }

    override fun onViewCreatedRestoreMotionScene(
        viewState: ChatGroupActionsMenuViewState,
        binding: FragmentChatGroupBinding
    ) {
        viewState.restoreMotionScene(binding.layoutMotionChat)
    }

    override fun openActionsMenu() {
        viewModel.updateViewState(ChatGroupActionsMenuViewState.Open)
    }
}
