package chat.sphinx.video_screen.ui.watch

import android.animation.Animator
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.resources.inputMethodManager
import chat.sphinx.video_screen.R
import chat.sphinx.video_screen.adapter.VideoFeedItemsAdapter
import chat.sphinx.video_screen.adapter.VideoFeedItemsFooterAdapter
import chat.sphinx.video_screen.databinding.FragmentVideoWatchScreenBinding
import chat.sphinx.video_screen.ui.VideoFeedScreenSideEffect
import chat.sphinx.video_screen.ui.viewstate.BoostAnimationViewState
import chat.sphinx.video_screen.ui.viewstate.LoadingVideoViewState
import chat.sphinx.video_screen.ui.viewstate.SelectedVideoViewState
import chat.sphinx.video_screen.ui.viewstate.VideoFeedScreenViewState
import chat.sphinx.video_screen.BuildConfig
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.isTrue
import chat.sphinx.wrapper_common.feed.isYoutubeVideo
import chat.sphinx.wrapper_common.feed.youtubeVideoId
import chat.sphinx.wrapper_common.hhmmElseDate
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.toSat
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubeVideoPlayerSupportFragmentXKt
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
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
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .build()
    }

    override val binding: FragmentVideoWatchScreenBinding by viewBinding(
        FragmentVideoWatchScreenBinding::bind
    )
    override val viewModel: VideoFeedWatchScreenViewModel by viewModels()
    private var youtubePlayer: YouTubePlayer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val a: Activity? = activity
        a?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        setupBoost()
        setupItems()
        setupVideoPlayer()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.createHistoryItem()
        viewModel.trackVideoConsumed()
        val a: Activity? = activity
        a?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    }

    private fun setupBoost() {
        binding.apply {
            includeLayoutBoostFireworks.apply {
                lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener {
                    override fun onAnimationEnd(animation: Animator?) {
                        root.gone
                    }
                    override fun onAnimationRepeat(animation: Animator?) {}

                    override fun onAnimationCancel(animation: Animator?) {}

                    override fun onAnimationStart(animation: Animator?) {}
                })
            }

            includeLayoutVideoPlayer.includeLayoutCustomBoost.apply {
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

    private fun setupVideoPlayer() {
        binding.includeLayoutVideoPlayer.apply {

            viewModel.setVideoView(videoViewVideoPlayer)

            val controller = MediaController(binding.root.context)
            controller.setAnchorView(videoViewVideoPlayer)
            controller.setMediaPlayer(videoViewVideoPlayer)
            videoViewVideoPlayer.setMediaController(controller)

            textViewSubscribeButton.setOnClickListener {
                viewModel.toggleSubscribeState()
            }
        }
    }

    private fun setupYoutubePlayer(videoId: String) {

        val youtubePlayerFragment = YouTubeVideoPlayerSupportFragmentXKt()

        childFragmentManager.beginTransaction()
            .replace(binding.includeLayoutVideoPlayer.frameLayoutYoutubePlayer.id, youtubePlayerFragment as Fragment)
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
                        Log.d("YouTubePlayer", "Youtube has seek $p0")
                    }
                    override fun onBuffering(p0: Boolean) {}

                    override fun onPlaying() {
                        viewModel.startTimer()
                        Log.d("YouTubePlayer", "Youtube is playing")
                    }
                    override fun onStopped() {
                        viewModel.stopTimer()
                        Log.d("YouTubePlayer", "Youtube has stopped")
                    }
                    override fun onPaused() {
                        viewModel.stopTimer()
                        Log.d("YouTubePlayer", "Youtube is on pause")
                    }
                }
            })
    }

    private suspend fun setupBoostAnimation(
        photoUrl: PhotoUrl?,
        amount: Sat?
    ) {
        binding.apply {
            includeLayoutVideoPlayer.includeLayoutCustomBoost.apply {
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
                            .placeholderResId(R.drawable.ic_profile_avatar_circle)
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
                        textViewContributorName.text = viewState.title.value

                        viewState.imageToShow?.let {
                            imageLoader.load(
                                imageViewContributorImage,
                                it.value,
                                imageLoaderOptions
                            )
                        }
                    }

                    includeLayoutVideoPlayer.apply {
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

                            textViewVideoTitle.text = viewState.title.value
                            textViewVideoDescription.text = viewState.description?.value ?: ""
                            textViewVideoPublishedDate.text = viewState.date?.hhmmElseDate()

                            if (viewState.url.isYoutubeVideo()) {

                                layoutConstraintVideoViewContainer.gone
                                frameLayoutYoutubePlayer.visible

                                if (youtubePlayer != null) {
                                    viewModel.createHistoryItem()
                                    viewModel.trackVideoConsumed()
                                    youtubePlayer?.cueVideo(viewState.id.youtubeVideoId())
                                    viewModel.createVideoRecordConsumed(viewState.id)
                                } else {
                                    setupYoutubePlayer(viewState.id.youtubeVideoId())
                                    viewModel.createVideoRecordConsumed(viewState.id)
                                }

                            } else {
                                layoutConstraintLoadingVideo.visible
                                layoutConstraintVideoViewContainer.visible
                                frameLayoutYoutubePlayer.gone

                                val videoUri = if (viewState.localFile != null) {
                                    viewState.localFile.toUri()
                                } else {
                                    viewState.url.value.toUri()
                                }

                                viewModel.initializeVideo(
                                    videoUri,
                                    viewState.duration?.value?.toInt()
                                )
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
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val currentOrientation = resources.configuration.orientation

        binding.includeLayoutVideoPlayer.layoutConstraintVideoPlayers.apply {
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                layoutParams.height =
                    binding.root.measuredWidth - (requireActivity() as InsetterActivity).statusBarInsetHeight.top
            } else {
                layoutParams.height = resources.getDimension(R.dimen.video_player_height).toInt()
            }
            requestLayout()
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: VideoFeedScreenSideEffect) {
        sideEffect.execute(requireActivity())
    }
}
