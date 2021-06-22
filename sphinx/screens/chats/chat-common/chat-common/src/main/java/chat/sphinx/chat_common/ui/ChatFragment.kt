package chat.sphinx.chat_common.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.LayoutRes
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import app.cash.exhaustive.Exhaustive
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.adapters.MessageListAdapter
import chat.sphinx.chat_common.databinding.LayoutChatFooterBinding
import chat.sphinx.chat_common.databinding.LayoutChatHeaderBinding
import chat.sphinx.chat_common.databinding.LayoutMessageHolderBinding
import chat.sphinx.chat_common.databinding.LayoutSelectedMessageBinding
import chat.sphinx.chat_common.navigation.ChatNavigator
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_common.ui.viewstate.header.ChatHeaderFooterViewState
import chat.sphinx.chat_common.ui.viewstate.selected.SelectedMessageViewState
import chat.sphinx.chat_common.ui.viewstate.messageholder.setView
import chat.sphinx.chat_common.ui.viewstate.selected.MenuItemState
import chat.sphinx.chat_common.ui.viewstate.selected.setMenuColor
import chat.sphinx.chat_common.ui.viewstate.selected.setMenuItems
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_repository_message.SendMessage
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.resources.*
import chat.sphinx.wrapper_chat.isTrue
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.unit
import chat.sphinx.wrapper_view.Dp
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

