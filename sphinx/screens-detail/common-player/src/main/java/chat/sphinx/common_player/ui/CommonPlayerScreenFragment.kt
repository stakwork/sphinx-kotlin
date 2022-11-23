package chat.sphinx.common_player.ui

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.feed.youtubeVideoId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubeCommonPlayerSupportFragmentXKt
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.invisible
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class CommonPlayerScreenFragment() : SideEffectFragment<
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
                            includeLayoutPlayersContainer.apply {
                                frameLayoutYoutubePlayer.gone
                                imageViewPodcastImage.visible
                                includeLayoutEpisodeSliderControl.root.visible
                            }
                            includeLayoutPlayerDescriptionAndControls.includeLayoutEpisodePlaybackControls.apply {
                                textViewShareClipButton.visible
                                textViewForward30Button.visible
                                textViewReplay15Button.visible
                                textViewPlayPauseButton.visible
                                textViewPlaybackSpeedButton.visible
                                imageViewPlayPauseButton.visible
                            }

                            youtubePlayer?.pause()
                        }
                        is CommonPlayerScreenViewState.FeedRecommendations.YouTubeVideoSelected -> {
                            includeLayoutPlayersContainer.apply {
                                frameLayoutYoutubePlayer.visible
                                imageViewPodcastImage.gone
                                includeLayoutEpisodeSliderControl.root.gone
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
                        Log.d("YouTubePlayer", "Youtube is playing")
                    }
                    override fun onStopped() {
                        Log.d("YouTubePlayer", "Youtube has stopped")
                    }
                    override fun onPaused() {
                        Log.d("YouTubePlayer", "Youtube is on pause")
                    }
                }
            })
    }

    override suspend fun onSideEffectCollect(sideEffect: CommonPlayerScreenSideEffect) {
    }

}

@Suppress("NOTHING_TO_INLINE")
inline fun String.youTubeVideoId(): String {
    return this.substringAfterLast("v/").substringAfterLast("v=").substringBefore("?")
}