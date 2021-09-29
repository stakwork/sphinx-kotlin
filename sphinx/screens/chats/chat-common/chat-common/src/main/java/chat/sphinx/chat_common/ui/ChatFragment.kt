package chat.sphinx.chat_common.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.LayoutRes
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavArgs
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import app.cash.exhaustive.Exhaustive
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.adapters.MessageListAdapter
import chat.sphinx.chat_common.adapters.MessageListFooterAdapter
import chat.sphinx.chat_common.databinding.*
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_common.ui.viewstate.attachment.AttachmentFullscreenViewState
import chat.sphinx.chat_common.ui.viewstate.attachment.AttachmentSendViewState
import chat.sphinx.chat_common.ui.viewstate.footer.FooterViewState
import chat.sphinx.chat_common.ui.viewstate.header.ChatHeaderViewState
import chat.sphinx.chat_common.ui.viewstate.menu.ChatMenuViewState
import chat.sphinx.chat_common.ui.viewstate.messageholder.setView
import chat.sphinx.chat_common.ui.viewstate.messageholder.toTimestamp
import chat.sphinx.chat_common.ui.viewstate.messagereply.MessageReplyViewState
import chat.sphinx.chat_common.ui.viewstate.selected.MenuItemState
import chat.sphinx.chat_common.ui.viewstate.selected.SelectedMessageViewState
import chat.sphinx.chat_common.ui.viewstate.selected.setMenuColor
import chat.sphinx.chat_common.ui.viewstate.selected.setMenuItems
import chat.sphinx.chat_common.ui.widgets.SlideToCancelImageView
import chat.sphinx.chat_common.ui.widgets.SphinxFullscreenImageView
import chat.sphinx.chat_common.util.AudioRecorderController
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_network_client_crypto.CryptoHeader
import chat.sphinx.concept_network_client_crypto.CryptoScheme
import chat.sphinx.concept_repository_message.model.AttachmentInfo
import chat.sphinx.concept_repository_message.model.SendMessage
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.menu_bottom.databinding.LayoutMenuBottomBinding
import chat.sphinx.menu_bottom.model.MenuBottomOption
import chat.sphinx.menu_bottom.ui.BottomMenu
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.resources.*
import chat.sphinx.wrapper_chat.isTrue
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_meme_server.headerKey
import chat.sphinx.wrapper_meme_server.headerValue
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_message_media.MediaType
import chat.sphinx.wrapper_message_media.isImage
import chat.sphinx.wrapper_message_media.isSphinxText
import chat.sphinx.wrapper_view.Dp
import io.matthewnelson.android_feature_screens.ui.motionlayout.MotionLayoutFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.goneIfTrue
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

