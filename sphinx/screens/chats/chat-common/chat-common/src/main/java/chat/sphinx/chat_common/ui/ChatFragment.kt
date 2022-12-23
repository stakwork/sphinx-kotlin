package chat.sphinx.chat_common.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.LayoutRes
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
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
import chat.sphinx.chat_common.ui.viewstate.messagereply.MessageReplyViewState
import chat.sphinx.chat_common.ui.viewstate.search.MessagesSearchViewState
import chat.sphinx.chat_common.ui.viewstate.selected.MenuItemState
import chat.sphinx.chat_common.ui.viewstate.selected.SelectedMessageViewState
import chat.sphinx.chat_common.ui.viewstate.selected.setMenuColor
import chat.sphinx.chat_common.ui.viewstate.selected.setMenuItems
import chat.sphinx.chat_common.ui.widgets.SlideToCancelImageView
import chat.sphinx.chat_common.ui.widgets.SphinxFullscreenImageView
import chat.sphinx.chat_common.util.AudioRecorderController
import chat.sphinx.chat_common.util.VideoThumbnailUtil
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_network_client_crypto.CryptoHeader
import chat.sphinx.concept_network_client_crypto.CryptoScheme
import chat.sphinx.concept_repository_message.model.AttachmentInfo
import chat.sphinx.concept_repository_message.model.SendMessage
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.insetter_activity.*
import chat.sphinx.keyboard_inset_fragment.KeyboardInsetMotionLayoutFragment
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.menu_bottom.databinding.LayoutMenuBottomBinding
import chat.sphinx.menu_bottom.model.MenuBottomOption
import chat.sphinx.menu_bottom.ui.BottomMenu
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.resources.*
import chat.sphinx.wrapper_chat.isTrue
import chat.sphinx.wrapper_common.FileSize
import chat.sphinx.wrapper_common.asFormattedString
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.util.getHHMMSSString
import chat.sphinx.wrapper_common.util.getHHMMString
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_meme_server.headerKey
import chat.sphinx.wrapper_meme_server.headerValue
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_message_media.*
import chat.sphinx.wrapper_view.Dp
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.goneIfTrue
import io.matthewnelson.android_feature_screens.util.visible
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
        >(@LayoutRes layoutId: Int): KeyboardInsetMotionLayoutFragment<
        Nothing,
        ChatSideEffectFragment,
        ChatSideEffect,
        ChatMenuViewState,
        VM,
        VB
        >(layoutId), ChatSideEffectFragment, SlideToCancelImageView.SlideToCancelListener
{
    protected abstract val footerBinding: LayoutChatFooterBinding
    protected abstract val searchFooterBinding: LayoutChatSearchFooterBinding
    protected abstract val headerBinding: LayoutChatHeaderBinding
    protected abstract val searchHeaderBinding: LayoutChatSearchHeaderBinding
    protected abstract val recordingAudioContainer: ConstraintLayout
    protected abstract val recordingCircleBinding: LayoutChatRecordingCircleBinding
    protected abstract val replyingMessageBinding: LayoutMessageReplyBinding
    protected abstract val selectedMessageBinding: LayoutSelectedMessageBinding
    protected abstract val selectedMessageHolderBinding: LayoutMessageHolderBinding
    protected abstract val attachmentSendBinding: LayoutAttachmentSendPreviewBinding
    protected abstract val attachmentFullscreenBinding: LayoutAttachmentFullscreenBinding
    protected abstract val menuBinding: LayoutChatMenuBinding
    protected abstract val callMenuBinding: LayoutMenuBottomBinding
    protected abstract val moreMenuBinding: LayoutMenuBottomBinding
    protected abstract val recyclerView: RecyclerView

    protected abstract val menuEnablePayments: Boolean

    protected abstract val userColorsHelper: UserColorsHelper
    protected abstract val imageLoader: ImageLoader<ImageView>

    protected val sendMessageBuilder = SendMessage.Builder()

    private val holderJobs: ArrayList<Job> = ArrayList(11)
    private val disposables: ArrayList<Disposable> = ArrayList(4)

    private val timeTrackerStart = System.currentTimeMillis()

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

    val bottomMenuMore: BottomMenu by lazy(LazyThreadSafetyMode.NONE) {
        BottomMenu(
            viewModel.dispatchers,
            onStopSupervisor,
            viewModel.moreOptionsMenuHandler
        )
    }

    override val contentChooserContract: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            viewModel.handleActivityResultUri(uri)
        }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.submitSideEffect(
                ChatSideEffect.Notify(
                    if (granted) {
                        getString(R.string.recording_permission_granted)
                    } else {
                        getString(R.string.recording_permission_required)
                    }
                )
            )
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
        setupSelectedMessage()
        setupHeader(insetterActivity)
        setupAttachmentSendPreview(insetterActivity)
        setupAttachmentFullscreen(insetterActivity)
        setupRecyclerView()

        viewModel.screenInit()
    }

    override fun onKeyboardToggle() {
        addViewKeyboardBottomPadding(
            (requireActivity() as InsetterActivity)
        )
        scrollToBottom(itemsDiff = 3)
    }

    private fun addViewKeyboardBottomPadding(insetterActivity: InsetterActivity) {
        callMenuBinding.apply {
            insetterActivity.addKeyboardPadding(root)
        }

        moreMenuBinding.apply {
            insetterActivity.addKeyboardPadding(root)
        }

        recordingCircleBinding.apply {
            insetterActivity.addKeyboardPadding(root)
        }

        footerBinding.apply {
            insetterActivity.addKeyboardPadding(root)
        }

        searchFooterBinding.apply {
            insetterActivity.addKeyboardPadding(root)
        }

        menuBinding.includeLayoutChatMenuOptions.apply {
            insetterActivity.addKeyboardPadding(root)
        }

        insetterActivity.addKeyboardPadding(recordingAudioContainer)
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
            lifecycleScope.launch(viewModel.mainImmediate) {
                viewModel.handleCommonChatOnBackPressed()
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
        setupMoreOptionsMenu()
        setupCallMenu()

        callMenuBinding.apply {
            insetterActivity.addNavigationBarPadding(root)
        }

        moreMenuBinding.apply {
            insetterActivity.addNavigationBarPadding(root)
        }

        recordingCircleBinding.apply {
            insetterActivity.addNavigationBarPadding(root)
        }

        insetterActivity.addNavigationBarPadding(recordingAudioContainer)

        footerBinding.apply {
            insetterActivity.addNavigationBarPadding(root)

            textViewChatFooterSend.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {

                    sendMessageBuilder.setText(editTextChatFooter.text?.toString())

                    sendMessageBuilder.setPaidMessagePrice(
                        attachmentSendBinding.editTextMessagePrice.text?.toString()?.toLongOrNull()?.toSat()
                    )

                    val attachmentViewState = viewModel.getAttachmentSendViewStateFlow().value

                    @Exhaustive
                    when (attachmentViewState) {
                        is AttachmentSendViewState.Idle -> {
                            sendMessageBuilder.setAttachmentInfo(null)
                        }
                        is AttachmentSendViewState.Preview -> {
                            if (
                                attachmentViewState.type.isImage ||
                                attachmentViewState.type.isVideo ||
                                attachmentViewState.type.isPdf ||
                                attachmentViewState.type.isUnknown
                            ) {
                                attachmentViewState.file?.let { nnFile ->
                                    sendMessageBuilder.setAttachmentInfo(
                                        AttachmentInfo(
                                            file = nnFile,
                                            mediaType = attachmentViewState.type,
                                            fileName = attachmentViewState.fileName,
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
                                            fileName = null,
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
                                    sendAttachmentViewState.fileName,
                                    Pair(text, price),
                                )
                            )
                        }
                    }
                }
            })

            editTextChatFooter.onCommitContentListener = viewModel.onIMEContent
            editTextChatFooter.addTextChangedListener { editable ->
                //Do not toggle microphone and send icon if on attachment mode
                if (viewModel.getFooterViewStateFlow().value !is FooterViewState.Attachment) {
                    textViewChatFooterSend.goneIfTrue(editable.isNullOrEmpty())
                    imageViewChatFooterMicrophone.goneIfFalse(editable.isNullOrEmpty())
                }
            }
        }

        searchFooterBinding.apply {
            insetterActivity.addNavigationBarPadding(root)

            textViewChatSearchNext.setOnClickListener {
                viewModel.navigateResults(1)
            }

            textViewChatSearchPrevious.setOnClickListener {
                viewModel.navigateResults(-1)
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

    open fun setupMoreOptionsMenu() {

        bottomMenuMore.newBuilder(moreMenuBinding, viewLifecycleOwner)
            .setHeaderText(R.string.bottom_menu_more_header_text)
            .setOptions(
                setOf(
                    MenuBottomOption(
                        text = R.string.bottom_menu_more_option_call,
                        textColor = R.color.primaryBlueFontColor,
                        onClick = {
                            viewModel.moreOptionsMenuHandler.updateViewState(
                                MenuBottomViewState.Closed
                            )
                            viewModel.callMenuHandler.updateViewState(
                                MenuBottomViewState.Open
                            )
                        }
                    ),
                    MenuBottomOption(
                        text = R.string.bottom_menu_more_option_search,
                        textColor = R.color.primaryBlueFontColor,
                        onClick = {
                            lifecycleScope.launch(viewModel.mainImmediate) {
                                viewModel.searchMessages(null)
                            }
                        }
                    ),
                    MenuBottomOption(
                        text = R.string.option_delete_user,
                        textColor = R.color.primaryRed,
                        onClick = {
                            lifecycleScope.launch(viewModel.mainImmediate) {
                                viewModel.confirmDeleteContact()
                            }
                        }
                    ),
                    MenuBottomOption(
                        text = R.string.option_block_user,
                        textColor = R.color.primaryRed,
                        onClick = {
                            lifecycleScope.launch(viewModel.mainImmediate) {
                               viewModel.confirmToggleBlockContactState()
                            }
                        }
                    ),
                )
            ).build()
    }

    private fun setupCallMenu() {
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
                                sendAttachmentViewState.fileName,
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

            root.layoutParams.height = root.layoutParams.height + insetterActivity.statusBarInsetHeight.top
            root.requestLayout()

            imageViewChatHeaderMuted.setOnClickListener {
                viewModel.toggleChatMuted()
            }

            textViewChatHeaderMore.setOnClickListener {
                viewModel.moreOptionsMenuHandler.updateViewState(
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

        searchHeaderBinding.apply {
            insetterActivity.addStatusBarPadding(root)

            root.layoutParams.height = root.layoutParams.height + insetterActivity.statusBarInsetHeight.top
            root.requestLayout()

            editTextChatSearch.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    lifecycleScope.launch(viewModel.mainImmediate) {
                        viewModel.searchMessages(s.toString())
                    }
                }
            })

            textViewChatSearchDone.setOnClickListener {
                editTextChatSearch.setText("")

                viewModel.messagesSearchViewStateContainer.updateViewState(
                    MessagesSearchViewState.Idle
                )

//                forceScrollToBottom()?
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
                includeLayoutSelectedMessageMenuItem5.root.setOnClickListener {
                    onSelectedMessageMenuItemClick(4)
                }
                includeLayoutSelectedMessageMenuItem6.root.setOnClickListener {
                    onSelectedMessageMenuItemClick(5)
                }
                includeLayoutSelectedMessageMenuItem7.root.setOnClickListener {
                    onSelectedMessageMenuItemClick(6)
                }
                includeLayoutSelectedMessageMenuItem8.root.setOnClickListener {
                    onSelectedMessageMenuItemClick(7)
                }
                includeLayoutSelectedMessageMenuItem9.root.setOnClickListener {
                    onSelectedMessageMenuItemClick(8)
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

            textViewAttachmentNextPage.setOnClickListener {
                viewModel.navigateToPdfPage(1)
            }
            textViewAttachmentPreviousPage.setOnClickListener {
                viewModel.navigateToPdfPage(-1)
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
                    holderState.message?.let { message ->
                        holderState.selectionMenuItems?.elementAtOrNull(index)?.let { item ->
                            when (item) {
                                is MenuItemState.Boost -> {
                                    viewModel.boostMessage(message.uuid)
                                }
                                is MenuItemState.CopyCallLink -> {
                                    // TODO: Implement
                                }
                                is MenuItemState.CopyLink -> {
                                    viewModel.copyMessageLink(message)
                                }
                                is MenuItemState.CopyText -> {
                                    viewModel.copyMessageText(message)
                                }
                                is MenuItemState.Delete -> {
                                    viewModel.deleteMessage(message)
                                }
                                is MenuItemState.Reply -> {
                                    viewModel.replyToMessage(message)
                                }
                                is MenuItemState.SaveFile -> {
                                    viewModel.saveFile(
                                        message,
                                        selectedMessageBinding.includeLayoutMessageHolderSelectedMessage.includeMessageHolderBubble.includeMessageTypeImageAttachment.imageViewAttachmentImage.drawable
                                    )
                                }
                                is MenuItemState.Resend -> {
                                    viewModel.resendMessage(message)
                                }
                                is MenuItemState.Flag -> {
                                    viewModel.flagMessage(message)
                                }
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

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                        lifecycleScope.launch(viewModel.mainImmediate) {
                            viewModel.readMessages()
                        }
                    }
                }
            })
        }
        Log.d("TimeTracker", "Chat messages were displayed in ${System.currentTimeMillis() - timeTrackerStart} milliseconds")
        viewModel.sendAppLog("- Chat messages were displayed in ${System.currentTimeMillis() - timeTrackerStart} milliseconds")
    }

    protected fun scrollToBottom(
        callback: (() -> Unit)? = null,
        replyingToMessage: Boolean = false,
        itemsDiff: Int = 0,
    ) {
        (recyclerView.adapter as ConcatAdapter).adapters.firstOrNull()?.let { messagesListAdapter ->
            (messagesListAdapter as MessageListAdapter<*>).scrollToBottomIfNeeded(callback, replyingToMessage, itemsDiff)
        }
    }

    protected fun forceScrollToBottom() {
        (recyclerView.adapter as ConcatAdapter).adapters.firstOrNull()?.let { messagesListAdapter ->
            (messagesListAdapter as MessageListAdapter<*>).forceScrollToBottom()
        }
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
                        sendMessageBuilder.setPodcastClip(null)

                        replyingMessageBinding.root.gone
                    }
                    is MessageReplyViewState.CommentingOnPodcast -> {
                        sendMessageBuilder.setReplyUUID(null)
                        sendMessageBuilder.setPodcastClip(viewState.podcastClip)

                        replyingMessageBinding.apply {
                            imageViewReplyMediaImage.gone
                            textViewReplyTextOverlay.gone

                            val tsString = (viewState.podcastClip.ts * 1000).toLong().getHHMMSSString()

                            textViewReplySenderLabel.text = viewState.podcastClip.title
                            textViewReplyMessageLabel.text = "${getString(R.string.share_audio_clip)} $tsString"

                            viewReplyBarLeading.setBackgroundColor(
                                root.context.getColor(
                                    R.color.washedOutReceivedText
                                )
                            )
                        }

                        replyingMessageBinding.root.visible
                    }
                    is MessageReplyViewState.ReplyingToMessage -> {
                        sendMessageBuilder.setPodcastClip(null)

                        val message = viewState.message

                        message.uuid?.value?.toReplyUUID().let { uuid ->
                            sendMessageBuilder.setReplyUUID(uuid)

                            replyingMessageBinding.apply {

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

                            Log.d("TimeTracker", "Chat contact/tribe name was displayed in ${System.currentTimeMillis() - timeTrackerStart} milliseconds")
                            viewModel.sendAppLog("- Chat contact/tribe name was displayed in ${System.currentTimeMillis() - timeTrackerStart} milliseconds")

                            imageViewChatHeaderMuted.apply {
                                viewState.isMuted?.let { muted ->
                                    if (muted) {
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

                        val menuHeight = (resources.getDimension(R.dimen.selected_message_menu_item_height) * (viewState.messageHolderViewState.selectionMenuItems?.size ?: 0))
                        val menuStandardMargin = Dp(10F).toPx(selectedMessageHolderBinding.root.context).value
                        val overlappedMenuMargin = Dp(60F).toPx(selectedMessageHolderBinding.root.context).value
                        var overlappedMenu = false

                        var menuYPos = if (viewState.showMenuTop) {
                            if ((viewState.holderYPos.value - overlappedMenuMargin) < menuHeight) {
                                overlappedMenu = true
                                overlappedMenuMargin
                            } else {
                                viewState.holderYPos.value -
                                        menuHeight -
                                        menuStandardMargin
                            }
                        } else {
                            if (viewState.holderYPos.value + viewState.bubbleHeight.value + menuHeight + overlappedMenuMargin > viewState.screenHeight.value) {
                                overlappedMenu = true
                                viewState.screenHeight.value -
                                        (menuHeight + overlappedMenuMargin)
                            } else {
                                viewState.holderYPos.value +
                                        viewState.bubbleHeight.value +
                                        menuStandardMargin
                            }
                        }

                        selectedMessageHolderBinding.apply {
                            root.y = viewState.holderYPos.value
                            root.alpha = if (overlappedMenu) 0.6F else 1.0F

                            setView(
                                lifecycleScope,
                                holderJobs,
                                disposables,
                                viewModel.dispatchers,
                                viewModel.audioPlayerController,
                                imageLoader,
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

                                this@menu.root.y = menuYPos

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
                        textViewRecordingTimer.text = viewState.duration.getHHMMString()
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
                            layoutConstraintVideoPlayButton.gone
                        }
                        is AttachmentSendViewState.Preview -> {
                            when (viewState.type) {
                                is MediaType.Image -> {
                                    layoutConstraintFileAttachmentPreview.gone
                                    includePaidTextMessageSendPreview.root.gone

                                    textViewAttachmentSendHeaderName.text = getString(R.string.attachment_send_header_image)
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
                                }
                                is MediaType.Pdf -> {
                                    includePaidTextMessageSendPreview.root.gone

                                    textViewAttachmentSendHeaderName.text = getString(R.string.attachment_send_header_pdf)
                                    textViewAttachmentFileIconPreview.text = getString(R.string.material_icon_name_file_pdf)

                                    viewState.file?.let { nnFile ->
                                        val fileDescriptor = ParcelFileDescriptor.open(nnFile,
                                            ParcelFileDescriptor.MODE_READ_ONLY
                                        )
                                        val renderer = PdfRenderer(fileDescriptor)
                                        val pageCount = renderer.pageCount

                                        textViewAttachmentFileNamePreview.text = viewState.fileName?.value ?: nnFile.name
                                        textViewAttachmentFileSizePreview.text = if (pageCount > 1) {
                                            "$pageCount ${getString(R.string.pdf_pages)}"
                                        } else {
                                            "$pageCount ${getString(R.string.pdf_page)}"
                                        }
                                    }

                                    layoutConstraintFileAttachmentPreview.visible
                                }
                                is MediaType.Video -> {
                                    layoutConstraintFileAttachmentPreview.gone
                                    includePaidTextMessageSendPreview.root.gone

                                    textViewAttachmentSendHeaderName.text = getString(R.string.attachment_send_header_video)
                                    // will load almost immediately b/c it's a file, so
                                    // no need to launch separate coroutine.
                                    viewState.file?.let { nnFile ->
                                        lifecycleScope.launch(viewModel.mainImmediate) {
                                            layoutConstraintVideoPlayButton.visible
                                            textViewAttachmentPlayButton.setOnClickListener {
                                                viewModel.goToFullscreenVideo(
                                                    messageId = MessageId(-1L),
                                                    nnFile.absolutePath
                                                )
                                            }
                                            imageViewAttachmentSendPreview.setImageBitmap(
                                                VideoThumbnailUtil.loadThumbnail(nnFile)
                                            )
                                        }.let { job ->
                                            attachmentSendViewStateJobs.add(job)
                                        }
                                    }
                                }
                                is MediaType.Text -> {
                                    layoutConstraintFileAttachmentPreview.gone

                                    textViewAttachmentSendHeaderName.text = getString(R.string.attachment_send_header_paid_message)

                                    includePaidTextMessageSendPreview.apply {
                                        textViewPaidMessagePreviewText.text = viewState.paidMessage?.first ?: footerBinding.editTextChatFooter.text

                                        textViewPaidMessagePreviewPrice.text =
                                            (viewState.paidMessage?.second ?: attachmentSendBinding.editTextMessagePrice.text?.toString()?.toLongOrNull())
                                                ?.toSat()?.asFormattedString(appendUnit = true) ?: "0 sats"

                                        root.visible
                                    }
                                }
                                is MediaType.Unknown -> {
                                    includePaidTextMessageSendPreview.root.gone

                                    textViewAttachmentSendHeaderName.text = getString(R.string.attachment_send_header_file)
                                    textViewAttachmentFileIconPreview.text = getString(R.string.material_icon_name_file_attachment)

                                    viewState.file?.let { nnFile ->
                                        textViewAttachmentFileNamePreview.text = viewState.fileName?.value ?: nnFile.name
                                        textViewAttachmentFileSizePreview.text = FileSize(nnFile.length()).asFormattedString()
                                    }

                                    layoutConstraintFileAttachmentPreview.visible
                                }
                                else -> { }
                            }
                            root.visible
                        }
                        is AttachmentSendViewState.PreviewGiphy -> {
                            layoutConstraintFileAttachmentPreview.gone
                            includePaidTextMessageSendPreview.root.gone

                            textViewAttachmentSendHeaderName.apply {
                                text = getString(R.string.attachment_send_header_giphy)
                            }

                            viewState.giphyData.retrieveImageUrlAndMessageMedia()?.let {
                                lifecycleScope.launch(viewModel.mainImmediate) {
                                    val disposable = imageLoader.load(imageViewAttachmentSendPreview, it.first)
                                    attachmentSendViewStateDisposables.add(disposable)
                                }.let { job ->
                                    attachmentSendViewStateJobs.add(job)
                                }
                            }

                            root.visible
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
                        is AttachmentFullscreenViewState.ImageFullscreen -> {
                            layoutConstraintPDFHeader.gone
                            textViewAttachmentNextPage.gone
                            textViewAttachmentPreviousPage.gone

                            imageViewAttachmentFullscreen.resetInteractionProperties()
                            imageViewAttachmentFullscreen.setBackgroundColor(
                                getColor(android.R.color.transparent)
                            )

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

                                    val disposable = imageLoader.load(
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
                        is AttachmentFullscreenViewState.PdfFullScreen -> {
                            val page = viewState.pdfRender.openPage(viewState.currentPage)

                            val bitmap = Bitmap.createBitmap(
                                page.width,
                                page.height,
                                Bitmap.Config.ARGB_8888
                            )

                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()

                            imageViewAttachmentFullscreen.resetInteractionProperties()
                            imageViewAttachmentFullscreen.setImageBitmap(bitmap)
                            imageViewAttachmentFullscreen.setBackgroundColor(
                                getColor(android.R.color.white)
                            )

                            layoutConstraintPDFHeader.visible

                            textViewAttachmentPdfName.text = viewState.fileName.value
                            textViewAttachmentPdfPage.text = getString(R.string.pdf_page_of, viewState.currentPage + 1, viewState.pageCount)

                            textViewAttachmentNextPage.goneIfFalse(viewState.currentPage < viewState.pageCount - 1)
                            textViewAttachmentPreviousPage.goneIfFalse(viewState.currentPage > 0)

                            progressBarAttachmentFullscreen.gone
                            root.visible
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.messagesSearchViewStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is MessagesSearchViewState.Idle -> {
                        headerBinding.root.visible
                        footerBinding.root.visible

                        searchHeaderBinding.root.gone
                        searchFooterBinding.root.gone

                        (recyclerView.adapter as ConcatAdapter).adapters.firstOrNull()?.let { messagesListAdapter ->
                            (messagesListAdapter as MessageListAdapter<*>).resetHighlighted()
                        }
                    }
                    is MessagesSearchViewState.Loading -> {
                        headerBinding.root.gone
                        footerBinding.root.gone

                        searchHeaderBinding.root.visible
                        searchFooterBinding.root.visible

                        searchFooterBinding.apply {
                            progressBarLoadingSearch.visible
                            textViewChatSearchResultsFound.text = getString(R.string.searching_messages)
                            root.visible
                        }
                    }
                    is MessagesSearchViewState.Searching -> {
                        headerBinding.root.gone
                        footerBinding.root.gone

                        searchHeaderBinding.root.visible
                        searchFooterBinding.root.visible

                        searchFooterBinding.apply {
                            progressBarLoadingSearch.gone

                            textViewChatSearchResultsFound.text = getString(R.string.results_found, viewState.messages.size)

                            val enabledColor = ContextCompat.getColor(binding.root.context, R.color.text)
                            val disabledColor = ContextCompat.getColor(binding.root.context, R.color.secondaryText)

                            val nextButtonEnable = viewState.index < viewState.messages.size - 1
                            textViewChatSearchNext.isEnabled = nextButtonEnable
                            textViewChatSearchNext.setTextColor(
                                if (nextButtonEnable) {
                                    enabledColor
                                } else {
                                    disabledColor
                                }
                            )

                            val previousButtonEnable = viewState.index > 0
                            textViewChatSearchPrevious.isEnabled = previousButtonEnable
                            textViewChatSearchPrevious.setTextColor(
                                if (previousButtonEnable) {
                                    enabledColor
                                } else {
                                    disabledColor
                                }
                            )

                        }

                        setFocusOnSearchField()
                        scrollToResult(
                            viewState.messages,
                            viewState.index,
                            if (viewState.navigatingForward) viewState.index - 1 else viewState.index + 1
                        )
                    }
                }
            }
        }
    }

    private fun scrollToResult(
        messages: List<Message>,
        index: Int,
        prevIndex: Int
    ) {
        if (messages.size > index) {
            (recyclerView.adapter as ConcatAdapter).adapters.firstOrNull()?.let { messagesListAdapter ->
                messages[index]?.let { message ->
                    (messagesListAdapter as MessageListAdapter<*>).highlightAndScrollToSearchResult(
                        message,
                        if (messages.size > prevIndex && prevIndex >= 0) messages[prevIndex] else null,
                        searchHeaderBinding.editTextChatSearch.text?.toString() ?: ""
                    )
                }
            }
        }
    }

    private fun setFocusOnSearchField() {
        searchHeaderBinding.apply {
            if (!editTextChatSearch.hasFocus()) {
                editTextChatSearch.requestFocus()

                context?.let {
                    val inputMethodManager = ContextCompat.getSystemService(it, InputMethodManager::class.java)
                    inputMethodManager?.showSoftInput(editTextChatSearch, InputMethodManager.SHOW_IMPLICIT)
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
        viewModel.audioRecorderController.startAudioRecording(binding.root.context)
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
            lifecycleScope.launch(viewModel.mainImmediate) {
                sendMessageBuilder.setAttachmentInfo(
                    AttachmentInfo(
                        file = it,
                        mediaType = MediaType.Audio(AudioRecorderController.AUDIO_FORMAT_MIME_TYPE),
                        fileName = null,
                        isLocalFile = true,
                    )
                )

                viewModel.sendMessage(sendMessageBuilder)?.let {
                    // if it did not return null that means it was valid
                    viewModel.updateFooterViewState(FooterViewState.Default)

                    sendMessageBuilder.clear()
                    viewModel.messageReplyViewStateContainer.updateViewState(MessageReplyViewState.ReplyingDismissed)
                }
                viewModel.audioRecorderController.clear()
            }
        }
    }
}
