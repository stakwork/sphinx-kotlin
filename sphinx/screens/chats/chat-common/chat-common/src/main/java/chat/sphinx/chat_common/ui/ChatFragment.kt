package chat.sphinx.chat_common.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.annotation.LayoutRes
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
import chat.sphinx.chat_common.ui.viewstate.messageholder.SelectedMessageViewState
import chat.sphinx.chat_common.ui.viewstate.messageholder.setView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_repository_message.SendMessage
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.resources.setBackgroundRandomColor
import chat.sphinx.resources.setTextColorExt
import chat.sphinx.wrapper_chat.isTrue
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.unit
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
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

        // TODO: OnBackPress listener for if message item is selected, switch state to NONE

        val insetterActivity = (requireActivity() as InsetterActivity)
        setupFooter(insetterActivity)
        setupHeader(insetterActivity)
        setupSelectedMessage()
        setupRecyclerView()
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
            imageViewSelectedMessage.apply {
                setOnClickListener {
                    viewModel.updateSelectedMessageViewState(SelectedMessageViewState.None)
                }
            }
        }
        selectedMessageHolderBinding.includeMessageHolderBubble.root.setOnClickListener {
            viewModel
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
                        selectedMessageHolderBinding.apply {
                            root.y = viewState.holderYPos.value
                            setView(
                                lifecycleScope,
                                disposables,
                                viewModel.dispatchers,
                                imageLoader,
                                viewModel.imageLoaderDefaults,
                                viewState.recyclerViewWidth,
                                viewState.messageHolderViewState
                            )
                        }

                        selectedMessageBinding.apply {
                            root.visible
                            // TODO: Top/Bottom menu show
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
