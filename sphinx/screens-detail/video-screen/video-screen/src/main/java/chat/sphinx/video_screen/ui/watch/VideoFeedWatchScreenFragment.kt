package chat.sphinx.video_screen.ui.watch

import android.animation.Animator
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.activity.OnBackPressedCallback
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.resources.inputMethodManager
import chat.sphinx.video_screen.R
import chat.sphinx.video_screen.adapter.VideoFeedItemsAdapter
import chat.sphinx.video_screen.adapter.VideoFeedItemsFooterAdapter
import chat.sphinx.video_screen.databinding.FragmentVideoWatchScreenBinding
import chat.sphinx.video_screen.ui.VideoFeedScreenSideEffect
import chat.sphinx.video_screen.ui.viewstate.*
import chat.sphinx.video_screen.ui.viewstate.LoadingVideoViewState
import chat.sphinx.video_screen.ui.viewstate.SelectedVideoViewState
import chat.sphinx.video_screen.ui.viewstate.VideoFeedScreenViewState
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.isTrue
import chat.sphinx.wrapper_common.feed.isYoutubeVideo
import chat.sphinx.wrapper_common.feed.youtubeVideoId
import chat.sphinx.wrapper_common.hhmmElseDate
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.toSat
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class VideoFeedWatchScreenFragment : SideEffectFragment<
        FragmentActivity,
        VideoFeedScreenSideEffect,
        VideoFeedScreenViewState,
        VideoFeedWatchScreenViewModel,
        FragmentVideoWatchScreenBinding
        >(R.layout.fragment_video_watch_screen) {
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_video_placeholder)
            .build()
    }

    override val binding: FragmentVideoWatchScreenBinding by viewBinding(
        FragmentVideoWatchScreenBinding::bind
    )
    override val viewModel: VideoFeedWatchScreenViewModel by viewModels()

    companion object {
        val SLIDER_VALUES = listOf(0,3,3,5,5,8,8,10,10,15,20,20,40,40,80,80,100)

        const val YOUTUBE_URL = "https://www.youtube.com"
        const val MIME_TYPE_HTML = "text/html"
        const val ENCODING_UTF = "UTF-8"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val a: Activity? = activity
        a?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        setupBoost()
        setupSeekBar()
        setupItems()
        cueYoutubeVideo()
        setupFeedItemDetails()
        setupFragmentLayout()
        setupYoutubePlayerIframe()

        BackPressHandler(viewLifecycleOwner, requireActivity())
    }

    private fun setupFragmentLayout() {
        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.includeLayoutFeedItem.includeLayoutFeedItemDetails.constraintLayoutFeedItem)
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
            if (viewModel.videoFeedItemDetailsViewState.value is VideoFeedItemDetailsViewState.Open) {
                viewModel.videoFeedItemDetailsViewState.updateViewState(VideoFeedItemDetailsViewState.Closed)
            } else {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        viewModel.createHistoryItem()
        viewModel.trackVideoConsumed()

        val a: Activity? = activity
        a?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    }

    private var draggingSatsSlider: Boolean = false
    private fun setupSeekBar() {
        binding.includeLayoutVideoItemsList.includeLayoutDescriptionBox.apply {
            seekBarSatsPerMinute.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        SLIDER_VALUES[progress].let {
                            textViewVideoSatsPerMinuteValue.text = it.toString()
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        draggingSatsSlider = true
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        draggingSatsSlider = false

                        seekBar?.let {
                            SLIDER_VALUES[seekBar.progress].let {
                                viewModel.updateSatsPerMinute(it.toLong())
                            }
                        }
                    }
                }
            )
        }
    }

    private fun setupBoost() {
        binding.apply {
            includeLayoutBoostFireworks.apply {
                lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {}

                    override fun onAnimationEnd(animation: Animator) {
                        root.gone
                    }

                    override fun onAnimationCancel(animation: Animator) {}

                    override fun onAnimationRepeat(animation: Animator) {}
                })

            }

            includeLayoutVideoItemsList.includeLayoutDescriptionBox.includeLayoutCustomBoost.apply {
                removeFocusOnEnter(editTextCustomBoost)

                imageViewFeedBoostButton.setOnClickListener {
                    val amount = editTextCustomBoost.text.toString()
                        .replace(" ", "")
                        .toLongOrNull()?.toSat() ?: Sat(0)

                    viewModel.sendBoost(
                        amount,
                        fireworksCallback = {
                            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                                setupBoostAnimation(null, amount)

                                includeLayoutBoostFireworks.apply fireworks@{
                                    this@fireworks.root.visible
                                    this@fireworks.lottieAnimationView.playAnimation()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun setupItems() {
        binding.includeLayoutVideoItemsList.let {
            it.recyclerViewVideoList.apply {
                val linearLayoutManager = LinearLayoutManager(context)
                val videoFeedItemsAdapter = VideoFeedItemsAdapter(
                    imageLoader,
                    viewLifecycleOwner,
                    onStopSupervisor,
                    viewModel,
                    viewModel
                )
                val videoListFooterAdapter =
                    VideoFeedItemsFooterAdapter(requireActivity() as InsetterActivity)
                this.setHasFixedSize(false)
                layoutManager = linearLayoutManager
                adapter = ConcatAdapter(videoFeedItemsAdapter, videoListFooterAdapter)
                itemAnimator = null
            }
        }
    }

    private fun cueYoutubeVideo() {
        binding.includeLayoutVideoPlayer.apply {

            viewModel.setVideoView(videoViewVideoPlayer)

            val controller = MediaController(binding.root.context)
            controller.setAnchorView(videoViewVideoPlayer)
            controller.setMediaPlayer(videoViewVideoPlayer)
            videoViewVideoPlayer.setMediaController(controller)

            binding.includeLayoutVideoItemsList.includeLayoutDescriptionBox.textViewSubscribeButton.setOnClickListener {
                viewModel.toggleSubscribeState()
            }
        }
    }

    private fun setupYoutubePlayerIframe() {
        var isSeeking = false

        binding.includeLayoutVideoPlayer.apply {
            webViewYoutubePlayer.settings.javaScriptEnabled = true

            webViewYoutubePlayer.webChromeClient = object : WebChromeClient() {

                private var customView: View? = null
                private var customViewCallback: CustomViewCallback? = null
                private var originalOrientation: Int = 0

                override fun onShowCustomView(view: View?, callback: CustomViewCallback) {
                    if (customView != null) {
                        onHideCustomView()
                        return
                    }

                    customView = view
                    originalOrientation = activity?.requestedOrientation ?: Configuration.ORIENTATION_UNDEFINED

                    customViewCallback = callback

                    val decor = activity?.window?.decorView as FrameLayout
                    decor.addView(customView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

                    ViewCompat.getWindowInsetsController(decor)?.let {
                        it.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                        it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }

                override fun onHideCustomView() {
                    val decor = activity?.window?.decorView as FrameLayout
                    decor.removeView(customView)
                    customView = null

                    ViewCompat.getWindowInsetsController(decor)?.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())

                    activity?.requestedOrientation = originalOrientation

                    customViewCallback?.onCustomViewHidden()
                    customViewCallback = null
                }

                override fun getDefaultVideoPoster(): Bitmap {
                    return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
                }
            }

            val videoStateListener = object : VideoStateListener {

                override fun onVideoSeek(time: Int) {
                    isSeeking = true
                    viewModel.setNewHistoryItem(time.toLong())
                    Log.d("YouTubePlayer", "Youtube has seek $time")
                }

                override fun onVideoPlaying() {
                    viewModel.startTimer(isSeeking)
                    viewModel.updateVideoLastPlayed()
                    isSeeking = false
                    Log.d("YouTubePlayer", "Youtube is playing")
                }
                override fun onVideoPaused() {
                    viewModel.stopTimer()
                    Log.d("YouTubePlayer", "Youtube is on pause")
                }

                override fun onVideoEnded() {
                    viewModel.stopTimer()
                    Log.d("YouTubePlayer", "Youtube video ended")
                }

                override fun onVideoReady() {}
                override fun onVideoBuffering() {}
                override fun onVideoUnstarted() {}
                override fun onVideoCued() {}
                override fun onVideoError(error: String) {}
                override fun onPlaybackQualityChange(quality: String) {}
                override fun onPlaybackRateChange(rate: String) {}
            }

            webViewYoutubePlayer.addJavascriptInterface(
                YoutubePlayerJavaScriptInterface(videoStateListener),
                "Android"
            )
        }
    }

    private fun cueYoutubeVideo(videoId: String) {
        binding.includeLayoutVideoPlayer.apply {
            val htmlContent = context?.assets?.open("youtube_iframe.html")?.bufferedReader()
                .use { it?.readText() }
            val formattedHtml = htmlContent?.replace("%%VIDEO_ID%%", videoId)
            formattedHtml?.let { html ->
                webViewYoutubePlayer.loadDataWithBaseURL(
                    YOUTUBE_URL,
                    html,
                    MIME_TYPE_HTML,
                    ENCODING_UTF,
                    null
                )
            }
        }
    }

    private suspend fun setupBoostAnimation(
        photoUrl: PhotoUrl?,
        amount: Sat?
    ) {
        binding.apply {
            includeLayoutVideoItemsList.includeLayoutDescriptionBox.includeLayoutCustomBoost.apply {
                editTextCustomBoost.setText(
                    (amount ?: Sat(100)).asFormattedString()
                )
            }

            includeLayoutBoostFireworks.apply {

                photoUrl?.let { photoUrl ->
                    imageLoader.load(
                        imageViewProfilePicture,
                        photoUrl.value,
                        ImageLoaderOptions.Builder()
                            .placeholderResId(R.drawable.ic_video_placeholder)
                            .transformation(Transformation.CircleCrop)
                            .build()
                    )
                }

                textViewSatsAmount.text = amount?.asFormattedString()
            }
        }
    }

    private fun removeFocusOnEnter(editText: EditText?) {
        editText?.setOnEditorActionListener(object :
            TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                    editText.let { nnEditText ->
                        binding.root.context.inputMethodManager?.let { imm ->
                            if (imm.isActive(nnEditText)) {
                                imm.hideSoftInputFromWindow(nnEditText.windowToken, 0)
                                nnEditText.clearFocus()
                            }
                        }
                    }
                    return true
                }
                return false
            }
        })
    }

    override suspend fun onViewStateFlowCollect(viewState: VideoFeedScreenViewState) {
        @Exhaustive
        when (viewState) {
            is VideoFeedScreenViewState.Idle -> {}

            is VideoFeedScreenViewState.FeedLoaded -> {
                binding.apply {
                    includeLayoutVideoItemsList.textViewVideosListCount.text =
                        viewState.items.count().toString()

                    includeLayoutVideoPlayer.apply {
                        includeLayoutVideoItemsList.includeLayoutDescriptionBox.apply {
                            textViewContributorName.text = viewState.title.value

                            viewState.imageToShow?.let {
                                imageLoader.load(
                                    imageViewContributorImage,
                                    it.value,
                                    imageLoaderOptions
                                )
                            }
                        }
                    }

                    includeLayoutVideoPlayer.apply {
                        includeLayoutVideoItemsList.includeLayoutDescriptionBox.apply {
                            val notLinkedToChat =
                                viewState.chatId?.value == ChatId.NULL_CHAT_ID.toLong()
                            textViewSubscribeButton.goneIfFalse(notLinkedToChat)

                            textViewSubscribeButton.text = if (viewState.subscribed.isTrue()) {
                                getString(R.string.unsubscribe)
                            } else {
                                getString(R.string.subscribe)
                            }

                            includeLayoutCustomBoost.apply customBoost@{
                                this@customBoost.layoutConstraintBoostButtonContainer.alpha =
                                    if (viewState.hasDestinations) 1.0f else 0.3f
                                this@customBoost.imageViewFeedBoostButton.isEnabled =
                                    viewState.hasDestinations
                                this@customBoost.editTextCustomBoost.isEnabled =
                                    viewState.hasDestinations
                            }

                            if (!draggingSatsSlider) {
                                val satsPerMinute = viewState.satsPerMinute?.value ?: 0
                                val closest = SLIDER_VALUES.closestValue(satsPerMinute.toInt())
                                val index = SLIDER_VALUES.indexOf(closest)
                                seekBarSatsPerMinute.max = SLIDER_VALUES.size - 1
                                seekBarSatsPerMinute.progress = index
                                textViewVideoSatsPerMinuteValue.text = viewState.satsPerMinute?.value.toString()

                                seekBarSatsPerMinute.alpha = if (viewState.hasDestinations) 1.0F else 0.5F

                                if (!viewState.hasDestinations) {
                                    seekBarSatsPerMinute.setOnTouchListener { _, _ -> true }
                                } else {
                                    seekBarSatsPerMinute.setOnTouchListener(null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.boostAnimationViewStateContainer.collect { viewState ->
                @app.cash.exhaustive.Exhaustive
                when (viewState) {
                    is BoostAnimationViewState.Idle -> {}

                    is BoostAnimationViewState.BoosAnimationInfo -> {
                        setupBoostAnimation(
                            viewState.photoUrl,
                            viewState.amount
                        )
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.selectedVideoStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is SelectedVideoViewState.Idle -> {}

                    is SelectedVideoViewState.VideoSelected -> {
                        binding.includeLayoutVideoPlayer.apply {
                            binding.includeLayoutVideoItemsList.includeLayoutDescriptionBox.apply {
                                textViewVideoTitle.text = viewState.title.value
                                textViewVideoDescription.text = viewState.description?.value ?: ""
                                textViewVideoPublishedDate.text = viewState.date?.hhmmElseDate()

                                if (viewState.url.isYoutubeVideo()) {
                                    viewModel.checkYoutubeVideoAvailable(viewState.id)
                                } else {
                                    val videoUri = if (viewState.localFile != null) {
                                        viewState.localFile.toUri()
                                    } else {
                                        viewState.url.value.toUri()
                                    }

                                    viewModel.videoPlayerStateContainer.updateViewState(VideoPlayerViewState.WebViewPlayer(videoUri, viewState.duration))
                                }
                            }
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.loadingVideoStateContainer.collect { viewState ->
                binding.includeLayoutVideoPlayer.apply {
                    @Exhaustive
                    when (viewState) {
                        is LoadingVideoViewState.Idle -> {}

                        is LoadingVideoViewState.MetaDataLoaded -> {
                            layoutConstraintLoadingVideo.gone
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.videoFeedItemDetailsViewState.collect { viewState ->

                binding.includeLayoutFeedItem.apply {
                    (viewState as? VideoFeedItemDetailsViewState.Open)?.let {
                        root.visible

                        includeLayoutFeedItemDetails.apply {
                            feedItemDetailsCommonInfoBinding(viewState)
                            layoutConstraintDownloadRow.gone
                            layoutConstraintCheckMarkRow.gone
                            circleSplitTwo.gone
                            textViewEpisodeDuration.gone
                        }
                    }

                    root.setTransitionDuration(300)
                    viewState.transitionToEndSet(root)

                    (viewState as? VideoFeedItemDetailsViewState.Closed)?.let {
                        delay(300L)
                        root.gone
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.videoPlayerStateContainer.collect { viewState ->
                binding.includeLayoutVideoPlayer.apply {
                @Exhaustive
                when (viewState) {
                    is VideoPlayerViewState.Idle -> {}
                    is VideoPlayerViewState.YoutubeVideoIframe -> {

                            layoutConstraintVideoViewContainer.gone
                            layoutConstraintYoutubeIframeContainer.visible

                            cueYoutubeVideo(viewState.videoId.youtubeVideoId())

                            viewModel.createHistoryItem()
                            viewModel.trackVideoConsumed()
                            viewModel.createVideoRecordConsumed(viewState.videoId)

                    }
                    is VideoPlayerViewState.WebViewPlayer -> {

                            layoutConstraintLoadingVideo.visible
                            layoutConstraintVideoViewContainer.visible
                            layoutConstraintYoutubeIframeContainer.gone

                            viewModel.initializeVideo(
                                viewState.videoUri,
                                viewState.duration?.value?.toInt()
                            )
                        }

                    }
                }
            }
        }

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val currentOrientation = resources.configuration.orientation

        binding.includeLayoutVideoPlayer.layoutConstraintVideoPlayers.apply {
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                layoutParams.height = binding.root.measuredWidth - (requireActivity() as InsetterActivity).statusBarInsetHeight.top
            } else {
                layoutParams.height = resources.getDimension(R.dimen.video_player_height).toInt()
            }
            requestLayout()
        }
    }

    private fun feedItemDetailsCommonInfoBinding(viewState: VideoFeedItemDetailsViewState.Open) {
        binding.includeLayoutFeedItem.includeLayoutFeedItemDetails.apply {
            textViewMainEpisodeTitle.text = viewState.feedItemDetail?.header
            imageViewItemRowEpisodeType.setImageDrawable(ContextCompat.getDrawable(root.context, viewState.feedItemDetail?.episodeTypeImage ?: R.drawable.ic_podcast_type))
            textViewEpisodeType.text = viewState.feedItemDetail?.episodeTypeText
            textViewEpisodeDate.text = viewState.feedItemDetail?.episodeDate
            textViewEpisodeDuration.text = viewState.feedItemDetail?.episodeDuration
            textViewPodcastName.text = viewState.feedItemDetail?.podcastName

            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                viewState.feedItemDetail?.image?.let {
                    imageLoader.load(
                        imageViewEpisodeDetailImage,
                        it,
                        ImageLoaderOptions.Builder()
                            .placeholderResId(R.drawable.ic_podcast_placeholder)
                            .build()
                    )
                }
            }
        }
    }

    private fun setupFeedItemDetails() {
        binding.includeLayoutFeedItem.apply {
            root.gone

            includeLayoutFeedItemDetails.apply {
                textViewClose.setOnClickListener {
                    viewModel.videoFeedItemDetailsViewState.updateViewState(
                        VideoFeedItemDetailsViewState.Closed
                    )
                }
                layoutConstraintCopyLinkRow.setOnClickListener {
                    (viewModel.videoFeedItemDetailsViewState.value as? VideoFeedItemDetailsViewState.Open)?.let { viewState ->
                        viewState.feedItemDetail?.link?.let { link ->
                            viewModel.copyCodeToClipboard(link)
                        }
                    }
                }
                layoutConstraintShareRow.setOnClickListener {
                    (viewModel.videoFeedItemDetailsViewState.value as? VideoFeedItemDetailsViewState.Open)?.let { viewState ->
                        viewState.feedItemDetail?.link?.let { link ->
                            viewModel.share(link, binding.root.context)
                        }
                    }
                }
            }
        }
    }

    private fun List<Int>.closestValue(value: Int) = minByOrNull {
        kotlin.math.abs(value - it)
    }

    override suspend fun onSideEffectCollect(sideEffect: VideoFeedScreenSideEffect) {
        sideEffect.execute(requireActivity())
    }
}
