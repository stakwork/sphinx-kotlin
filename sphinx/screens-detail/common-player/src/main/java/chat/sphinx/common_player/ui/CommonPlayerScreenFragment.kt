package chat.sphinx.common_player.ui

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.common_player.BuildConfig
import chat.sphinx.common_player.R
import chat.sphinx.common_player.adapter.RecommendedItemsAdapter
import chat.sphinx.common_player.adapter.RecommendedItemsFooterAdapter
import chat.sphinx.common_player.databinding.FragmentCommonPlayerScreenBinding
import chat.sphinx.common_player.viewstate.BoostAnimationViewState
import chat.sphinx.common_player.viewstate.CommonPlayerScreenViewState
import chat.sphinx.common_player.viewstate.EpisodePlayerViewState
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.util.getHHMMSSString
import chat.sphinx.wrapper_feed.FeedRecommendation
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubeCommonPlayerSupportFragmentXKt
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.invisible
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
internal class CommonPlayerScreenFragment : SideEffectFragment<
        Context,
        CommonPlayerScreenSideEffect,
        CommonPlayerScreenViewState,
        CommonPlayerScreenViewModel,
        FragmentCommonPlayerScreenBinding
        >(R.layout.fragment_common_player_screen) {

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val binding: FragmentCommonPlayerScreenBinding by viewBinding(
        FragmentCommonPlayerScreenBinding::bind
    )
    override val viewModel: CommonPlayerScreenViewModel by viewModels()

    private var youtubePlayer: YouTubePlayer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val a: Activity? = activity
        a?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

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

        setupItems()
    }

    private fun toggleLoadingWheel(show: Boolean) {
        binding.apply {
            includeLayoutPlayersContainer.includeLayoutEpisodeSliderControl.apply layoutEpisodesSlider@ {
                this@layoutEpisodesSlider.progressBarAudioLoading.goneIfFalse(show)
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

    private suspend fun loadingEpisode(feedRecommendation: FeedRecommendation) {
        binding.apply {
            includeLayoutPlayerDescriptionAndControls.apply {
                textViewItemTitle.text = feedRecommendation.title
            }

            togglePlayPauseButton(true)

            includeLayoutPlayersContainer.apply {
                feedRecommendation.largestImageUrl?.let { imageUrl ->
                    imageLoader.load(
                        imageViewPodcastImage,
                        imageUrl,
                        ImageLoaderOptions.Builder()
                            .placeholderResId(feedRecommendation.getPlaceHolderImageRes())
                            .build()
                    )
                } ?: run {
                    imageViewPodcastImage.setImageDrawable(
                        ContextCompat.getDrawable(root.context, feedRecommendation.getPlaceHolderImageRes())
                    )
                }

                includeLayoutEpisodeSliderControl.apply {
                    val currentTime = feedRecommendation.currentTime.toLong()
                    val duration = (feedRecommendation.duration ?: 0).toLong()

                    textViewCurrentEpisodeDuration.text = duration.getHHMMSSString()
                    textViewCurrentEpisodeProgress.text = currentTime.getHHMMSSString()

                    val progress: Int =
                        try {
                            ((currentTime * 100) / duration).toInt()
                        } catch (e: ArithmeticException) {
                            0
                        }

                    seekBarCurrentEpisodeProgress.progress = progress

                    toggleLoadingWheel(true)
                }
            }
        }
    }

    private suspend fun seekTo(
        feedRecommendation: FeedRecommendation,
        progress: Int,
        speed: Double
    ) {
        val duration = withContext(viewModel.io) {
            feedRecommendation.getDuration(viewModel::retrieveItemDuration)
        }
        val seekTime = (duration * (progress.toDouble() / 100.toDouble())).toInt()
        viewModel.seekTo(seekTime, speed)
    }

    private fun updateViewAfterSeek(feedRecommendation: FeedRecommendation) {
        lifecycleScope.launch(viewModel.mainImmediate) {
            setTimeLabelsAndProgressBar(feedRecommendation)
        }
    }

    private suspend fun setTimeLabelsAndProgressBar(
        feedRecommendation: FeedRecommendation,
        currentTime: Long? = null,
        duration: Long? = null
    ) {
        val currentTime: Long = currentTime ?: feedRecommendation.currentTime.toLong()

        val duration = duration ?: withContext(viewModel.io) {
            feedRecommendation.getDuration(viewModel::retrieveItemDuration)
        }
        val progress: Int =
            try {
                ((currentTime * 100) / duration).toInt()
            } catch (e: ArithmeticException) {
                0
            }

        setTimeLabelsAndProgressBarTo(duration, currentTime, progress)
    }

    private fun setTimeLabelsAndProgressBarTo(
        duration: Long,
        currentTime: Long? = null,
        progress: Int
    ) {
        binding.includeLayoutPlayersContainer.includeLayoutEpisodeSliderControl.apply {
            val currentT: Double = currentTime?.toDouble() ?: (duration.toDouble() * (progress.toDouble()) / 100)

            textViewCurrentEpisodeDuration.text = duration.getHHMMSSString()
            textViewCurrentEpisodeProgress.text = currentT.toLong().getHHMMSSString()

            seekBarCurrentEpisodeProgress.progress = progress
        }
    }

    private suspend fun showFeedRecommendationInfo(
        feedRecommendation: FeedRecommendation,
        state: MediaPlayerServiceState.ServiceActive.MediaState? = null
    ) {
        binding.apply {

            includeLayoutPlayerDescriptionAndControls.apply {
                textViewItemTitle.text = feedRecommendation.title

                includeLayoutEpisodePlaybackControls.apply {
                    textViewPlaybackSpeedButton.text = (state?.speed ?: 1.0).getSpeedString()
                }
            }

            includeLayoutPlayersContainer.apply {
                feedRecommendation.largestImageUrl?.let { imageUrl ->
                    imageLoader.load(
                        imageViewPodcastImage,
                        imageUrl,
                        ImageLoaderOptions.Builder()
                            .placeholderResId(feedRecommendation.getPlaceHolderImageRes())
                            .build()
                    )
                } ?: run {
                    imageViewPodcastImage.setImageDrawable(
                        ContextCompat.getDrawable(root.context, feedRecommendation.getPlaceHolderImageRes())
                    )
                }
            }

            val playing = state is MediaPlayerServiceState.ServiceActive.MediaState.Playing
            togglePlayPauseButton(playing || feedRecommendation.isPlaying)

            (state as? MediaPlayerServiceState.ServiceActive.MediaState.Playing)?.let {

                if (!dragging)
                    setTimeLabelsAndProgressBar(
                        feedRecommendation,
                        it.currentTime.toLong(),
                        it.episodeDuration.toLong()
                    )
            } ?: run {
                if (!dragging) setTimeLabelsAndProgressBar(feedRecommendation)
            }

            binding.includeRecommendedItemsList.recyclerViewList.adapter?.notifyItemChanged(feedRecommendation.position)

            toggleLoadingWheel(false)
            addPlayerOnClickListeners(feedRecommendation)
        }
    }

    private var dragging: Boolean = false
    private fun addPlayerOnClickListeners(
        feedRecommendation: FeedRecommendation
    ) {
        binding.apply {
            val speed = includeLayoutPlayerDescriptionAndControls.includeLayoutEpisodePlaybackControls.textViewPlaybackSpeedButton.text.toString().toDoubleOrNull() ?: 1.0

            includeLayoutPlayersContainer.includeLayoutEpisodeSliderControl.apply {
                seekBarCurrentEpisodeProgress.setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {

                        override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            if (fromUser) {
                                val duration = feedRecommendation.getDuration(viewModel::retrieveItemDuration)
                                setTimeLabelsAndProgressBarTo(duration, null, progress)
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {
                            dragging = true
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                                val speed = includeLayoutPlayerDescriptionAndControls.includeLayoutEpisodePlaybackControls.textViewPlaybackSpeedButton.text.toString().toDoubleOrNull() ?: 1.0
                                seekTo(feedRecommendation, seekBar?.progress ?: 0, speed)
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

//                textViewShareClipButton.setOnClickListener {
//                    viewModel.shouldShareClip()
//                }

                textViewReplay15Button.setOnClickListener {
                    viewModel.seekTo(feedRecommendation.currentTime - 15000, speed)
                    updateViewAfterSeek(feedRecommendation)
                }

                textViewPlayPauseButton.setOnClickListener {
                    if (feedRecommendation.isPlaying) {
                        viewModel.pauseEpisode(feedRecommendation)
                    } else {
                        viewModel.playEpisode(feedRecommendation, feedRecommendation.currentTime, speed)
                    }
                }

                textViewForward30Button.setOnClickListener {
                    viewModel.seekTo(feedRecommendation.currentTime + 30000, speed)
                    updateViewAfterSeek(feedRecommendation)
                }

//                includeLayoutCustomBoost.apply customBoost@ {
//                    this@customBoost.imageViewFeedBoostButton.setOnClickListener {
//                        val amount = editTextCustomBoost.text.toString()
//                            .replace(" ", "")
//                            .toLongOrNull()?.toSat() ?: Sat(0)
//
//                        viewModel.sendPodcastBoost(
//                            amount,
//                            fireworksCallback = {
//                                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
//                                    setupBoostAnimation(null, amount)
//
//                                    includeLayoutBoostFireworks.apply fireworks@ {
//                                        this@fireworks.root.visible
//                                        this@fireworks.lottieAnimationView.playAnimation()
//                                    }
//                                }
//                            }
//                        )
//                    }
//                }
            }
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

    override suspend fun onViewStateFlowCollect(viewState: CommonPlayerScreenViewState) {
        @Exhaustive
        when(viewState) {
            is CommonPlayerScreenViewState.Idle -> {
                print("test")
            }
            is CommonPlayerScreenViewState.FeedRecommendations -> {
                binding.apply {
                    includeLayoutPlayerDescriptionAndControls.apply {
                        textViewItemTitle.text = viewState.selectedItem.title
                        textViewItemDescription.text = viewState.selectedItem.description
                        textViewItemPublishedDate.text = viewState.selectedItem.dateString
                    }
                    includeRecommendedItemsList.textViewListCount.text = viewState.recommendations.size.toString()

                    when(viewState) {
                        is CommonPlayerScreenViewState.FeedRecommendations.PodcastSelected -> {
                            youtubePlayer?.pause()

                            includeLayoutPlayersContainer.apply {
                                frameLayoutYoutubePlayer.gone
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
                            showFeedRecommendationInfo(viewState.selectedItem)
                        }
                        is CommonPlayerScreenViewState.FeedRecommendations.YouTubeVideoSelected -> {
                            includeLayoutPlayersContainer.apply {
                                frameLayoutYoutubePlayer.visible
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

                            if (youtubePlayer != null) {
                                youtubePlayer?.cueVideo(viewState.selectedItem.link.youTubeVideoId())
                            } else {
                                setupYoutubePlayer(viewState.selectedItem.link.youTubeVideoId())
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
            viewModel.episodePlayerViewStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is EpisodePlayerViewState.Idle -> {}

                    is EpisodePlayerViewState.ServiceLoading -> {
                        toggleLoadingWheel(true)
                    }
                    is EpisodePlayerViewState.ServiceInactive -> {
                        togglePlayPauseButton(false)
                    }

                    is EpisodePlayerViewState.EpisodeLoaded -> {
                        toggleLoadingWheel(true)
                        showFeedRecommendationInfo(viewState.feedRecommendation)
                    }

                    is EpisodePlayerViewState.LoadingEpisode -> {
                        loadingEpisode(viewState.feedRecommendation)
                    }

                    is EpisodePlayerViewState.EpisodePlayed -> {
                        showFeedRecommendationInfo(viewState.feedRecommendation)
                    }

                    is EpisodePlayerViewState.MediaStateUpdate -> {
                        toggleLoadingWheel(false)

                        showFeedRecommendationInfo(
                            viewState.feedRecommendation,
                            viewState.state
                        )
                    }
                }
            }
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

//            includeLayoutBoostFireworks.apply {
//
//                photoUrl?.let { photoUrl ->
//                    imageLoader.load(
//                        imageViewProfilePicture,
//                        photoUrl.value,
//                        ImageLoaderOptions.Builder()
//                            .placeholderResId(R.drawable.ic_profile_avatar_circle)
//                            .transformation(Transformation.CircleCrop)
//                            .build()
//                    )
//                }
//
//                textViewSatsAmount.text = amount?.asFormattedString()
//            }
        }
    }

    private fun setupItems() {
        binding.includeRecommendedItemsList.recyclerViewList.apply {
            val linearLayoutManager = LinearLayoutManager(context)
            val recommendedItemsAdapter = RecommendedItemsAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel,
            )
            val recommendedListFooterAdapter =
                RecommendedItemsFooterAdapter(requireActivity() as InsetterActivity)
            this.setHasFixedSize(false)

            layoutManager = linearLayoutManager
            adapter = ConcatAdapter(recommendedItemsAdapter, recommendedListFooterAdapter)
            itemAnimator = null
        }
    }

    private fun setupYoutubePlayer(videoId: String) {

        val youtubePlayerFragment = YouTubeCommonPlayerSupportFragmentXKt()

        childFragmentManager.beginTransaction()
            .replace(binding.includeLayoutPlayersContainer.frameLayoutYoutubePlayer.id, youtubePlayerFragment as Fragment)
            .commit()

        youtubePlayerFragment.initialize(
            BuildConfig.YOUTUBE_API_KEY,
            object : YouTubePlayer.OnInitializedListener {
                override fun onInitializationSuccess(
                    p0: YouTubePlayer.Provider?,
                    p1: YouTubePlayer?,
                    p2: Boolean
                ) {
                    p1?.let {
                        youtubePlayer = it
                    }
                    p1?.cueVideo(videoId)
                    p1?.setPlaybackEventListener(playbackEventListener)
                }

                override fun onInitializationFailure(
                    p0: YouTubePlayer.Provider?,
                    p1: YouTubeInitializationResult?
                ) {}
                private val playbackEventListener = object : YouTubePlayer.PlaybackEventListener {

                    override fun onSeekTo(p0: Int) {
                        Log.d("YouTubePlayer", "Youtube has seek $p0")
                    }
                    override fun onBuffering(p0: Boolean) {}

                    override fun onPlaying() {
                        playingVideoUpdate()
                        Log.d("YouTubePlayer", "Youtube is playing")
                    }
                    override fun onStopped() {
                        Log.d("YouTubePlayer", "Youtube has stopped")
                    }
                    override fun onPaused() {
                        playingVideoDidPause()
                        Log.d("YouTubePlayer", "Youtube is on pause")
                    }
                }
            })
    }

    private fun playingVideoDidPause() {
        (currentViewState as? CommonPlayerScreenViewState.FeedRecommendations.YouTubeVideoSelected)?.let {
            it.selectedItem.isPlaying = false
            binding.includeRecommendedItemsList.recyclerViewList.adapter?.notifyItemChanged(it.selectedItem.position)
        }
    }

    private fun playingVideoUpdate() {
        (currentViewState as? CommonPlayerScreenViewState.FeedRecommendations.YouTubeVideoSelected)?.let {
            it.selectedItem.isPlaying = true
            binding.includeRecommendedItemsList.recyclerViewList.adapter?.notifyItemChanged(it.selectedItem.position)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        (currentViewState as? CommonPlayerScreenViewState.FeedRecommendations.YouTubeVideoSelected)?.let { viewState ->
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
    }

}

@Suppress("NOTHING_TO_INLINE")
inline fun String.youTubeVideoId(): String {
    return this.substringAfterLast("v/").substringAfterLast("v=").substringBefore("?")
}

inline fun Double.getSpeedString(): String {
    if (this == 0.0) {
        return "1x"
    }
    if (this.roundToInt().toDouble() == this) {
        return "${this.toInt()}x"
    }
    return "${this}x"
}

inline fun FeedRecommendation.getPlaceHolderImageRes(): Int {
    if (isPodcast) {
        return R.drawable.ic_podcast_placeholder
    }
    if (isYouTubeVideo) {
        return R.drawable.ic_video_placeholder
    }
    if (isNewsletter) {
        return R.drawable.ic_newsletter_placeholder
    }
    return R.drawable.ic_podcast_placeholder
}