abstract class ChatFragment<
        VB: ViewBinding,
        ARGS: NavArgs,
        VM: ChatViewModel<ARGS>
        >(@LayoutRes layoutId: Int): MotionLayoutFragment<
        Nothing,
        ChatSideEffectFragment,
        ChatSideEffect,
        ChatMenuViewState,
        VM,
        VB
        >(layoutId), ChatSideEffectFragment, SlideToCancelImageView.SlideToCancelListener
{
    protected abstract val footerBinding: LayoutChatFooterBinding
    protected abstract val headerBinding: LayoutChatHeaderBinding
    protected abstract val recordingCircleBinding: LayoutChatRecordingCircleBinding
    protected abstract val replyingMessageBinding: LayoutMessageReplyBinding
    protected abstract val selectedMessageBinding: LayoutSelectedMessageBinding
    protected abstract val selectedMessageHolderBinding: LayoutMessageHolderBinding
    protected abstract val attachmentSendBinding: LayoutAttachmentSendPreviewBinding
    protected abstract val attachmentFullscreenBinding: LayoutAttachmentFullscreenBinding
    protected abstract val menuBinding: LayoutChatMenuBinding
    protected abstract val callMenuBinding: LayoutMenuBottomBinding
    protected abstract val recyclerView: RecyclerView

    protected abstract val menuEnablePayments: Boolean

    protected abstract val userColorsHelper: UserColorsHelper
    protected abstract val imageLoader: ImageLoader<ImageView>

    private val sendMessageBuilder = SendMessage.Builder()

    private val holderJobs: ArrayList<Job> = ArrayList(11)
    private val disposables: ArrayList<Disposable> = ArrayList(4)

    override val chatFragmentContext: Context
        get() = binding.root.context

    override val chatFragmentWindow: Window?
        get() = activity?.window

    private val bottomMenuCall: BottomMenu by lazy(LazyThreadSafetyMode.NONE) {
        BottomMenu(
            viewModel.dispatchers,
            onStopSupervisor,
            viewModel.callMenuHandler
        )
    }

    override val contentChooserContract: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            viewModel.handleActivityResultUri(uri)
        }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            lifecycleScope.launch(viewModel.mainImmediate) {
                viewModel.submitSideEffect(
                    ChatSideEffect.Notify(getString(R.string.recording_permission_granted))
                )
            }
        } else {
            lifecycleScope.launch(viewModel.mainImmediate) {
                viewModel.submitSideEffect(
                    ChatSideEffect.Notify(getString(R.string.recording_permission_required))
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        SelectedMessageStateBackPressHandler(viewLifecycleOwner, requireActivity())

        val insetterActivity = (requireActivity() as InsetterActivity)
        setupMenu(insetterActivity)
        setupFooter(insetterActivity)
        setupAttachmentPriceView()
        setupHeader(insetterActivity)
        setupSelectedMessage()
        setupAttachmentSendPreview(insetterActivity)
        setupAttachmentFullscreen(insetterActivity)
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
            val attachmentSendViewState = viewModel.getAttachmentSendViewStateFlow().value
            val attachmentFullscreenViewState = viewModel.getAttachmentFullscreenViewStateFlow().value

            when {
                viewModel.currentViewState is ChatMenuViewState.Open -> {
                    viewModel.updateViewState(ChatMenuViewState.Closed)
                }
                attachmentFullscreenViewState is AttachmentFullscreenViewState.Fullscreen -> {
                    viewModel.updateAttachmentFullscreenViewState(AttachmentFullscreenViewState.Idle)
                }
                attachmentSendViewState is AttachmentSendViewState.Preview -> {
                    viewModel.updateAttachmentSendViewState(AttachmentSendViewState.Idle)
                    viewModel.updateFooterViewState(FooterViewState.Default)
                    viewModel.deleteUnsentAttachment(attachmentSendViewState)
                }
                attachmentSendViewState is AttachmentSendViewState.PreviewGiphy -> {
                    viewModel.updateAttachmentSendViewState(AttachmentSendViewState.Idle)
                    viewModel.updateFooterViewState(FooterViewState.Default)
                }
                viewModel.getSelectedMessageViewStateFlow().value is SelectedMessageViewState.SelectedMessage -> {
                    viewModel.updateSelectedMessageViewState(SelectedMessageViewState.None)
                }
                else -> {
                    lifecycleScope.launch(viewModel.mainImmediate) {
                        viewModel.chatNavigator.popBackStack()
                    }
                }
            }
        }
    }

    private fun setupMenu(insetterActivity: InsetterActivity) {
        menuBinding.includeLayoutChatMenuOptions.apply options@ {
            insetterActivity.addNavigationBarPadding(root)

            textViewMenuOptionCancel.setOnClickListener {
                viewModel.updateViewState(ChatMenuViewState.Closed)
            }

            this@options.root.setOnClickListener { viewModel }

            layoutConstraintMenuOptionCamera.setOnClickListener {
                viewModel.chatMenuOptionCamera()
            }

            layoutConstraintMenuOptionMediaLibrary.setOnClickListener {
                viewModel.chatMenuOptionMediaLibrary()
            }

            layoutConstraintMenuOptionGif.setOnClickListener {
                viewModel.chatMenuOptionGif(parentFragmentManager)
            }

            layoutConstraintMenuOptionFile.setOnClickListener {
                viewModel.chatMenuOptionFileLibrary()
            }

            layoutConstraintMenuOptionPaidMessage.setOnClickListener {
                viewModel.chatMenuOptionPaidMessage()
            }

            val alpha = if (menuEnablePayments) 1.0F else 0.4F
            layoutConstraintMenuOptionPaymentRequest.apply request@ {
                this@request.isEnabled = menuEnablePayments
                this@request.alpha = alpha
                this@request.setOnClickListener {
                    viewModel.chatMenuOptionPaymentRequest()
                }
            }

            layoutConstraintMenuOptionPaymentSend.apply send@ {
                this@send.isEnabled = menuEnablePayments
                this@send.alpha = alpha
                this@send.setOnClickListener {
                    viewModel.chatMenuOptionPaymentSend()
                }
            }
        }

        menuBinding.viewChatMenuInputLock.setOnClickListener {
            viewModel.updateViewState(ChatMenuViewState.Closed)
        }
    }

    private fun setupFooter(insetterActivity: InsetterActivity) {
        bottomMenuCall.newBuilder(callMenuBinding, viewLifecycleOwner)
            .setHeaderText(R.string.bottom_menu_call_header_text)
            .setOptions(
                setOf(
                    MenuBottomOption(
                        text = R.string.bottom_menu_call_option_audio,
                        textColor = R.color.primaryBlueFontColor,
                        onClick = {
                            viewModel.sendCallInvite(true)
                        }
                    ),
                    MenuBottomOption(
                        text = R.string.bottom_menu_call_option_video_or_audio,
                        textColor = R.color.primaryBlueFontColor,
                        onClick = {
                            viewModel.sendCallInvite(false)
                        }
                    )
                )
            ).build()

        callMenuBinding.apply {
            insetterActivity.addNavigationBarPadding(root)
        }

        recordingCircleBinding.apply {
            insetterActivity.addNavigationBarPadding(root)
        }

        footerBinding.apply {
            insetterActivity.addNavigationBarPadding(root)

            textViewChatFooterSend.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    sendMessageBuilder.setText(editTextChatFooter.text?.toString())

                    sendMessageBuilder.setMessagePrice(
                        attachmentSendBinding.editTextMessagePrice.text?.toString()?.toLongOrNull()?.toSat()
                    )

                    val attachmentViewState = viewModel.getAttachmentSendViewStateFlow().value

                    @Exhaustive
                    when (attachmentViewState) {
                        is AttachmentSendViewState.Idle -> {
                            sendMessageBuilder.setAttachmentInfo(null)
                        }
                        is AttachmentSendViewState.Preview -> {
                            if (attachmentViewState.type.isImage) {
                                attachmentViewState.file?.let { nnFile ->
                                    sendMessageBuilder.setAttachmentInfo(
                                        AttachmentInfo(
                                            file = nnFile,
                                            mediaType = attachmentViewState.type,
                                            isLocalFile = true,
                                        )
                                    )
                                }
                            } else if (attachmentViewState.type.isSphinxText) {

                                val text = attachmentViewState.paidMessage?.first ?: editTextChatFooter.text?.toString()

                                viewModel.createPaidMessageFile(text)?.let { file ->
                                    sendMessageBuilder.setAttachmentInfo(
                                        AttachmentInfo(
                                            file = file,
                                            mediaType = MediaType.Text,
                                            isLocalFile = true,
                                        )
                                    )
                                }
                            }
                        }
                        is AttachmentSendViewState.PreviewGiphy -> {
                            sendMessageBuilder.setGiphyData(attachmentViewState.giphyData)
                        }
                    }

                    viewModel.sendMessage(sendMessageBuilder)?.let {
                        // if it did not return null that means it was valid
                        if (attachmentViewState !is AttachmentSendViewState.Idle) {
                            viewModel.updateAttachmentSendViewState(AttachmentSendViewState.Idle)
                            viewModel.updateFooterViewState(FooterViewState.Default)
                        }

                        sendMessageBuilder.clear()
                        editTextChatFooter.setText("")
                        attachmentSendBinding.editTextMessagePrice.setText("")

                        viewModel.messageReplyViewStateContainer.updateViewState(MessageReplyViewState.ReplyingDismissed)
                    }
                }
            }

            imageViewChatFooterMicrophone.slideToCancelListener = this@ChatFragment
            imageViewChatFooterMicrophone.setOnLongClickListener {
                if (!viewModel.audioRecorderController.isRecording()) {
                    if (isRecordingPermissionsGranted()) {
                        startRecording()
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
                return@setOnLongClickListener true
            }
            textViewChatFooterAttachment.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    editTextChatFooter.let { editText ->
                        binding.root.context.inputMethodManager?.let { imm ->
                            if (imm.isActive(editText)) {
                                imm.hideSoftInputFromWindow(editText.windowToken, 0)
                                delay(250L)
                            }
                        }

                        viewModel.updateViewState(ChatMenuViewState.Open)
                    }
                }
            }

            editTextChatFooter.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    val sendAttachmentViewState = viewModel.getAttachmentSendViewStateFlow().value

                    if (sendAttachmentViewState is AttachmentSendViewState.Preview && sendAttachmentViewState.type.isSphinxText) {
                        s?.toString()?.let { text ->
                            val price = attachmentSendBinding.editTextMessagePrice.text?.toString()?.toLongOrNull() ?: 0

                            viewModel.updateAttachmentSendViewState(
                                AttachmentSendViewState.Preview(
                                    null,
                                    sendAttachmentViewState.type,
                                    Pair(text, price),
                                )
                            )
                        }
                    }
                }
            })

            editTextChatFooter.onCommitContentListener = viewModel.onIMEContent
            editTextChatFooter.addTextChangedListener { editable ->
                textViewChatFooterSend.goneIfTrue(editable.isNullOrEmpty())
                imageViewChatFooterMicrophone.goneIfFalse(editable.isNullOrEmpty())
            }
        }

        replyingMessageBinding.apply {
            textViewReplyClose.visible
            textViewReplyClose.setOnClickListener {
                viewModel.replyToMessage(null)
            }

            root.setBackgroundColor(getColor(R.color.headerBG))
        }
    }

    private fun setupAttachmentPriceView() {
        attachmentSendBinding.editTextMessagePrice.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val sendAttachmentViewState = viewModel.getAttachmentSendViewStateFlow().value

                if (sendAttachmentViewState is AttachmentSendViewState.Preview && sendAttachmentViewState.type.isSphinxText) {
                    footerBinding.editTextChatFooter.text?.toString()?.let { text ->
                        val price = s?.toString()?.toLongOrNull() ?: 0

                        viewModel.updateAttachmentSendViewState(
                            AttachmentSendViewState.Preview(
                                null,
                                sendAttachmentViewState.type,
                                Pair(text, price),
                            )
                        )
                    }
                }
            }
        })
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

            textViewChatHeaderPhone.setOnClickListener {
                viewModel.callMenuHandler.updateViewState(
                    MenuBottomViewState.Open
                )
            }

            textViewChatHeaderNavBack.setOnClickListener {
                lifecycleScope.launch {
                    viewModel.chatNavigator.popBackStack()
                }
            }

            layoutConstraintChatHeaderName.setOnClickListener {
                viewModel.goToChatDetailScreen()
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

    private fun setupAttachmentSendPreview(insetterActivity: InsetterActivity) {
        attachmentSendBinding.apply {

            root.setOnClickListener { viewModel }

            layoutConstraintChatAttachmentSendHeader.apply {
                insetterActivity.addStatusBarPadding(this)
                this.layoutParams.height = this.layoutParams.height + insetterActivity.statusBarInsetHeight.top
                this.requestLayout()
            }

            textViewAttachmentSendHeaderClose.setOnClickListener {
                editTextMessagePrice.setText("")

                val vs = viewModel.getAttachmentSendViewStateFlow().value
                if (vs is AttachmentSendViewState.Preview) {
                    viewModel.deleteUnsentAttachment(vs)
                    viewModel.updateFooterViewState(FooterViewState.Default)
                    viewModel.updateAttachmentSendViewState(AttachmentSendViewState.Idle)
                } else if (vs is AttachmentSendViewState.PreviewGiphy) {
                    viewModel.updateFooterViewState(FooterViewState.Default)
                    viewModel.updateAttachmentSendViewState(AttachmentSendViewState.Idle)
                }
            }
        }
    }

    private fun setupAttachmentFullscreen(insetterActivity: InsetterActivity) {
        attachmentFullscreenBinding.apply {

            root.setOnClickListener { viewModel }

            layoutConstraintAttachmentFullscreenHeader.apply {
                insetterActivity.addStatusBarPadding(this)
                this.layoutParams.height = this.layoutParams.height + insetterActivity.statusBarInsetHeight.top
                this.requestLayout()
            }

            textViewAttachmentFullscreenHeaderBack.setOnClickListener {
                viewModel.updateAttachmentFullscreenViewState(
                    AttachmentFullscreenViewState.Idle
                )
            }

            imageViewAttachmentFullscreen.onSingleTapListener = object: SphinxFullscreenImageView.OnSingleTapListener {
                override fun onSingleTapConfirmed() {
                    layoutConstraintAttachmentFullscreenHeader.goneIfTrue(
                        layoutConstraintAttachmentFullscreenHeader.isVisible
                    )
                }
            }

            imageViewAttachmentFullscreen.onCloseViewHandler = object: SphinxFullscreenImageView.OnCloseViewHandler() {
                override fun onCloseView() {
                    imageViewAttachmentFullscreen.animate()
                        .scaleY(0f)
                        .scaleX(0f)
                        .setDuration(200L)
                        .withEndAction {
                            viewModel.updateAttachmentFullscreenViewState(
                                AttachmentFullscreenViewState.Idle
                            )
                        }
                        .start()

                }
            }
        }
    }

    /**
     * Used to round scale Factor in an attempt to limit the jitter
     */
    private fun Float.rounded(): Float {
        return ((this*1000).toInt()/1000.0f)
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
                                viewModel.copyMessageLink(holderState.message)
                            }
                            is MenuItemState.CopyText -> {
                                viewModel.copyMessageText(holderState.message)
                            }
                            is MenuItemState.Delete -> {
                                viewModel.deleteMessage(holderState.message)
                            }
                            is MenuItemState.Reply -> {
                                viewModel.replyToMessage(holderState.message)
                            }
                            is MenuItemState.SaveFile -> {
                                viewModel.saveFile(
                                    holderState.message,
                                    selectedMessageBinding.includeLayoutMessageHolderSelectedMessage.includeMessageHolderBubble.includeMessageTypeImageAttachment.imageViewAttachmentImage.drawable
                                )
                            }
                            is MenuItemState.Resend -> {
                                viewModel.resendMessage(holderState.message)
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
            imageLoader,
            userColorsHelper
        )
        val footerAdapter = MessageListFooterAdapter()
        recyclerView.apply {
            setHasFixedSize(false)
            layoutManager = linearLayoutManager
            adapter = ConcatAdapter(messageListAdapter, footerAdapter)
            itemAnimator = null
        }
    }

    private fun scrollToBottom(
        callback: () -> Unit,
        replyingToMessage: Boolean = false
    ) {
        (recyclerView.adapter as ConcatAdapter).adapters.firstOrNull()?.let { messagesListAdapter ->
            (messagesListAdapter as MessageListAdapter<*>).scrollToBottomIfNeeded(callback, replyingToMessage)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.readMessages()
    }

    override fun getMotionLayouts(): Array<MotionLayout> {
        return arrayOf(menuBinding.root)
    }

    override fun onViewCreatedRestoreMotionScene(viewState: ChatMenuViewState, binding: VB) {
        viewState.restoreMotionScene(menuBinding.root)
    }

    override suspend fun onViewStateFlowCollect(viewState: ChatMenuViewState) {
        @Exhaustive
        when (viewState) {
            is ChatMenuViewState.Closed -> {
                menuBinding.root.setTransitionDuration(250)
            }
            is ChatMenuViewState.Open -> {
                menuBinding.root.setTransitionDuration(400)
            }
        }

        viewState.transitionToEndSet(menuBinding.root)
    }

    private var messageReplyLastViewState: MessageReplyViewState? = null
    private val messageReplyViewStateJobs: MutableList<Job> = ArrayList(1)
    private val messageReplyViewStateDisposables: MutableList<Disposable> = ArrayList(1)

    private var headerInitialHolderLastViewState: InitialHolderViewState? = null
    private val headerInitialHolderViewStateJobs: MutableList<Job> = ArrayList(1)
    private val headerInitialHolderViewStateDisposables: MutableList<Disposable> = ArrayList(1)

    private var attachmentSendLastViewState: AttachmentSendViewState? = null
    private val attachmentSendViewStateJobs: MutableList<Job> = ArrayList(1)
    private val attachmentSendViewStateDisposables: MutableList<Disposable> = ArrayList(1)

    private var fullscreenLastViewState: AttachmentFullscreenViewState? = null
    private val fullScreenViewStateJobs: MutableList<Job> = ArrayList(1)
    private val fullScreenViewStateDisposables: MutableList<Disposable> = ArrayList(1)

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.messageReplyViewStateContainer.collect { viewState ->
                messageReplyLastViewState?.let {
                    if (it == viewState) {
                        return@collect
                    }
                }

                messageReplyLastViewState = viewState

                for (job in messageReplyViewStateJobs) {
                    job.cancel()
                }
                messageReplyViewStateJobs.clear()
                for (disposable in messageReplyViewStateDisposables) {
                    disposable.dispose()
                }
                messageReplyViewStateDisposables.clear()

                @Exhaustive
                when (viewState) {
                    is MessageReplyViewState.ReplyingDismissed -> {
                        sendMessageBuilder.setReplyUUID(null)
                        replyingMessageBinding.root.gone
                    }

                    is MessageReplyViewState.ReplyingToMessage -> {
                        val message = viewState.message

                        message.uuid?.value?.toReplyUUID().let { uuid ->
                            sendMessageBuilder.setReplyUUID(uuid)

                            replyingMessageBinding.apply {

                                textViewReplyMessageLabel.apply {
                                    textViewReplyMessageLabel.gone
                                    textViewReplyTextOverlay.gone

                                    if (message.isAudioMessage) {
                                        textViewReplyMessageLabel.text = getString(R.string.media_type_label_audio)
                                        textViewReplyMessageLabel.visible

                                        textViewReplyTextOverlay.text = getString(R.string.material_icon_name_volume_up)
                                        textViewReplyTextOverlay.visible
                                    } else {
                                        message.retrieveTextToShow()?.let { messageText ->
                                            textViewReplyMessageLabel.text = messageText
                                            textViewReplyMessageLabel.goneIfFalse(messageText.isNotEmpty())
                                        }
                                    }
                                }

                                textViewReplySenderLabel.text = viewState.senderAlias

                                viewReplyBarLeading.setBackgroundColor(
                                    Color.parseColor(
                                        userColorsHelper.getHexCodeForKey(
                                            message.getColorKey(),
                                            root.context.getRandomHexCode(),
                                        )
                                    )
                                )

                                message.retrieveImageUrlAndMessageMedia()?.let { mediaData ->
                                    lifecycleScope.launch(viewModel.mainImmediate) {
                                        val options: ImageLoaderOptions? =
                                            if (mediaData.second != null) {
                                                val builder = ImageLoaderOptions.Builder()

                                                mediaData.second?.host?.let { host ->
                                                    viewModel.memeServerTokenHandler.retrieveAuthenticationToken(
                                                        host
                                                    )?.let { token ->
                                                        builder.addHeader(
                                                            token.headerKey,
                                                            token.headerValue
                                                        )

                                                        mediaData.second
                                                            ?.mediaKeyDecrypted
                                                            ?.value
                                                            ?.let { key ->
                                                                val header = CryptoHeader.Decrypt.Builder()
                                                                    .setScheme(CryptoScheme.Decrypt.JNCryptor)
                                                                    .setPassword(key)
                                                                    .build()

                                                                builder.addHeader(
                                                                    header.key,
                                                                    header.value
                                                                )
                                                            }
                                                    }
                                                }

                                                builder.build()
                                            } else {
                                                null
                                            }

                                        val disposable = imageLoader.load(
                                            imageViewReplyMediaImage,
                                            mediaData.first,
                                            options
                                        )
                                        messageReplyViewStateDisposables.add(disposable)
                                    }.let { job ->
                                        messageReplyViewStateJobs.add(job)
                                    }

                                    imageViewReplyMediaImage.visible

                                } ?: imageViewReplyMediaImage.gone

                                scrollToBottom(callback = {
                                    root.visible
                                }, true)
                            }
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.chatHeaderViewStateContainer.collect { viewState ->

                @Exhaustive
                when (viewState) {
                    is ChatHeaderViewState.Idle -> {}
                    is ChatHeaderViewState.Initialized -> {
                        headerBinding.apply {

                            textViewChatHeaderName.text = viewState.chatHeaderName
                            textViewChatHeaderLock.goneIfFalse(viewState.showLock)

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
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.headerInitialHolderSharedFlow.collect { viewState ->
                headerInitialHolderLastViewState?.let {
                    if (it == viewState) {
                        return@collect
                    }
                }

                headerInitialHolderLastViewState = viewState

                for (job in headerInitialHolderViewStateJobs) {
                    job.cancel()
                }
                headerInitialHolderViewStateJobs.clear()

                for (disposable in headerInitialHolderViewStateDisposables) {
                    disposable.dispose()
                }
                headerInitialHolderViewStateDisposables.clear()

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
                                    Color.parseColor(
                                        userColorsHelper.getHexCodeForKey(
                                            viewState.colorKey,
                                            root.context.getRandomHexCode(),
                                        )
                                    ),
                                )
                            }

                        }
                        is InitialHolderViewState.None -> {
                            textViewInitials.gone
                            imageViewChatPicture.visible
                            lifecycleScope.launch(viewModel.mainImmediate) {
                                val disposable = imageLoader.load(
                                    imageViewChatPicture,
                                    R.drawable.ic_profile_avatar_circle,
                                )
                                headerInitialHolderViewStateDisposables.add(disposable)
                            }.let { job ->
                                headerInitialHolderViewStateJobs.add(job)
                            }
                        }
                        is InitialHolderViewState.Url -> {
                            textViewInitials.gone
                            imageViewChatPicture.visible
                            lifecycleScope.launch(viewModel.mainImmediate) {
                                val disposable = imageLoader.load(
                                    imageViewChatPicture,
                                    viewState.photoUrl.value,
                                    viewModel.imageLoaderDefaults,
                                )
                                headerInitialHolderViewStateDisposables.add(disposable)
                            }.let { job ->
                                headerInitialHolderViewStateJobs.add(job)
                            }
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
                        selectedMessageBinding.apply {
                            root.gone
                            imageViewSelectedMessageBlur.setImageBitmap(null)
                        }
                    }
                    is SelectedMessageViewState.SelectedMessage -> {
                        if (viewState.messageHolderViewState.selectionMenuItems.isNullOrEmpty()) {
                            viewModel.updateSelectedMessageViewState(SelectedMessageViewState.None)
                            return@collect
                        }

                        selectedMessageHolderBinding.apply {
                            root.y = viewState.holderYPos.value

                            setView(
                                lifecycleScope,
                                holderJobs,
                                disposables,
                                viewModel.dispatchers,
                                viewModel.audioPlayerController,
                                imageLoader,
                                viewModel.imageLoaderDefaults,
                                viewModel.memeServerTokenHandler,
                                viewState.recyclerViewWidth,
                                viewState.messageHolderViewState,
                                userColorsHelper,
                            )
                            includeMessageStatusHeader.root.gone
                            includeMessageHolderChatImageInitialHolder.root.gone
                        }

                        selectedMessageBinding.apply message@ {

                            binding.root.takeScreenshot(
                                requireActivity().window,
                                bitmapCallback = { bitmap ->
                                    if (viewModel.getSelectedMessageViewStateFlow().value == viewState) {
                                        selectedMessageBinding
                                            .imageViewSelectedMessageBlur
                                            .setImageBitmap(bitmap.blur(root.context, 25.0F))

                                        this@message.root.visible
                                    }
                                },
                                errorCallback = {}
                            )

                            this@message.includeLayoutSelectedMessageMenu.apply {
                                spaceSelectedMessageMenuArrowTop.goneIfFalse(!viewState.showMenuTop)
                                layoutConstraintSelectedMessageMenuArrowTopContainer.goneIfFalse(!viewState.showMenuTop)

                                spaceSelectedMessageMenuArrowBottom.goneIfFalse(viewState.showMenuTop)
                                layoutConstraintSelectedMessageMenuArrowBottomContainer.goneIfFalse(viewState.showMenuTop)

                                imageViewSelectedMessageMenuArrowBottomCenter.gone
                                imageViewSelectedMessageMenuArrowBottomRight.gone
                                imageViewSelectedMessageMenuArrowBottomLeft.gone

                                imageViewSelectedMessageMenuArrowTopCenter.gone
                                imageViewSelectedMessageMenuArrowTopRight.gone
                                imageViewSelectedMessageMenuArrowTopLeft.gone
                            }

                            this@message.includeLayoutSelectedMessageMenu.apply menu@ {

                                this@menu.root.y = if (viewState.showMenuTop) {
                                    viewState.holderYPos.value -
                                            (resources.getDimension(R.dimen.selected_message_menu_item_height) * (viewState.messageHolderViewState.selectionMenuItems?.size ?: 0)) -
                                            Dp(10F).toPx(root.context).value
                                } else {
                                    viewState.holderYPos.value +
                                            viewState.bubbleHeight.value +
                                            Dp(10F).toPx(root.context).value
                                }

                                val menuWidth = resources.getDimension(R.dimen.selected_message_menu_width)
                                var menuXPos = viewState.bubbleCenterXPos.value - (menuWidth / 2F)

                                when {
                                    (menuXPos + menuWidth > viewState.recyclerViewWidth.value) -> {
                                        menuXPos = viewState.recyclerViewWidth.value - menuWidth - Dp(16F).toPx(root.context).value

                                        this@menu.imageViewSelectedMessageMenuArrowBottomRight.visible
                                        this@menu.imageViewSelectedMessageMenuArrowTopRight.visible
                                    }
                                    (menuXPos < 0F) -> {
                                        menuXPos = Dp(16F).toPx(root.context).value

                                        this@menu.imageViewSelectedMessageMenuArrowBottomLeft.visible
                                        this@menu.imageViewSelectedMessageMenuArrowTopLeft.visible
                                    }
                                    else -> {
                                        this@menu.imageViewSelectedMessageMenuArrowBottomCenter.visible
                                        this@menu.imageViewSelectedMessageMenuArrowTopCenter.visible
                                    }
                                }

                                this@menu.root.x = menuXPos
                            }
                            this@message.setMenuColor(viewState.messageHolderViewState)
                            this@message.setMenuItems(viewState.messageHolderViewState.selectionMenuItems)
                        }
                    }
                }
            }
        }.invokeOnCompletion {
            viewModel.updateSelectedMessageViewState(SelectedMessageViewState.None)
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.getFooterViewStateFlow().collect { viewState ->
                footerBinding.apply {
                    editTextChatFooter.hint = getString(viewState.hintTextStringId)

                    layoutConstraintChatFooterRecordingActions.goneIfFalse(viewState.recordingEnabled)
                    imageViewChatFooterMicrophone.goneIfFalse(viewState.showRecordAudioIcon)
                    textViewChatFooterSend.goneIfFalse(viewState.showSendIcon)
                    textViewChatFooterAttachment.goneIfFalse(viewState.showMenuIcon)

                    editTextChatFooter.isEnabled = viewState.messagingEnabled
                    textViewChatFooterSend.isEnabled = viewState.messagingEnabled
                    textViewChatFooterAttachment.isEnabled = viewState.messagingEnabled

                    if (viewState is FooterViewState.RecordingAudioAttachment) {
                        textViewRecordingTimer.text = viewState.duration.toTimestamp()
                    } else {
                        layoutConstraintChatFooterActions.translationX = 0f
                    }
                }

                recordingCircleBinding.apply {
                    root.goneIfFalse(viewState.recordingEnabled)
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.getAttachmentSendViewStateFlow().collect { viewState ->
                attachmentSendLastViewState?.let {
                    if (it == viewState) {
                        return@collect
                    }
                }

                attachmentSendLastViewState = viewState

                for (job in attachmentSendViewStateJobs) {
                    job.cancel()
                }
                attachmentSendViewStateJobs.clear()
                for (disposable in attachmentSendViewStateDisposables) {
                    disposable.dispose()
                }
                attachmentSendViewStateDisposables.clear()

                attachmentSendBinding.apply {
                    @Exhaustive
                    when (viewState) {
                        is AttachmentSendViewState.Idle -> {
                            root.gone
                            includePaidTextMessageSendPreview.root.gone
                            imageViewAttachmentSendPreview.setImageDrawable(null)
                        }
                        is AttachmentSendViewState.Preview -> {

                            textViewAttachmentSendHeaderName.apply {
                                @Exhaustive
                                when (viewState.type) {
                                    is MediaType.Image -> {
                                        text = getString(R.string.attachment_send_header_image)
                                    }
                                    is MediaType.Audio -> {
                                        // TODO: Implement
                                    }
                                    is MediaType.Pdf -> {
                                        // TODO: Implement
                                    }
                                    is MediaType.Video -> {
                                        text = getString(R.string.attachment_send_header_video)
                                    }
                                    is MediaType.Text -> {
                                        text = getString(R.string.attachment_send_header_paid_message)
                                    }
                                    is MediaType.Unknown -> {}
                                }
                            }

                            if (viewState.type is MediaType.Image) {

                                // will load almost immediately b/c it's a file, so
                                // no need to launch separate coroutine.
                                viewState.file?.let { nnFile ->
                                    lifecycleScope.launch(viewModel.mainImmediate) {
                                        val disposable = imageLoader.load(imageViewAttachmentSendPreview, nnFile)
                                        attachmentSendViewStateDisposables.add(disposable)
                                    }.let { job ->
                                        attachmentSendViewStateJobs.add(job)
                                    }
                                }

                            } else if (viewState.type == MediaType.Text) {

                                includePaidTextMessageSendPreview.apply {
                                    textViewPaidMessagePreviewText.text = viewState.paidMessage?.first ?: footerBinding.editTextChatFooter.text

                                    textViewPaidMessagePreviewPrice.text =
                                        (viewState.paidMessage?.second ?: attachmentSendBinding.editTextMessagePrice.text?.toString()?.toLongOrNull())
                                        ?.toSat()?.asFormattedString(appendUnit = true) ?: "0 sats"

                                    root.visible
                                }

                            }

                            root.visible
                        }
                        is AttachmentSendViewState.PreviewGiphy -> {

                            textViewAttachmentSendHeaderName.apply {
                                text = getString(R.string.attachment_send_header_giphy)
                            }

                            root.visible

                            viewState.giphyData.retrieveImageUrlAndMessageMedia()?.let {
                                lifecycleScope.launch(viewModel.mainImmediate) {
                                    val disposable = imageLoader.load(imageViewAttachmentSendPreview, it.first)
                                    attachmentSendViewStateDisposables.add(disposable)
                                }.let { job ->
                                    attachmentSendViewStateJobs.add(job)
                                }
                            }
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.getAttachmentFullscreenViewStateFlow().collect { viewState ->
                fullscreenLastViewState?.let {
                    if (it == viewState) {
                        return@collect
                    }
                }

                fullscreenLastViewState = viewState

                for (job in fullScreenViewStateJobs) {
                    job.cancel()
                }
                fullScreenViewStateJobs.clear()
                for (disposable in fullScreenViewStateDisposables) {
                    disposable.dispose()
                }
                fullScreenViewStateDisposables.clear()

                attachmentFullscreenBinding.apply {
                    @Exhaustive
                    when (viewState) {
                        is AttachmentFullscreenViewState.Idle -> {
                            root.gone
                            imageViewAttachmentFullscreen.setImageDrawable(null)
                        }
                        is AttachmentFullscreenViewState.Fullscreen -> {
                            imageViewAttachmentFullscreen.resetInteractionProperties()

                            viewState.media?.localFile?.let { nnLocalFile ->
                                lifecycleScope.launch(viewModel.mainImmediate) {
                                    val disposable = imageLoader.load(
                                        imageViewAttachmentFullscreen,
                                        nnLocalFile
                                    )
                                    fullScreenViewStateDisposables.add(disposable)
                                }.let { job ->
                                    fullScreenViewStateJobs.add(job)
                                }
                            } ?: run {
                                lifecycleScope.launch(viewModel.mainImmediate) {
                                    val builder = ImageLoaderOptions.Builder()

                                    viewState.media?.host?.let { host ->
                                        viewModel.memeServerTokenHandler.retrieveAuthenticationToken(
                                            host
                                        )?.let { token ->
                                            builder.addHeader(token.headerKey, token.headerValue)

                                            viewState.media.mediaKeyDecrypted?.value?.let { key ->
                                                val header = CryptoHeader.Decrypt.Builder()
                                                    .setScheme(CryptoScheme.Decrypt.JNCryptor)
                                                    .setPassword(key)
                                                    .build()

                                                builder.addHeader(header.key, header.value)
                                            }
                                        }
                                    }

                                    val disposable =imageLoader.load(
                                        imageViewAttachmentFullscreen,
                                        viewState.url,
                                        builder.build()
                                    )
                                    fullScreenViewStateDisposables.add(disposable)
                                }.let { job ->
                                    fullScreenViewStateJobs.add(job)
                                }
                            }

                            root.visible
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.readMessages()
    }

    override fun onStop() {
        super.onStop()
        viewModel.audioPlayerController.pauseMediaIfPlaying()
    }

    override suspend fun onSideEffectCollect(sideEffect: ChatSideEffect) {
        sideEffect.execute(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        messageReplyLastViewState = null
        headerInitialHolderLastViewState = null
        fullscreenLastViewState = null
        attachmentSendLastViewState = null
    }

    private fun isRecordingPermissionsGranted() = arrayOf(Manifest.permission.RECORD_AUDIO).all {
        ContextCompat.checkSelfPermission(
            requireContext(), it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startRecording() {
        viewModel.audioRecorderController.startAudioRecording()
    }

    override fun isActive(): Boolean {
        return viewModel.audioRecorderController.isRecording()
    }

    override fun thresholdX(): Float {
        return footerBinding.textViewRecordingSlideToCancel.x + footerBinding.textViewRecordingSlideToCancel.measuredWidth
    }

    override fun onSlideToCancel() {
        viewModel.stopAndDeleteAudioRecording()
    }

    override fun onInteractionComplete() {
        viewModel.audioRecorderController.stopAudioRecording()
        viewModel.audioRecorderController.recordingTempFile?.let {
            sendMessageBuilder.setAttachmentInfo(
                AttachmentInfo(
                    file = it,
                    mediaType = MediaType.Audio(AudioRecorderController.AUDIO_FORMAT_MIME_TYPE),
                    isLocalFile = true,
                )
            )

            viewModel.sendMessage(sendMessageBuilder)?.let {
                // if it did not return null that means it was valid
                viewModel.audioRecorderController.clear()
                viewModel.updateFooterViewState(FooterViewState.Default)

                sendMessageBuilder.clear()
                viewModel.messageReplyViewStateContainer.updateViewState(MessageReplyViewState.ReplyingDismissed)
            }

        }
    }
}
