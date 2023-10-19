package chat.sphinx.chat_tribe.ui

import android.animation.Animator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_common.databinding.*
import chat.sphinx.chat_common.ui.ChatFragment
import chat.sphinx.chat_common.ui.ChatSideEffect
import chat.sphinx.chat_common.ui.viewstate.mentions.MessageMentionsViewState
import chat.sphinx.chat_common.ui.viewstate.menu.MoreMenuOptionsViewState
import chat.sphinx.chat_common.ui.viewstate.messageholder.LayoutState
import chat.sphinx.chat_common.ui.viewstate.messagereply.MessageReplyViewState
import chat.sphinx.chat_common.ui.viewstate.thread.ThreadHeaderViewState
import chat.sphinx.chat_common.util.VideoThumbnailUtil
import chat.sphinx.chat_tribe.R
import chat.sphinx.chat_tribe.adapters.BadgesItemAdapter
import chat.sphinx.chat_tribe.adapters.MessageMentionsAdapter
import chat.sphinx.chat_tribe.databinding.FragmentChatTribeBinding
import chat.sphinx.chat_tribe.databinding.LayoutChatTribeMemberMentionPopupBinding
import chat.sphinx.chat_tribe.databinding.LayoutChatTribePopupBinding
import chat.sphinx.chat_tribe.databinding.*
import chat.sphinx.chat_tribe.model.TribeFeedData
import chat.sphinx.chat_tribe.ui.viewstate.BoostAnimationViewState
import chat.sphinx.chat_tribe.ui.viewstate.TribeMemberDataViewState
import chat.sphinx.chat_tribe.ui.viewstate.TribeMemberProfileViewState
import chat.sphinx.chat_tribe.ui.viewstate.*
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addKeyboardPadding
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.menu_bottom.databinding.LayoutMenuBottomBinding
import chat.sphinx.menu_bottom.model.MenuBottomOption
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.resources.databinding.LayoutBoostFireworksBinding
import chat.sphinx.resources.databinding.LayoutPodcastPlayerFooterBinding
import chat.sphinx.resources.databinding.LayoutTribeAppBinding
import chat.sphinx.resources.databinding.LayoutTribeMemberProfileBinding
import chat.sphinx.resources.getRandomHexCode
import chat.sphinx.resources.getString
import chat.sphinx.resources.setBackgroundRandomColor
import chat.sphinx.wrapper_chat.protocolLessUrl
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_view.Px
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.invisible
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.delay
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
    private val podcastPlayerBinding: LayoutPodcastPlayerFooterBinding
        get() = binding.includePodcastPlayerFooter
    private val boostAnimationBinding: LayoutBoostFireworksBinding
        get() = binding.includeLayoutBoostFireworks
    private val tribePopupBinding: LayoutChatTribePopupBinding
        get() = binding.includeLayoutPopup
    private val tribeMemberProfileBinding: LayoutTribeMemberProfileBinding
        get() = binding.includeLayoutTribeMemberProfile
    private val tribeAppBinding: LayoutTribeAppBinding
        get() = binding.includeLayoutTribeApp
    override val footerBinding: LayoutChatFooterBinding
        get() = binding.includeChatTribeFooter
    override val searchFooterBinding: LayoutChatSearchFooterBinding
        get() = binding.includeChatTribeSearchFooter
    override val recordingAudioContainer: ConstraintLayout
        get() = binding.layoutConstraintRecordingAudioContainer
    override val recordingCircleBinding: LayoutChatRecordingCircleBinding
        get() = binding.includeChatRecordingCircle
    override val headerBinding: LayoutChatHeaderBinding
        get() = binding.includeChatTribeHeader
    override val searchHeaderBinding: LayoutChatSearchHeaderBinding
        get() = binding.includeChatTribeSearchHeader
    override val replyingMessageBinding: LayoutMessageReplyBinding
        get() = binding.includeChatTribeMessageReply
    override val selectedMessageBinding: LayoutSelectedMessageBinding
        get() = binding.includeChatTribeSelectedMessage
    override val selectedMessageHolderBinding: LayoutMessageHolderBinding
        get() = binding.includeChatTribeSelectedMessage.includeLayoutMessageHolderSelectedMessage
    override val attachmentSendBinding: LayoutAttachmentSendPreviewBinding
        get() = binding.includeChatTribeAttachmentSendPreview
    override val menuBinding: LayoutChatMenuBinding
        get() = binding.includeChatTribeMenu
    override val callMenuBinding: LayoutMenuBottomBinding
        get() = binding.includeLayoutMenuBottomCall
    override val moreMenuBinding: LayoutMenuBottomBinding
        get() = binding.includeLayoutMenuBottomMore
    override val scrollDownButtonBinding: LayoutScrollDownButtonBinding
        get() = binding.includeChatTribeScrollDown
    override val shimmerBinding: LayoutShimmerContainerBinding
        get() = binding.includeChatTribeShimmerContainer
    override val attachmentFullscreenBinding: LayoutAttachmentFullscreenBinding
        get() = binding.includeChatTribeAttachmentFullscreen
    private val mentionMembersPopup: LayoutChatTribeMemberMentionPopupBinding
        get() = binding.includeChatTribeMembersMentionPopup
    override val pinHeaderBinding: LayoutChatPinedMessageHeaderBinding?
        get() = binding.includeChatPinedMessageHeader

    override val threadOriginalMessageBinding: LayoutThreadOriginalMessageBinding?
        get() = binding.includeLayoutThreadOriginalMessage

    private val layoutChatPinPopupBinding: LayoutChatPinPopupBinding
        get() = binding.includePinMessagePopup
    private val layoutBottomPinned: LayoutBottomPinnedBinding
        get() = binding.includeLayoutBottomPinned
    private val webView: WebView
        get() = tribeAppBinding.includeLayoutTribeAppDetails.webView
    private val threadHeader: LayoutThreadHeaderBinding
        get() = binding.includeLayoutThreadHeader

    override val menuEnablePayments: Boolean
        get() = false

    override val recyclerView: RecyclerView
        get() = binding.recyclerViewMessages

    override val viewModel: ChatTribeViewModel by viewModels()
    private val tribeFeedViewModel: TribeFeedViewModel by viewModels()
    private val tribeAppViewModel: TribeAppViewModel by viewModels()

    @Inject
    @Suppress("ProtectedInFinal", "PropertyName")
    protected lateinit var _userColorsHelper: UserColorsHelper
    override val userColorsHelper: UserColorsHelper
        get() = _userColorsHelper

    @Inject
    @Suppress("ProtectedInFinal", "PropertyName")
    protected lateinit var _imageLoader: ImageLoader<ImageView>
    override val imageLoader: ImageLoader<ImageView>
        get() = _imageLoader


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        BackPressHandler(viewLifecycleOwner, requireActivity())

        lifecycleScope.launch(viewModel.mainImmediate) {
            try {
                viewModel.feedDataStateFlow.collect { data ->
                    @Exhaustive
                    when (data) {
                        is TribeFeedData.Loading -> {}
                        is TribeFeedData.Result -> {
                            tribeFeedViewModel.init(data)
                            tribeAppViewModel.init(data)
                            throw Exception()
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        binding.includeChatPinedMessageHeader.apply {
            layoutConstraintChatPinedMessageHeader.setOnClickListener {
                viewModel.showPinBottomView()
            }
        }

        binding.includeLayoutThreadHeader.apply {
            val insetterActivity = (requireActivity() as InsetterActivity)
            insetterActivity.addStatusBarPadding(root)

            root.layoutParams.height = root.layoutParams.height + insetterActivity.statusBarInsetHeight.top
            root.requestLayout()

            textViewChatHeaderNavBack.setOnClickListener {
                lifecycleScope.launch {
                    viewModel.chatNavigator.popBackStack()
                }
            }
        }

        podcastPlayerBinding.apply {
            imageViewForward30Button.setOnClickListener {
                tribeFeedViewModel.podcastViewStateContainer.value.clickFastForward?.invoke()
            }
            textViewPlayButton.setOnClickListener {
                tribeFeedViewModel.podcastViewStateContainer.value.clickPlayPause?.invoke()
            }
            animationViewPauseButton.setOnClickListener {
                tribeFeedViewModel.podcastViewStateContainer.value.clickPlayPause?.invoke()
            }
            layoutConstraintPodcastInfo.setOnClickListener {
                tribeFeedViewModel.podcastViewStateContainer.value.clickTitle?.invoke()
            }
        }

        binding.includeChatTribeHeader.imageViewChatWebView.setOnClickListener {
            tribeAppViewModel.toggleWebAppView()
        }

        binding.includeChatTribeHeader.imageViewThreadsView.visible
        binding.includeChatTribeHeader.imageViewThreadsView.setOnClickListener {
            viewModel.navigateToThreads()
        }

        boostAnimationBinding.lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener{
            override fun onAnimationEnd(animation: Animator) {
                boostAnimationBinding.root.gone
            }

            override fun onAnimationRepeat(animation: Animator) {}

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationStart(animation: Animator) {}
        })

        tribeFeedViewModel.shareClipHandler = { podcastClip ->
            viewModel.messageReplyViewStateContainer.updateViewState(
                MessageReplyViewState.CommentingOnPodcast(podcastClip)
            )
        }

        tribePopupBinding.layoutChatTribePopup.apply {
            buttonSendSats.setOnClickListener {
                viewModel.goToPaymentSend()
            }
            textViewDirectPaymentPopupClose.setOnClickListener {
                viewModel.tribeMemberDataViewStateContainer.updateViewState(TribeMemberDataViewState.Idle)
            }
        }

        tribeMemberProfileBinding.apply {
            (requireActivity() as InsetterActivity).addStatusBarPadding(root)
            (requireActivity() as InsetterActivity).addKeyboardPadding(root)

            includeLayoutTribeMemberProfileDetails.apply {
                includeLayoutTribeSendSatsBar.apply {
                    layoutConstraintSendSatsButton.setOnClickListener {
                        viewModel.goToPaymentSend()
                    }

                    layoutConstraintEarnBadges.setOnClickListener {
                        viewModel.goToKnownBadges()
                    }
                }

                layoutConstraintDismissLine.setOnClickListener {
                    viewModel.tribeMemberProfileViewStateContainer.updateViewState(TribeMemberProfileViewState.Closed)
                }

                includeLayoutTribeProfileInfoContainer.apply {
                    constraintLayoutTribeRow1.setOnClickListener {
                        viewModel.tribeMemberProfileViewStateContainer.updateViewState(
                            TribeMemberProfileViewState.FullScreen
                        )
                    }

                    layoutBadgesArrowDownContainer.setOnClickListener {
                        viewModel.tribeMemberProfileViewStateContainer.updateViewState(
                            TribeMemberProfileViewState.Open
                        )
                    }
                }
            }
        }

        layoutBottomPinned.apply {
            (requireActivity() as InsetterActivity).addNavigationBarPadding(root)

            includeLayoutPinBottomTemplate.apply {
                layoutConstraintPinnedBottomUnpinButton.apply {
                    setOnClickListener {
                        viewModel.unPinMessage()
                    }
                }
            }

            viewPinBottomInputLock.setOnClickListener {
                viewModel.pinedMessageBottomViewState.updateViewState(
                    PinMessageBottomViewState.Closed
                )
            }
        }

        footerBinding.editTextChatFooter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.processMemberMention(s)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        tribeAppBinding.includeLayoutTribeAppDetails.apply {
            (requireActivity() as InsetterActivity).addNavigationBarPadding(layoutConstraintBudget)

            buttonAuthorize.setOnClickListener {
                tribeAppViewModel.authorizeWebApp(editTextSatsAmount.text.toString())
            }
            textViewDetailScreenClose.setOnClickListener {
                tribeAppViewModel.hideAuthorizePopup()
            }
        }

        mentionMembersPopup.listviewMentionTribeMembers.setOnItemClickListener { parent, _, position, _ ->
            (parent.adapter as? ArrayAdapter<String>?)?.let {
                it.getItem(position)?.let { selectedAlias ->
                    footerBinding.editTextChatFooter.apply {

                        val newText = text.toString().messageWithMention(selectedAlias)

                        setText(newText)
                        setSelection(length())
                    }

                    mentionMembersPopup.root.gone
                }
            }
        }

        mentionMembersPopup.listviewMentionTribeMembers.adapter = MessageMentionsAdapter(binding.root.context, mutableListOf())

        tribeMemberProfileBinding.includeLayoutTribeMemberProfileDetails
            .includeLayoutTribeProfileInfoContainer.recyclerViewBadges
            .apply {
                val linearLayoutManager = LinearLayoutManager(context)
                val badgesItemsAdapter = BadgesItemAdapter(
                    imageLoader,
                    viewLifecycleOwner,
                    onStopSupervisor,
                    viewModel
                )
                this.setHasFixedSize(false)
                layoutManager = linearLayoutManager
                adapter = badgesItemsAdapter
                itemAnimator = null
            }
    }

    private suspend fun loadBadgeImage(imageView: ImageView, photoUrl: String?) {
        if (photoUrl == null) {
            imageView.invisible
        } else {
            imageLoader.load(
                imageView,
                photoUrl,
                ImageLoaderOptions.Builder()
                    .placeholderResId(R.drawable.sphinx_icon)
                    .transformation(Transformation.CircleCrop)
                    .build()
            )
        }
    }
    private fun loadWebView(url: String) {
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(tribeAppViewModel, "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()
                view?.loadUrl(url)
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                tribeAppViewModel.didFinishLoadingWebView()
                super.onPageFinished(view, url)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                tribeAppViewModel.didFinishLoadingWebView()
                super.onReceivedError(view, request, error)
            }

            override fun onUnhandledKeyEvent(view: WebView?, event: KeyEvent?) {
                super.onUnhandledKeyEvent(view, event)

                if (event?.keyCode == KeyEvent.KEYCODE_ENTER || event?.keyCode == KeyEvent.ACTION_DOWN) {
                    val imm =
                        tribeAppBinding.root.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view?.windowToken, 0)
                }
            }
        }
        webView.loadUrl(url)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        tribeFeedViewModel.trackPodcastConsumed()
    }

    private inner class BackPressHandler(
        owner: LifecycleOwner,
        activity: FragmentActivity,
    ): OnBackPressedCallback(true) {

        init {
            activity.apply {
                onBackPressedDispatcher.addCallback(
                    owner,
                    this@BackPressHandler,
                )
            }
        }

        override fun handleOnBackPressed() {
            when (viewModel.tribeMemberProfileViewStateContainer.value) {
                is TribeMemberProfileViewState.Open -> {
                    viewModel.tribeMemberProfileViewStateContainer.updateViewState(
                        TribeMemberProfileViewState.Closed
                    )
                }
                is TribeMemberProfileViewState.FullScreen -> {
                    viewModel.tribeMemberProfileViewStateContainer.updateViewState(
                        TribeMemberProfileViewState.Open
                    )
                } else -> {
                    (tribeAppViewModel.webAppViewStateContainer.value as? WebAppViewState.AppAvailable.WebViewOpen)?.let {
                        tribeAppViewModel.webAppViewStateContainer.updateViewState(
                            WebAppViewState.AppAvailable.WebViewClosed(it.appUrl)
                        )
                        tribeAppViewModel.webViewLayoutScreenViewStateContainer.updateViewState(WebViewLayoutScreenViewState.Closed)
                    } ?: (viewModel.pinedMessageBottomViewState.value as? PinMessageBottomViewState.Open)?.let {
                        viewModel.pinedMessageBottomViewState.updateViewState(PinMessageBottomViewState.Closed)
                    } ?: run {
                        lifecycleScope.launch(viewModel.mainImmediate) {
                            viewModel.handleCommonChatOnBackPressed()
                        }
                    }
                }
            }
        }
    }

    override fun setupMoreOptionsMenu() {
        val menuOptions: MutableSet<MenuBottomOption> = LinkedHashSet(3)

        menuOptions.add(
            MenuBottomOption(
                text = chat.sphinx.chat_common.R.string.bottom_menu_more_option_call,
                textColor = chat.sphinx.chat_common.R.color.primaryBlueFontColor,
                onClick = {
                    viewModel.moreOptionsMenuHandler.updateViewState(
                        MenuBottomViewState.Closed
                    )
                    viewModel.callMenuHandler.updateViewState(
                        MenuBottomViewState.Open
                    )
                }
            )
        )

        menuOptions.add(
            MenuBottomOption(
                text = chat.sphinx.chat_common.R.string.bottom_menu_more_option_notification,
                textColor = chat.sphinx.chat_common.R.color.primaryBlueFontColor,
                onClick = {
                    viewModel.navigateToNotificationLevel()
                }
            )
        )

        if (viewModel.moreOptionsMenuStateFlow.value is MoreMenuOptionsViewState.OwnTribe) {
            menuOptions.add(
                MenuBottomOption(
                    text = chat.sphinx.chat_common.R.string.bottom_menu_more_option_share,
                    textColor = chat.sphinx.chat_common.R.color.primaryBlueFontColor,
                    onClick = {
                        viewModel.navigateToTribeShareScreen()
                    }
                )
            )
        }

        menuOptions.add(
            MenuBottomOption(
                text = chat.sphinx.chat_common.R.string.bottom_menu_more_option_search,
                textColor = chat.sphinx.chat_common.R.color.primaryBlueFontColor,
                onClick = {
                    lifecycleScope.launch(viewModel.mainImmediate) {
                        viewModel.searchMessages(null)
                    }
                }
            )
        )

        bottomMenuMore.newBuilder(moreMenuBinding, viewLifecycleOwner)
            .setHeaderText(chat.sphinx.chat_common.R.string.bottom_menu_more_header_text)
            .setOptions(menuOptions)
            .build()
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.moreOptionsMenuStateFlow.collect {
                setupMoreOptionsMenu()
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            tribeFeedViewModel.boostAnimationViewStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is BoostAnimationViewState.Idle -> {}

                    is BoostAnimationViewState.BoosAnimationInfo -> {
                        boostAnimationBinding.apply {

                            viewState.photoUrl?.let { photoUrl ->
                                imageLoader.load(
                                    imageViewProfilePicture,
                                    photoUrl.value,
                                    ImageLoaderOptions.Builder()
                                        .placeholderResId(chat.sphinx.podcast_player.R.drawable.ic_profile_avatar_circle)
                                        .transformation(Transformation.CircleCrop)
                                        .build()
                                )
                            }

                            textViewSatsAmount.text = viewState.amount?.asFormattedString()
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            tribeAppViewModel.budgetStateFlow.collect { sats ->
                tribeAppBinding.includeLayoutTribeAppDetails.textViewRemainingBudget.text =
                    String.format(getString(R.string.web_view_remaining_budget),
                    sats.value.toString()
                )
            }
        }

        // TODO: Remove hackery (utilized now to update podcast object's sats per minute
        //  value if it's changed from tribe detail screen)
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            tribeFeedViewModel.satsPerMinuteStateFlow.collect {
                /* no-op */
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            tribeFeedViewModel.podcastViewStateContainer.collect { viewState ->
                podcastPlayerBinding.apply {
                    when (viewState) {
                        is PodcastViewState.NoPodcast -> {
                            root.gone
                        }
                        is PodcastViewState.PodcastVS -> {

                            textViewPlayButton.goneIfFalse(viewState.showPlayButton && !viewState.showLoading)
                            animationViewPauseButton.goneIfFalse(!viewState.showPlayButton && !viewState.showLoading)

                            progressBar.progress = viewState.playingProgress

                            textViewEpisodeTitle.isSelected = !viewState.showPlayButton && !viewState.showLoading
                            textViewEpisodeTitle.text = viewState.title
                            textViewContributorTitle.text = viewState.subtitle

                            viewState.imageUrl?.let { imageUrl ->
                                imageLoader.load(
                                    imageViewPodcastEpisode,
                                    imageUrl,
                                    ImageLoaderOptions.Builder()
                                        .placeholderResId(chat.sphinx.podcast_player.R.drawable.ic_podcast_placeholder)
                                        .build()
                                )
                            }

                            imageViewForward30Button.goneIfFalse(!viewState.showLoading)
                            progressBarAudioLoading.goneIfFalse(viewState.showLoading)

                            scrollToBottom(callback = {
                                root.visible
                            })
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            tribeFeedViewModel.contributionsViewStateContainer.collect { viewState ->
                headerBinding.apply {

                    @Exhaustive
                    when (viewState) {
                        is PodcastContributionsViewState.Contributions -> {
                            textViewChatHeaderContributionsIcon.visible
                            textViewChatHeaderContributions.apply string@ {
                                this@string.visible
                                this@string.text = viewState.text
                            }
                        }
                        is PodcastContributionsViewState.None -> {
                            textViewChatHeaderContributionsIcon.gone
                            textViewChatHeaderContributions.gone
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.pinedMessagePopupViewState.collect { viewState ->
                layoutChatPinPopupBinding.apply {
                    @Exhaustive
                    when(viewState) {
                        is PinedMessagePopupViewState.Idle -> {
                            root.goneIfFalse(false)
                        }
                        is PinedMessagePopupViewState.Visible -> {
                            root.goneIfFalse(true)
                            includePinedMessagePopup.textViewPinedMessage.text = viewState.text
                        }
                    }
                }
            }
        }


        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.pinedMessageBottomViewState.collect { viewState ->
                layoutBottomPinned.apply {
                    layoutMotionBottomPinned.setTransitionDuration(150)
                    viewState.transitionToEndSet(layoutMotionBottomPinned)
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.pinedMessageDataViewState.collect { viewState ->
                layoutBottomPinned.apply {
                    @Exhaustive
                    when (viewState) {
                        is PinedMessageDataViewState.Idle -> {
                            binding.includeChatPinedMessageHeader.root.goneIfFalse(false)
                        }
                        is PinedMessageDataViewState.Data -> {
                            binding.includeChatPinedMessageHeader.apply {
                                root.goneIfFalse(true)
                                textViewChatHeaderName.text = viewState.messageContent
                            }

                            includeLayoutPinBottomTemplate.apply {
                                layoutConstraintPinnedBottomUnpinButton.goneIfFalse(viewState.isOwnTribe)

                                viewState.senderPic?.let { senderPhotoUrl ->
                                    imageLoader.load(
                                        messageHolderPinImageInitialHolder.imageViewChatPicture,
                                        senderPhotoUrl.value,
                                        ImageLoaderOptions.Builder()
                                            .placeholderResId(chat.sphinx.podcast_player.R.drawable.ic_profile_avatar_circle)
                                            .transformation(Transformation.CircleCrop)
                                            .build()
                                    )
                                }

                                viewState.senderAlias.let { senderAlias ->
                                    textViewPinnedBottomBodyUsername.text = senderAlias
                                    textViewPinnedBottomBodyUsername.setTextColor(
                                        Color.parseColor(
                                            userColorsHelper.getHexCodeForKey(
                                                viewState.senderColorKey,
                                                root.context.getRandomHexCode(),
                                            )
                                        )
                                    )
                                }

                                includePinnedBottomMessageHolder.apply {
                                    textViewPinnedBottomHeaderText.text = viewState.messageContent
                                }
                            }
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.threadHeaderViewState.collect { viewState ->
                threadHeader.apply {
                    @Exhaustive
                    when(viewState) {
                        is ThreadHeaderViewState.Idle -> {
                            root.gone
                        }
                        is ThreadHeaderViewState.BasicHeader -> {
                            root.visible

                            binding.includeChatTribeHeader.root.gone
                            binding.includeChatPinedMessageHeader.root.gone
                            threadOriginalMessageBinding?.root?.gone

                            layoutConstraintThreadContactName.gone
                            textViewHeader.visible
                        }
                        is ThreadHeaderViewState.FullHeader -> {
                            root.visible
                            binding.includeChatTribeHeader.root.gone
                            binding.includeChatPinedMessageHeader.root.gone

                            threadOriginalMessageBinding?.root?.visible

                            layoutConstraintThreadContactName.visible
                            textViewHeader.gone

                            textViewThreadDate.text = viewState.date
                            threadOriginalMessageBinding?.textViewThreadMessageContent?.text = viewState.message

                            binding.includeLayoutThreadHeader.layoutContactInitialHolder.apply {
                                textViewInitialsName.visible
                                imageViewChatPicture.gone

                                viewState.senderInfo?.let { senderInfo ->
                                    textViewContactHeaderName.text = senderInfo.second?.value ?: ""

                                    textViewInitialsName.apply {
                                        text = (senderInfo.second?.value ?: "").getInitials()

                                        senderInfo.third.let { colorKey ->
                                            setBackgroundRandomColor(
                                                chat.sphinx.chat_common.R.drawable.chat_initials_circle,
                                                Color.parseColor(
                                                    userColorsHelper.getHexCodeForKey(
                                                        colorKey,
                                                        root.context.getRandomHexCode(),
                                                    )
                                                ),
                                            )
                                        }

                                        senderInfo.first?.let { photoUrl ->
                                            textViewInitialsName.gone
                                            imageViewChatPicture.visible

                                            imageLoader.load(
                                                layoutContactInitialHolder.imageViewChatPicture,
                                                photoUrl.value,
                                                ImageLoaderOptions.Builder()
                                                    .placeholderResId(chat.sphinx.podcast_player.R.drawable.ic_profile_avatar_circle)
                                                    .transformation(Transformation.CircleCrop)
                                                    .build()
                                            )
                                        }
                                    }
                                }
                            }

                            threadOriginalMessageBinding?.apply {
                                layoutConstraintMediaContainer.gone

                                viewState.imageAttachment?.let { imageAttachment ->
                                    layoutConstraintMediaContainer.visible
                                    imageViewThreadCardView.visible

                                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                                        imageAttachment.media?.localFile?.let {
                                            imageLoader.load(
                                                imageViewElementPicture,
                                                it,
                                                ImageLoaderOptions.Builder().build()
                                            )
                                        } ?: imageAttachment.url.let {
                                            imageLoader.load(
                                                imageViewElementPicture,
                                                it,
                                                ImageLoaderOptions.Builder().build()
                                            )
                                        }
                                    }
                                }
                                
                                viewState.videoAttachment?.let { videoAttachment ->
                                    layoutConstraintMediaContainer.visible
                                    imageViewThreadCardView.visible

                                    (videoAttachment as? LayoutState.Bubble.ContainerSecond.VideoAttachment.FileAvailable)?.let {
                                        VideoThumbnailUtil.loadThumbnail(it.file)?.let { thumbnail ->
                                            imageViewElementPicture.setImageBitmap(thumbnail)
                                            imageViewAlpha.visible
                                            textViewAttachmentPlayButton.visible
                                        }
                                    }
                                }
                                
                                viewState.fileAttachment?.let { fileAttachment ->
                                    layoutConstraintMediaContainer.visible
                                    textViewAttachmentFileIcon.visible
                                    textViewAttachmentFileIcon.text = getString(chat.sphinx.chat_common.R.string.material_icon_name_file_pdf)

                                    (fileAttachment as? LayoutState.Bubble.ContainerSecond.FileAttachment.FileAvailable)?.let {
                                        threadOriginalMessageBinding?.textViewThreadMessageContent?.text = it.fileName?.value ?: "Unnamed File"
                                    }
                                }

                                viewState.audioAttachment?.let { audioAttachment ->
                                    layoutConstraintMediaContainer.visible
                                    textViewAttachmentFileIcon.visible
                                    textViewAttachmentFileIcon.text = getString(chat.sphinx.chat_common.R.string.material_icon_name_volume_up)
                                    threadOriginalMessageBinding?.textViewThreadMessageContent?.text = "Audio Clip"
                                }
                            }
                        }
                    }
                }
            }
        }


        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.tribeMemberProfileViewStateContainer.collect { viewState ->

                tribeMemberProfileBinding.includeLayoutTribeMemberProfileDetails.apply {
                    @Exhaustive
                    when (viewState) {
                        is TribeMemberProfileViewState.Closed -> {
                            includeLayoutTribeProfileInfoContainer.constraintLayoutTribeRecyclerRow.gone
                        }
                        is TribeMemberProfileViewState.Open -> {
                            includeLayoutTribeProfileInfoContainer.apply {
                                constraintLayoutTribeRecyclerRow.gone
                                constraintLayoutBadgesImageContainer.visible
                                layoutBadgesArrowDownContainer.gone
                            }

                            layoutConstraintTribeMemberContainer.layoutParams.height = resources.getDimensionPixelSize(R.dimen.tribe_member_collapsed_height)
                        }
                        is TribeMemberProfileViewState.FullScreen -> {
                            includeLayoutTribeProfileInfoContainer.apply {
                                constraintLayoutTribeRecyclerRow.visible
                                constraintLayoutBadgesImageContainer.gone
                                layoutBadgesArrowDownContainer.visible
                            }

                            delay(250L)

                            layoutConstraintTribeMemberContainer.layoutParams.height = 0
                        }
                    }
                }

                tribeMemberProfileBinding.root.setTransitionDuration(250)
                viewState.transitionToEndSet(tribeMemberProfileBinding.root)
            }
        }

        onStopSupervisor.scope.launch(tribeAppViewModel.mainImmediate) {
            tribeAppViewModel.webAppViewStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is WebAppViewState.NoApp -> {
                        binding.includeChatTribeHeader.imageViewChatWebView.gone
                    }
                    is WebAppViewState.AppAvailable.WebViewClosed -> {
                        binding.includeChatTribeHeader.imageViewChatWebView.visible

                        binding.includeChatTribeHeader.imageViewChatWebView.setImageDrawable(
                            ContextCompat.getDrawable(binding.root.context, R.drawable.ic_icon_web_view)
                        )
                    }
                    is WebAppViewState.AppAvailable.WebViewOpen.Loading -> {
                        binding.includeChatTribeHeader.imageViewChatWebView.visible

                        binding.includeChatTribeHeader.imageViewChatWebView.setImageDrawable(
                            ContextCompat.getDrawable(binding.root.context, R.drawable.ic_icon_web_view_chat)
                        )

                        tribeAppBinding.includeLayoutTribeAppDetails.layoutConstraintBudget.gone
                        tribeAppBinding.includeLayoutTribeAppDetails.layoutConstraintProgressBarContainer.visible

                        viewState.appUrl?.let { url ->
                            loadWebView(url.value)
                            tribeAppBinding.includeLayoutTribeAppDetails.textViewWebUrl.text = url.protocolLessUrl
                        }
                    }

                    is WebAppViewState.AppAvailable.WebViewOpen.Loaded -> {
                        tribeAppBinding.includeLayoutTribeAppDetails.layoutConstraintProgressBarContainer.gone
                        tribeAppBinding.includeLayoutTribeAppDetails.layoutConstraintBudget.visible
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            tribeAppViewModel.webViewLayoutScreenViewStateContainer.collect { viewState ->
                tribeAppBinding.includeLayoutTribeAppDetails.apply {
                    viewState.transitionToEndSet(tribeAppBinding.root)
                    tribeAppBinding.root.setTransitionDuration(250)
                }
            }
        }

        onStopSupervisor.scope.launch(tribeAppViewModel.mainImmediate) {
            tribeAppViewModel.webViewViewStateContainer.collect { viewState ->

                @Exhaustive
                when(viewState) {
                    is WebViewViewState.Idle -> {
                        tribeAppBinding.includeLayoutTribeAppDetails.layoutConstraintAuthorizePopup.gone
                    }

                    is WebViewViewState.RequestAuthorization -> {
                        tribeAppBinding.includeLayoutTribeAppDetails.layoutConstraintAuthorizePopup.visible
                    }
                    is WebViewViewState.SendAuthorization -> {
                        webView.evaluateJavascript(
                            viewState.script,
                            null
                        )
                    }
                    is WebViewViewState.SendMessage -> {
                        webView.evaluateJavascript(
                            viewState.script,
                            null
                        )

                        viewState.error?.let {
                            if (!it.isNullOrEmpty()) {
                                viewModel.submitSideEffect(
                                    ChatSideEffect.Notify(it)
                                )
                            }
                        }
                    }
                    is WebViewViewState.ChallengeError -> {
                        viewModel.submitSideEffect(
                            ChatSideEffect.Notify(viewState.error)
                        )
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.tribeMemberDataViewStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is TribeMemberDataViewState.Idle -> {
                        tribePopupBinding.root.gone
                    }
                    is TribeMemberDataViewState.TribeMemberPopup -> {
                        tribePopupBinding.apply {
                            root.visible

                            layoutChatTribePopup.apply {
                                textViewInitials.apply {
                                    text = viewState.memberName.value.getInitials()
                                    setBackgroundRandomColor(
                                        chat.sphinx.chat_common.R.drawable.chat_initials_circle,
                                        Color.parseColor(
                                            userColorsHelper.getHexCodeForKey(
                                                viewState.colorKey,
                                                root.context.getRandomHexCode(),
                                            )
                                        ),
                                    )
                                }

                                viewState.memberPic?.let { photoUrl ->
                                    imageViewMemberProfilePicture.visible

                                    imageLoader.load(
                                        imageViewMemberProfilePicture,
                                        photoUrl.value,
                                        ImageLoaderOptions.Builder()
                                            .placeholderResId(chat.sphinx.podcast_player.R.drawable.ic_profile_avatar_circle)
                                            .transformation(Transformation.CircleCrop)
                                            .build()
                                    )
                                }

                                textViewMemberName.text = viewState.memberName.value
                            }
                        }
                    }
                    is TribeMemberDataViewState.LoadingTribeMemberProfile -> {
                        tribePopupBinding.root.gone

                        tribeMemberProfileBinding.apply {
                            includeLayoutTribeMemberProfileDetails.apply {
                                includeLayoutLoadingPlaceholder.root.visible
                            }
                        }
                    }
                    is TribeMemberDataViewState.TribeMemberProfile -> {
                        tribePopupBinding.root.gone

                        tribeMemberProfileBinding.apply {
                            includeLayoutTribeMemberProfileDetails.apply {
                                includeLayoutLoadingPlaceholder.root.gone

                                includeLayoutTribeProfilePictureHolder.apply {
                                    textViewTribeProfileName.text = viewState.profile.owner_alias
                                    textViewTribeProfileDescription.text = viewState.profile.description

                                    viewState.profile.img.let { photoUrl ->
                                        imageViewTribeProfilePicture.goneIfFalse(photoUrl.isNotEmpty())
                                        textViewTribeProfileInitials.goneIfFalse(photoUrl.isEmpty())

                                        imageLoader.load(
                                            imageViewTribeProfilePicture,
                                            photoUrl,
                                            ImageLoaderOptions.Builder()
                                                .placeholderResId(chat.sphinx.podcast_player.R.drawable.ic_profile_avatar_circle)
                                                .transformation(Transformation.CircleCrop)
                                                .build()
                                        )
                                    }
                                }

                                includeLayoutTribeProfileInfoContainer.apply {
                                    textViewReputation.text = (viewState.leaderboard?.reputation ?: 0).toString()
                                    textViewSatsContributionsNumber.text = (viewState.leaderboard?.spent ?: 0).toString()
                                    textViewSatsEarningsNumber.text = (viewState.leaderboard?.earned ?: 0).toString()

                                    if (viewState.badges.isNullOrEmpty()) {
                                        constraintLayoutTribeRow1.gone
                                    } else {
                                        val badgesList = viewState.badges

                                        if (badgesList.size > 4) {
                                            textViewTribeBadgePictureNum.visible
                                            textViewTribeBadgePictureNum.text = "+${badgesList.size - 3}"

                                            cardViewBadgeImage1.visible
                                            cardViewBadgeImage2.visible
                                            cardViewBadgeImage3.visible
                                            cardViewBadgeImage4.invisible

                                            loadBadgeImage(imageViewTribeBadgePicture1, badgesList.getOrNull(0)?.icon)
                                            loadBadgeImage(imageViewTribeBadgePicture2, badgesList.getOrNull(1)?.icon)
                                            loadBadgeImage(imageViewTribeBadgePicture3, badgesList.getOrNull(2)?.icon)
                                        }
                                        else {
                                            textViewTribeBadgePictureNum.invisible

                                            cardViewBadgeImage1.invisible
                                            cardViewBadgeImage2.invisible
                                            cardViewBadgeImage3.invisible
                                            cardViewBadgeImage4.invisible

                                            badgesList.getOrNull(0)?.let {
                                                cardViewBadgeImage4.visible
                                                loadBadgeImage(imageViewTribeBadgePicture4, it.icon)
                                            }

                                            badgesList.getOrNull(1)?.let {
                                                cardViewBadgeImage3.visible
                                                loadBadgeImage(imageViewTribeBadgePicture3, it.icon)
                                            }

                                            badgesList.getOrNull(2)?.let {
                                                cardViewBadgeImage2.visible
                                                loadBadgeImage(imageViewTribeBadgePicture2, it.icon)
                                            }

                                            badgesList.getOrNull(3)?.let {
                                                cardViewBadgeImage1.visible
                                                loadBadgeImage(imageViewTribeBadgePicture1, it.icon)
                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.messageMentionsViewStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is MessageMentionsViewState.MessageMentions -> {
                        if (viewState.mentions.isNotEmpty()) {

                            val itemHeight = resources.getDimensionPixelSize(R.dimen.message_mention_item_height)
                            val listHeight = viewState.mentions.size.coerceAtMost(4) * itemHeight

                            mentionMembersPopup.listviewMentionTribeMembers.apply {
                                layoutParams.height = listHeight
                                requestLayout()

                                (adapter as? MessageMentionsAdapter)?.let {
                                    it.clear()
                                    it.addAll(viewState.mentions)
                                    it.notifyDataSetChanged()
                                }

                                this.smoothScrollToPosition(viewState.mentions.size)
                            }

                            mentionMembersPopup.root.visible
                        }
                        else mentionMembersPopup.root.gone
                    }
                }
            }
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun String.messageWithMention(mention: String): String {
    this.split(" ").last()?.let { partialTypedAlias ->
        return this.dropLast(partialTypedAlias.length) + "@$mention "
    }
    return this
}
