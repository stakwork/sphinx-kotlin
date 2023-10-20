package chat.sphinx.common_player.ui

import android.animation.Animator
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.common_player.R
import chat.sphinx.common_player.adapter.RecommendedItemsAdapter
import chat.sphinx.common_player.adapter.RecommendedItemsFooterAdapter
import chat.sphinx.common_player.databinding.FragmentCommonPlayerScreenBinding
import chat.sphinx.common_player.viewstate.BoostAnimationViewState
import chat.sphinx.common_player.viewstate.PlayerViewState
import chat.sphinx.common_player.viewstate.RecommendationsPodcastPlayerViewState
import chat.sphinx.common_player.viewstate.RecommendedFeedItemDetailsViewState
import chat.sphinx.concept_connectivity_helper.ConnectivityHelper
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.resources.inputMethodManager
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_common.util.getHHMMSSString
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastEpisode
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.invisible
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
internal class CommonPlayerScreenFragment : SideEffectFragment<
        Context,
        CommonPlayerScreenSideEffect,
        RecommendationsPodcastPlayerViewState,
        CommonPlayerScreenViewModel,
        FragmentCommonPlayerScreenBinding
        >(R.layout.fragment_common_player_screen) {

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var connectivityHelper: ConnectivityHelper

    override val binding: FragmentCommonPlayerScreenBinding by viewBinding(
        FragmentCommonPlayerScreenBinding::bind
    )
    override val viewModel: CommonPlayerScreenViewModel by viewModels()

    companion object {
        const val YOUTUBE_URL = "https://www.youtube.com"
        const val MIME_TYPE_HTML = "text/html"
        const val ENCODING_UTF = "UTF-8"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val a: Activity? = activity
        a?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        BackPressHandler(viewLifecycleOwner, requireActivity())

        binding.apply {

            includeLayoutPlayerDescriptionAndControls.includeLayoutEpisodePlaybackControls.apply {
                includeLayoutCustomBoost.apply {
                    root.alpha = 0.3F
                    editTextCustomBoost.isEnabled = false
                    imageViewFeedBoostButton.isEnabled = false
                }

                textViewShareClipButton.alpha = 0.3F
                textViewShareClipButton.isEnabled = false
            }
        }

        setupBoost()
        setupItems()
        setupFeedItemDetails()
        setupFragmentLayout()
        setupYoutubePlayerIframe()
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
            if (viewModel.recommendedFeedItemDetailsViewState.value is RecommendedFeedItemDetailsViewState.Open) {
                viewModel.recommendedFeedItemDetailsViewState.updateViewState(RecommendedFeedItemDetailsViewState.Closed)
            } else {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }
    }

    private fun setupBoost() {
        binding.apply {
            includeLayoutBoostFireworks.apply {
                lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener {
                    override fun onAnimationEnd(animation: Animator) {
                        root.gone
                    }

                    override fun onAnimationRepeat(animation: Animator) {}

                    override fun onAnimationCancel(animation: Animator) {}

                    override fun onAnimationStart(animation: Animator) {}
                })
            }

            includeLayoutPlayerDescriptionAndControls.includeLayoutEpisodePlaybackControls.includeLayoutCustomBoost.apply {
                removeFocusOnEnter(editTextCustomBoost)
            }
        }
    }

    private fun removeFocusOnEnter(editText: EditText?) {
        editText?.setOnEditorActionListener(object:
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

    override fun onDestroyView() {
        super.onDestroyView()

        viewModel.trackPodcastConsumed()
        viewModel.createHistoryItem()
        viewModel.trackVideoConsumed()

        val a: Activity? = activity
        a?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    }

    private fun setupItems() {
        binding.includeRecommendedItemsList.recyclerViewList.apply {
            val linearLayoutManager = LinearLayoutManager(context)
            val recommendedItemsAdapter = RecommendedItemsAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel,
                connectivityHelper
            )
            val recommendedListFooterAdapter =
                RecommendedItemsFooterAdapter(requireActivity() as InsetterActivity)
            this.setHasFixedSize(false)

            layoutManager = linearLayoutManager
            adapter = ConcatAdapter(recommendedItemsAdapter, recommendedListFooterAdapter)
            itemAnimator = null
        }
    }

    private fun setupFeedItemDetails() {
        binding.includeLayoutFeedItem.includeLayoutFeedItemDetails.apply {
            textViewClose.setOnClickListener {
                viewModel.recommendedFeedItemDetailsViewState.updateViewState(
                    RecommendedFeedItemDetailsViewState.Closed
                )
            }
            layoutConstraintCopyLinkRow.setOnClickListener {
                (viewModel.recommendedFeedItemDetailsViewState.value as? RecommendedFeedItemDetailsViewState.Open)?.let { viewState ->
                    viewState.feedItemDetail?.link?.let { link ->
                        viewModel.copyCodeToClipboard(link)
                    }
                }
            }
            layoutConstraintShareRow.setOnClickListener {
                (viewModel.recommendedFeedItemDetailsViewState.value as? RecommendedFeedItemDetailsViewState.Open)?.let { viewState ->
                    viewState.feedItemDetail?.link?.let { link ->
                        viewModel.share(link, binding.root.context)
                    }
                }
            }
        }
    }

    private var dragging: Boolean = false
    private fun addPodcastOnClickListeners(podcast: Podcast) {
        binding.apply {
            includeLayoutPlayersContainer.includeLayoutRecommendationSliderControl.apply {
                seekBarCurrentEpisodeProgress.setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {

                        override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            if (fromUser) {
                                val duration = podcast.getCurrentEpisodeDuration(viewModel::retrieveEpisodeDuration)
                                setTimeLabelsAndProgressBarTo(duration, null, progress)
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {
                            dragging = true
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                                seekTo(podcast, seekBar?.progress ?: 0)
                            }
                            dragging = false
                        }
                    }
                )
            }

            includeLayoutPlayerDescriptionAndControls.includeLayoutEpisodePlaybackControls.apply {
                textViewPlaybackSpeedButton.setOnClickListener {
                    showSpeedPopup()
                }

                textViewReplay15Button.setOnClickListener {
                    viewModel.seekTo(podcast.timeMilliSeconds - 15000L)
                    updateViewAfterSeek(podcast)
                }

                textViewPlayPauseButton.setOnClickListener {
                    val currentEpisode = podcast.getCurrentEpisode()

                    if (currentEpisode.playing) {
                        viewModel.pauseEpisode(currentEpisode)
                    } else {
                        viewModel.playEpisode(currentEpisode, podcast.timeMilliSeconds)
                    }
                }

                textViewForward30Button.setOnClickListener {
                    viewModel.seekTo(podcast.timeMilliSeconds + 30000L)
                    updateViewAfterSeek(podcast)
                }
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: RecommendationsPodcastPlayerViewState) {
        @Exhaustive
        when (viewState) {
            is RecommendationsPodcastPlayerViewState.Idle -> {}

            is RecommendationsPodcastPlayerViewState.ServiceLoading -> {
                toggleLoadingWheel(true)
            }
            is RecommendationsPodcastPlayerViewState.ServiceInactive -> {
                togglePlayPauseButton(false)
            }

            is RecommendationsPodcastPlayerViewState.PodcastViewState.PodcastLoaded -> {
                showPodcastInfo(viewState.podcast)
            }

            is RecommendationsPodcastPlayerViewState.PodcastViewState.LoadingEpisode -> {
                loadingEpisode(viewState.episode)
            }

            is RecommendationsPodcastPlayerViewState.PodcastViewState.EpisodePlayed -> {
                showPodcastInfo(viewState.podcast)
            }

            is RecommendationsPodcastPlayerViewState.PodcastViewState.MediaStateUpdate -> {
                toggleLoadingWheel(false)
                showPodcastInfo(viewState.podcast)
            }
        }
    }

    private suspend fun showPodcastInfo(podcast: Podcast) {
        binding.apply {

            var currentEpisode: PodcastEpisode = podcast.getCurrentEpisode()

            includeLayoutPlayerDescriptionAndControls.apply {
                textViewItemTitle.text = currentEpisode.description?.value ?: "-"
                textViewItemDescription.text = currentEpisode.title.value
                textViewItemPublishedDate.text = currentEpisode.dateString
            }

            includeLayoutPlayersContainer.apply {
                podcast.imageToShow?.value?.let { podcastImage ->
                    imageLoader.load(
                        imageViewPodcastImage,
                        podcastImage,
                        ImageLoaderOptions.Builder()
                            .placeholderResId(currentEpisode.getPlaceHolderImageRes())
                            .build()
                    )
                }
            }

            includeRecommendedItemsList.textViewListCount.text = podcast.episodesCount.toString()

            includeLayoutPlayerDescriptionAndControls.includeLayoutEpisodePlaybackControls.apply {
                textViewPlaybackSpeedButton.text = "${podcast.getSpeedString()}"

                includeLayoutCustomBoost.apply customBoost@ {
                    this@customBoost.imageViewFeedBoostButton.setOnClickListener {
                        val amount = editTextCustomBoost.text.toString()
                            .replace(" ", "")
                            .toLongOrNull()?.toSat() ?: Sat(0)

                        viewModel.sendPodcastBoost(
                            amount,
                            fireworksCallback = {
                                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                                    setupBoostAnimation(null, amount)

                                    includeLayoutBoostFireworks.apply fireworks@ {
                                        this@fireworks.root.visible
                                        this@fireworks.lottieAnimationView.playAnimation()
                                    }
                                }
                            }
                        )
                    }

                    this@customBoost.layoutConstraintBoostButtonContainer.alpha = if (currentEpisode.isBoostAllowed) 1.0f else 0.3f
                    this@customBoost.imageViewFeedBoostButton.isEnabled = currentEpisode.isBoostAllowed
                    this@customBoost.editTextCustomBoost.isEnabled = currentEpisode.isBoostAllowed
                }
            }

            togglePlayPauseButton(podcast.isPlaying)

            if (!dragging && currentEpisode != null) setTimeLabelsAndProgressBar(podcast)

            toggleLoadingWheel(false)
            addPodcastOnClickListeners(podcast)
        }
    }

    private suspend fun setupBoostAnimation(
        photoUrl: PhotoUrl?,
        amount: Sat?
    ) {

        binding.apply {
            includeLayoutPlayerDescriptionAndControls.includeLayoutEpisodePlaybackControls.includeLayoutCustomBoost.apply {
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
                            .placeholderResId(R.drawable.ic_podcast_placeholder)
                            .transformation(Transformation.CircleCrop)
                            .build()
                    )
                }

                textViewSatsAmount.text = amount?.asFormattedString()
            }
        }
    }

    private fun toggleLoadingWheel(show: Boolean) {
        binding.apply {
            includeLayoutPlayersContainer.includeLayoutRecommendationSliderControl.apply layoutRecommendationSlider@ {
                this@layoutRecommendationSlider.progressBarAudioLoading.goneIfFalse(show)
            }
            includeLayoutPlayerDescriptionAndControls.includeLayoutEpisodePlaybackControls.apply layoutPlaybackControls@ {
                this@layoutPlaybackControls.textViewPlayPauseButton.isEnabled = !show
            }
        }
    }

    private fun togglePlayPauseButton(playing: Boolean) {
        binding.apply {
            includeLayoutPlayerDescriptionAndControls.includeLayoutEpisodePlaybackControls.apply {
                textViewPlayPauseButton.background =
                    ContextCompat.getDrawable(root.context,
                        if (playing) R.drawable.ic_podcast_pause_circle else R.drawable.ic_podcast_play_circle
                    )
            }
        }
    }

    private suspend fun loadingEpisode(episode: PodcastEpisode) {
        binding.apply {
            includeLayoutPlayerDescriptionAndControls.apply {
                textViewItemTitle.text = episode.description?.value ?: "-"
                textViewItemDescription.text = episode.title.value
                textViewItemPublishedDate.text = episode.dateString
            }

            includeLayoutPlayersContainer.apply {

                episode.image?.value?.let { podcastImage ->
                    imageLoader.load(
                        imageViewPodcastImage,
                        podcastImage,
                        ImageLoaderOptions.Builder()
                            .placeholderResId(R.drawable.ic_podcast_placeholder)
                            .build()
                    )
                }

                includeLayoutRecommendationSliderControl.apply {
                    textViewCurrentEpisodeDuration.text = 0.toLong().getHHMMSSString()
                    textViewCurrentEpisodeProgress.text = 0.toLong().getHHMMSSString()

                    seekBarCurrentEpisodeProgress.progress = 0

                    setTimeLabelsAndProgressBarTo(0, 0, 0)
                    setClipView(0F, 0F)

                    toggleLoadingWheel(true)
                }
            }
        }
    }

    private suspend fun seekTo(podcast: Podcast, progress: Int) {
        val duration = withContext(viewModel.io) {
            podcast.getCurrentEpisodeDuration(viewModel::retrieveEpisodeDuration)
        }
        val seekTime = (duration * (progress.toDouble() / 100.toDouble())).toLong()
        viewModel.seekTo(seekTime)
    }

    private fun updateViewAfterSeek(podcast: Podcast) {
        lifecycleScope.launch(viewModel.mainImmediate) {
            setTimeLabelsAndProgressBar(podcast)
        }
    }

    private suspend fun setTimeLabelsAndProgressBar(podcast: Podcast) {
        val currentTime = podcast.timeMilliSeconds

        toggleLoadingWheel(podcast.shouldLoadDuration)

        val duration = withContext(viewModel.io) {
            podcast.getCurrentEpisodeDuration(viewModel::retrieveEpisodeDuration)
        }
        val progress: Int =
            try {
                ((currentTime * 100) / duration).toInt()
            } catch (e: ArithmeticException) {
                0
            }

        setTimeLabelsAndProgressBarTo(duration, currentTime, progress)

        setClipView(
            duration,
            (podcast.getCurrentEpisode().clipStartTime ?: 0).toLong(),
            (podcast.getCurrentEpisode().clipEndTime ?: 0).toLong()
        )
    }

    private fun setClipView(duration: Long, clipStartTime: Long, clipEndTime: Long) {
        val startTimeProgress: Int =
            try {
                ((clipStartTime * 100) / duration).toInt()
            } catch (e: ArithmeticException) {
                0
            }

        val durationProgress: Int =
            try {
                (((clipEndTime - clipStartTime) * 100) / duration).toInt()
            } catch (e: ArithmeticException) {
                0
            }

        setClipView(
            startTimeProgress.toFloat(),
            durationProgress.toFloat()
        )
    }

    private fun setClipView(startTimeProgress: Float, durationProgress: Float) {
        binding.includeLayoutPlayersContainer.includeLayoutRecommendationSliderControl.apply {
            val startTimeParams: LinearLayout.LayoutParams = viewClipStart.layoutParams as LinearLayout.LayoutParams
            startTimeParams.weight = startTimeProgress
            viewClipStart.layoutParams = startTimeParams

            val endTimeParams: LinearLayout.LayoutParams = viewClipDuration.layoutParams as LinearLayout.LayoutParams
            endTimeParams.weight = durationProgress
            viewClipDuration.layoutParams = endTimeParams
        }
    }

    private fun setTimeLabelsAndProgressBarTo(duration: Long, currentTime: Long? = null, progress: Int) {
        binding.includeLayoutPlayersContainer.includeLayoutRecommendationSliderControl.apply {
            val currentT: Double = currentTime?.toDouble() ?: (duration.toDouble() * (progress.toDouble()) / 100)

            textViewCurrentEpisodeDuration.text = duration.getHHMMSSString()
            textViewCurrentEpisodeProgress.text = currentT.toLong().getHHMMSSString()

            seekBarCurrentEpisodeProgress.progress = progress
        }
    }

    private fun showSpeedPopup() {
        binding.includeLayoutPlayerDescriptionAndControls.includeLayoutEpisodePlaybackControls.apply {
            val wrapper: Context = ContextThemeWrapper(context, R.style.speedMenu)
            val popup = PopupMenu(wrapper, textViewPlaybackSpeedButton)
            popup.inflate(R.menu.speed_menu)

            popup.setOnMenuItemClickListener { item: MenuItem? ->
                when (item!!.itemId) {
                    R.id.speed0_5 -> {
                        textViewPlaybackSpeedButton.text = "0.5x"
                        viewModel.adjustSpeed(0.5)
                    }
                    R.id.speed0_8 -> {
                        textViewPlaybackSpeedButton.text = "0.8x"
                        viewModel.adjustSpeed(0.8)
                    }
                    R.id.speed1 -> {
                        textViewPlaybackSpeedButton.text = "1x"
                        viewModel.adjustSpeed(1.0)
                    }
                    R.id.speed1_2 -> {
                        textViewPlaybackSpeedButton.text = "1.2x"
                        viewModel.adjustSpeed(1.2)
                    }
                    R.id.speed1_5 -> {
                        textViewPlaybackSpeedButton.text = "1.5x"
                        viewModel.adjustSpeed(1.5)
                    }
                    R.id.speed2_1 -> {
                        textViewPlaybackSpeedButton.text = "2.1x"
                        viewModel.adjustSpeed(2.1)
                    }
                }
                true
            }

            popup.show()
        }
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.boostAnimationViewStateContainer.collect { viewState ->
                @Exhaustive
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
            viewModel.recommendedFeedItemDetailsViewState.collect { viewState ->

                binding.includeRecommendedItemsList.recyclerViewList.adapter?.notifyDataSetChanged()

                binding.includeLayoutFeedItem.apply {
                    when (viewState) {
                        is RecommendedFeedItemDetailsViewState.Open -> {
                            includeLayoutFeedItemDetails.apply {
                                feedItemDetailsCommonInfoBinding(viewState)
                                layoutConstraintDownloadRow.gone
                                layoutConstraintCheckMarkRow.gone
                                circleSplitTwo.gone
                                textViewEpisodeDuration.gone
                            }

                        }
                        else -> {}
                    }

                    root.setTransitionDuration(300)
                    viewState.transitionToEndSet(root)
                }
            }
        }


        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.playerViewStateContainer.collect { viewState ->
                binding.apply {
                    @Exhaustive
                    when (viewState) {
                        is PlayerViewState.Idle -> {}

                        is PlayerViewState.PodcastEpisodeSelected -> {

                            includeLayoutPlayersContainer.apply {
                                webViewYoutubePlayer.gone
                                imageViewPodcastImage.visible
                                layoutConstraintSliderContainer.visible
                            }
                            includeLayoutPlayerDescriptionAndControls.includeLayoutEpisodePlaybackControls.apply {
                                textViewShareClipButton.visible
                                textViewForward30Button.visible
                                textViewReplay15Button.visible
                                textViewPlayPauseButton.visible
                                textViewPlaybackSpeedButton.visible
                                imageViewPlayPauseButton.visible
                            }
                        }

                        is PlayerViewState.YouTubeVideoSelected -> {
                            includeLayoutPlayersContainer.apply {
                                webViewYoutubePlayer.visible
                                imageViewPodcastImage.gone
                                layoutConstraintSliderContainer.gone
                            }
                            includeLayoutPlayerDescriptionAndControls.includeLayoutEpisodePlaybackControls.apply {
                                textViewShareClipButton.invisible
                                textViewForward30Button.invisible
                                textViewReplay15Button.invisible
                                textViewPlayPauseButton.invisible
                                textViewPlaybackSpeedButton.invisible
                                imageViewPlayPauseButton.invisible
                            }

                            cueYoutubeVideo(viewState.episode.enclosureUrl.value.youTubeVideoId())

                            viewModel.createHistoryItem()
                            viewModel.trackVideoConsumed()
                            viewModel.createVideoRecordConsumed(viewState.episode.id)
                        }
                    }
                }
            }
        }
    }

    private fun feedItemDetailsCommonInfoBinding(viewState: RecommendedFeedItemDetailsViewState.Open) {
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

    private fun setupYoutubePlayerIframe() {
        var isSeeking = false

        binding.includeLayoutPlayersContainer.apply {
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
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        viewModel.playingVideoUpdate()
                        binding.includeRecommendedItemsList.recyclerViewList.adapter?.notifyDataSetChanged()
                    }
                    viewModel.startTimer()
                    isSeeking = false

                    Log.d("YouTubePlayer", "Youtube is playing")
                }
                override fun onVideoPaused() {
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        viewModel.playingVideoDidPause()
                        binding.includeRecommendedItemsList.recyclerViewList.adapter?.notifyDataSetChanged()
                    }
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
        binding.includeLayoutPlayersContainer.apply {

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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        (viewModel.playerViewStateContainer.value as? PlayerViewState.YouTubeVideoSelected)?.let {
            val currentOrientation = resources.configuration.orientation

            binding.includeLayoutPlayersContainer.layoutConstraintPlayers.apply {
                if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    layoutParams.height =
                        binding.root.measuredWidth - (requireActivity() as InsetterActivity).statusBarInsetHeight.top
                } else {
                    layoutParams.height = resources.getDimension(R.dimen.video_player_height).toInt()
                }
                requestLayout()
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: CommonPlayerScreenSideEffect) {
        sideEffect.execute(requireActivity())
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun String.youTubeVideoId(): String {
    return this.substringAfterLast("v/").substringAfterLast("v=").substringBefore("?")
}

inline fun PodcastEpisode.getPlaceHolderImageRes(): Int {
    if (isMusicClip) {
        return R.drawable.ic_podcast_placeholder
    }
    if (isYouTubeVideo) {
        return R.drawable.ic_video_placeholder
    }
    return R.drawable.ic_podcast_placeholder
}