abstract class ChatFragment<
        VB: ViewBinding,
        ARGS: NavArgs,
        VM: ChatViewModel<ARGS>,
        >(@LayoutRes layoutId: Int): SideEffectFragment<
        Context,
        ChatSideEffect,
        ChatHeaderFooterViewState,
        VM,
        VB
        >(layoutId)
{
    protected abstract val footerBinding: LayoutChatFooterBinding
    protected abstract val headerBinding: LayoutChatHeaderBinding
    protected abstract val selectedMessageBinding: LayoutSelectedMessageBinding
    protected abstract val selectedMessageHolderBinding: LayoutMessageHolderBinding
    protected abstract val recyclerView: RecyclerView

    protected abstract val imageLoader: ImageLoader<ImageView>

    protected abstract val chatNavigator: ChatNavigator

    private val sendMessageBuilder = SendMessage.Builder()

    private val disposables: ArrayList<Disposable> = ArrayList(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        SelectedMessageStateBackPressHandler(viewLifecycleOwner, requireActivity())

        val insetterActivity = (requireActivity() as InsetterActivity)
        setupFooter(insetterActivity)
        setupHeader(insetterActivity)
        setupSelectedMessage()
        setupRecyclerView()
    }

    private inner class SelectedMessageStateBackPressHandler(
        owner: LifecycleOwner,
        activity: FragmentActivity,
    ): OnBackPressedCallback(true) {

        init {
            activity.apply {
                onBackPressedDispatcher.addCallback(
                    owner,
                    this@SelectedMessageStateBackPressHandler
                )
            }
        }

        override fun handleOnBackPressed() {
            if (viewModel.getSelectedMessageViewStateFlow().value is SelectedMessageViewState.SelectedMessage) {
                viewModel.updateSelectedMessageViewState(SelectedMessageViewState.None)
            } else {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    chatNavigator.popBackStack()
                }
            }
        }
    }

    private fun setupFooter(insetterActivity: InsetterActivity) {
        footerBinding.apply {
            insetterActivity.addNavigationBarPadding(root)

            textViewChatFooterSend.setOnClickListener {

                sendMessageBuilder.setText(editTextChatFooter.text?.toString())

                viewModel.sendMessage(sendMessageBuilder)?.let {
                    // if it did not return null that means it was valid
                    sendMessageBuilder.clear()
                    editTextChatFooter.setText("")
                }
            }

            textViewChatFooterAttachment.setOnClickListener {
                lifecycleScope.launch {
                    editTextChatFooter.let { editText ->
                        binding.root.context.inputMethodManager?.let { imm ->
                            if (imm.isActive(editText)) {
                                imm.hideSoftInputFromWindow(editText.windowToken, 0)
                                delay(250L)
                            }
                        }
                        viewModel.shouldShowSendAttachmentMenu()
                    }
                }
            }
        }
    }

    private fun setupHeader(insetterActivity: InsetterActivity) {
        headerBinding.apply {
            insetterActivity.addStatusBarPadding(root)

            root.layoutParams.height =
                root.layoutParams.height + insetterActivity.statusBarInsetHeight.top
            root.requestLayout()

            imageViewChatHeaderMuted.setOnClickListener {
                viewModel.toggleChatMuted()
            }

            textViewChatHeaderNavBack.setOnClickListener {
                lifecycleScope.launch {
                    chatNavigator.popBackStack()
                }
            }
        }
    }

    private fun setupSelectedMessage() {
        selectedMessageBinding.apply {
            imageViewSelectedMessage.setOnClickListener {
                viewModel.updateSelectedMessageViewState(SelectedMessageViewState.None)
            }

            includeLayoutSelectedMessageMenu.apply {
                includeLayoutSelectedMessageMenuItem1.root.setOnClickListener {
                    onSelectedMessageMenuItemClick(0)
                }
                includeLayoutSelectedMessageMenuItem2.root.setOnClickListener {
                    onSelectedMessageMenuItemClick(1)
                }
                includeLayoutSelectedMessageMenuItem3.root.setOnClickListener {
                    onSelectedMessageMenuItemClick(2)
                }
                includeLayoutSelectedMessageMenuItem4.root.setOnClickListener {
                    onSelectedMessageMenuItemClick(3)
                }
            }
        }
        selectedMessageHolderBinding.includeMessageHolderBubble.root.setOnClickListener {
            viewModel
        }
    }

    private fun onSelectedMessageMenuItemClick(index: Int) {
        viewModel.getSelectedMessageViewStateFlow().value.let { state ->
            if (state is SelectedMessageViewState.SelectedMessage) {
                state.messageHolderViewState.let { holderState ->
                    holderState.selectionMenuItems?.elementAtOrNull(index)?.let { item ->
                        when (item) {
                            is MenuItemState.Boost -> {
                                viewModel.boostMessage(holderState.message.uuid)
                            }
                            is MenuItemState.CopyCallLink -> {
                                // TODO: Implement
                            }
                            is MenuItemState.CopyLink -> {
                                // TODO: Implement
                            }
                            is MenuItemState.CopyText -> {
                                // TODO: Implement
                            }
                            is MenuItemState.Delete -> {
                                // TODO: Implement
                            }
                            is MenuItemState.Reply -> {
                                // TODO: Implement
                            }
                            is MenuItemState.SaveFile -> {
                                // TODO: Implement
                            }
                        }
                    }
                }

                viewModel.updateSelectedMessageViewState(SelectedMessageViewState.None)
            }
        }
    }

    private fun setupRecyclerView() {
        val linearLayoutManager = LinearLayoutManager(binding.root.context)
        val messageListAdapter = MessageListAdapter(
            recyclerView,
            headerBinding,
            linearLayoutManager,
            viewLifecycleOwner,
            onStopSupervisor,
            viewModel,
            imageLoader
        )
        recyclerView.apply {
            setHasFixedSize(false)
            layoutManager = linearLayoutManager
            adapter = messageListAdapter
            itemAnimator = null
        }
    }

    protected fun scrollToBottom(callback: () -> Unit) {
        (recyclerView.adapter as MessageListAdapter<*>).scrollToBottomIfNeeded(callback)
    }

    override fun onStart() {
        super.onStart()
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.headerInitialHolderSharedFlow.collect { viewState ->

                headerBinding.layoutChatInitialHolder.apply {
                    @Exhaustive
                    when (viewState) {
                        is InitialHolderViewState.Initials -> {
                            imageViewChatPicture.gone
                            textViewInitials.apply {
                                visible
                                text = viewState.initials
                                setBackgroundRandomColor(
                                    R.drawable.chat_initials_circle,
                                    viewState.color,
                                )
                            }

                        }
                        is InitialHolderViewState.None -> {
                            textViewInitials.gone
                            imageViewChatPicture.visible
                            imageLoader.load(
                                imageViewChatPicture,
                                R.drawable.ic_profile_avatar_circle,
                            )
                        }
                        is InitialHolderViewState.Url -> {
                            textViewInitials.gone
                            imageViewChatPicture.visible
                            imageLoader.load(
                                imageViewChatPicture,
                                viewState.photoUrl.value,
                                viewModel.imageLoaderDefaults,
                            )
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.checkRoute.collect { loadResponse ->
                headerBinding.textViewChatHeaderConnectivity.apply {
                    @Exhaustive
                    when (loadResponse) {
                        is LoadResponse.Loading -> {
                            setTextColorExt(R.color.washedOutReceivedText)
                        }
                        is Response.Error -> {
                            setTextColorExt(R.color.sphinxOrange)
                        }
                        is Response.Success -> {
                            val colorRes = if (loadResponse.value) {
                                R.color.primaryGreen
                            } else {
                                R.color.sphinxOrange
                            }

                            setTextColorExt(colorRes)
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.getSelectedMessageViewStateFlow().collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is SelectedMessageViewState.None -> {
                        selectedMessageBinding.root.gone
                    }
                    is SelectedMessageViewState.SelectedMessage -> {
                        if (viewState.messageHolderViewState.selectionMenuItems.isNullOrEmpty()) {
                            viewModel.updateSelectedMessageViewState(SelectedMessageViewState.None)
                            return@collect
                        }

                        selectedMessageHolderBinding.apply {
                            root.y = viewState.holderYPos.value + viewState.statusHeaderHeight.value
                            setView(
                                lifecycleScope,
                                disposables,
                                viewModel.dispatchers,
                                imageLoader,
                                viewModel.imageLoaderDefaults,
                                viewState.recyclerViewWidth,
                                viewState.messageHolderViewState
                            )
                            includeMessageStatusHeader.root.gone
                            includeMessageHolderChatImageInitialHolder.root.gone
                        }

                        selectedMessageBinding.apply message@ {

                            val screenshot = binding.root.takeScreenshot()
                            imageViewSelectedMessageBlur.setImageBitmap(screenshot.blur(root.context, 25.0f))

                            this@message.includeLayoutSelectedMessageMenu.apply {
                                spaceSelectedMessageMenuArrowTop.goneIfFalse(!viewState.showMenuTop)
                                imageViewSelectedMessageMenuArrowTop.goneIfFalse(!viewState.showMenuTop)

                                spaceSelectedMessageMenuArrowBottom.goneIfFalse(viewState.showMenuTop)
                                imageViewSelectedMessageMenuArrowBottom.goneIfFalse(viewState.showMenuTop)
                            }

                            this@message.includeLayoutSelectedMessageMenu.root.apply menu@ {

                                this@menu.y = if (viewState.showMenuTop) {
                                    viewState.holderYPos.value -
                                    (resources.getDimension(R.dimen.selected_message_menu_item_height) * (viewState.messageHolderViewState.selectionMenuItems?.size ?: 0)) +
                                    viewState.statusHeaderHeight.value -
                                    Dp(10F).toPx(context).value
                                } else {
                                    viewState.holderYPos.value          +
                                    viewState.bubbleHeight.value        +
                                    viewState.statusHeaderHeight.value  +
                                    Dp(10F).toPx(context).value
                                }
                                val menuWidth = resources.getDimension(R.dimen.selected_message_menu_width)

                                // TODO: Handle small bubbles better
                                this@menu.x = viewState.bubbleCenterXPos.value - (menuWidth / 2F)
                            }
                            this@message.setMenuColor(viewState.messageHolderViewState)
                            this@message.setMenuItems(viewState.messageHolderViewState.selectionMenuItems)
                            this@message.root.visible
                        }
                    }
                }
            }
        }

        viewModel.readMessages()
    }

    override suspend fun onViewStateFlowCollect(viewState: ChatHeaderFooterViewState) {
        @Exhaustive
        when (viewState) {
            is ChatHeaderFooterViewState.Idle -> {}
            is ChatHeaderFooterViewState.Initialized -> {
                headerBinding.apply {

                    textViewChatHeaderName.text = viewState.chatHeaderName
                    textViewChatHeaderLock.goneIfFalse(viewState.showLock)

                    viewState.contributions?.let {
                        imageViewChatHeaderContributions.visible
                        textViewChatHeaderContributions.apply {
                            visible
                            @SuppressLint("SetTextI18n")
                            text = getString(R.string.chat_tribe_contributions) + " ${it.asFormattedString()} ${it.unit}"
                        }
                    } ?: let {
                        imageViewChatHeaderContributions.gone
                        textViewChatHeaderContributions.gone
                    }

                    imageViewChatHeaderMuted.apply {
                        viewState.isMuted?.let { muted ->
                            if (muted.isTrue()) {
                                imageLoader.load(
                                    headerBinding.imageViewChatHeaderMuted,
                                    R.drawable.ic_baseline_notifications_off_24
                                )
                            } else {
                                imageLoader.load(
                                    headerBinding.imageViewChatHeaderMuted,
                                    R.drawable.ic_baseline_notifications_24
                                )
                            }
                        } ?: gone
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.readMessages()
    }

    override suspend fun onSideEffectCollect(sideEffect: ChatSideEffect) {
        sideEffect.execute(binding.root.context)
    }
}
