package chat.sphinx.common_player.ui

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
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
import chat.sphinx.common_player.viewstate.PlayerViewState
import chat.sphinx.common_player.viewstate.RecommendationsPodcastPlayerViewState
import chat.sphinx.concept_connectivity_helper.ConnectivityHelper
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.util.getHHMMSSString
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastEpisode
import com.google.android.youtube.player.YouTubeCommonPlayerSupportFragmentXKt
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
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

    private val args: CommonPlayerScreenFragmentArgs by navArgs()

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
                    viewModel.seekTo(podcast.currentTime - 15000)
                    updateViewAfterSeek(podcast)
                }

                textViewPlayPauseButton.setOnClickListener {
                    val currentEpisode = podcast.getCurrentEpisode()

                    if (currentEpisode.playing) {
                        viewModel.pauseEpisode(currentEpisode)
                    } else {
                        viewModel.playEpisode(currentEpisode, podcast.currentTime)
                    }
                }

                textViewForward30Button.setOnClickListener {
                    viewModel.seekTo(podcast.currentTime + 30000)
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
                toggleLoadingWheel(true)
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
                    this@customBoost.layoutConstraintBoostButtonContainer.alpha = if (podcast.hasDestinations) 1.0f else 0.3f
                    this@customBoost.imageViewFeedBoostButton.isEnabled = podcast.hasDestinations
                    this@customBoost.editTextCustomBoost.isEnabled = podcast.hasDestinations
                }
            }

            togglePlayPauseButton(podcast.isPlaying)

            if (!dragging && currentEpisode != null) setTimeLabelsAndProgressBar(podcast)

            toggleLoadingWheel(false)
            addPodcastOnClickListeners(podcast)
        }
    }

    private fun setupBoostAnimation(
        amount: Sat?
    ) {

        binding.apply {
            includeLayoutPlayerDescriptionAndControls.includeLayoutEpisodePlaybackControls.includeLayoutCustomBoost.apply {
                editTextCustomBoost.setText(
                    (amount ?: Sat(100)).asFormattedString()
                )
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
        val seekTime = (duration * (progress.toDouble() / 100.toDouble())).toInt()
        viewModel.seekTo(seekTime)
    }

    private fun updateViewAfterSeek(podcast: Podcast) {
        lifecycleScope.launch(viewModel.mainImmediate) {
            setTimeLabelsAndProgressBar(podcast)
        }
    }

    private suspend fun setTimeLabelsAndProgressBar(podcast: Podcast) {
        podcast.setInitialEpisodeDuration(args.argEpisodeDuration)

        val currentTime = podcast.currentTime.toLong()

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
                            viewState.amount
                        )
                    }
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
                        }

                        is PlayerViewState.YouTubeVideoSelected -> {
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

                            didSeekToStartTime = false

                            if (youtubePlayer != null) {
                                youtubePlayer?.cueVideo(viewState.episode.enclosureUrl.value.youTubeVideoId())

                                viewModel.createHistoryItem()
                                viewModel.trackVideoConsumed()
                                viewModel.createVideoRecordConsumed(viewState.episode.id)
                            } else {
                                setupYoutubePlayer(
                                    viewState.episode.enclosureUrl.value.youTubeVideoId(),
                                )
                                viewModel.createVideoRecordConsumed(viewState.episode.id)
                            }
                        }
                    }
                }
            }
        }
    }

    private var didSeekToStartTime = false
    private fun seekToStartTime() {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            (viewModel.playerViewStateContainer.value as? PlayerViewState.YouTubeVideoSelected)?.let { viewState ->
                viewState.episode.clipStartTime?.let {
                    if (!didSeekToStartTime) {
                        youtubePlayer?.seekToMillis(it)
                    }
                    didSeekToStartTime = true
                }
            }
        }
    }

    private fun setupYoutubePlayer(
        videoId: String
    ) {
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
                        viewModel.setNewHistoryItem(p0.toLong())
                    }

                    override fun onBuffering(p0: Boolean) {}

                    override fun onPlaying() {
                        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                            viewModel.playingVideoUpdate()
                            binding.includeRecommendedItemsList.recyclerViewList.adapter?.notifyDataSetChanged()
                        }

                        seekToStartTime()
                        viewModel.startTimer()
                    }
                    override fun onStopped() {
                        viewModel.stopTimer()
                    }

                    override fun onPaused() {
                        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                            viewModel.playingVideoDidPause()
                            binding.includeRecommendedItemsList.recyclerViewList.adapter?.notifyDataSetChanged()
                        }
                        viewModel.stopTimer()
                    }
                }
            })
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