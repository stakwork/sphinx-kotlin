package chat.sphinx.video_screen.ui.watch

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.video_screen.R
import chat.sphinx.video_screen.adapter.VideoFeedItemsAdapter
import chat.sphinx.video_screen.adapter.VideoFeedItemsFooterAdapter
import chat.sphinx.video_screen.databinding.FragmentVideoWatchScreenBinding
import chat.sphinx.video_screen.ui.viewstate.PlayingVideoViewState
import chat.sphinx.video_screen.ui.viewstate.VideoFeedScreenViewState
import chat.sphinx.wrapper_common.feed.isYoutubeVideo
import chat.sphinx.wrapper_common.feed.youtubeVideoId
import chat.sphinx.wrapper_common.hhmmElseDate
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class VideoFeedWatchScreenFragment: BaseFragment<
        VideoFeedScreenViewState,
        VideoFeedWatchScreenViewModel,
        FragmentVideoWatchScreenBinding
        >(R.layout.fragment_video_watch_screen)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .build()
    }

    override val binding: FragmentVideoWatchScreenBinding by viewBinding(FragmentVideoWatchScreenBinding::bind)
    override val viewModel: VideoFeedWatchScreenViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupItems()
    }

    private fun setupItems() {
        binding.includeLayoutVideoItemsList?.let {
            it.recyclerViewVideoList.apply {
                val linearLayoutManager = LinearLayoutManager(context)
                val videoFeedItemsAdapter = VideoFeedItemsAdapter(
                    imageLoader,
                    viewLifecycleOwner,
                    onStopSupervisor,
                    viewModel,
                    viewModel
                )
                val videoListFooterAdapter = VideoFeedItemsFooterAdapter(requireActivity() as InsetterActivity)
                this.setHasFixedSize(false)
                layoutManager = linearLayoutManager
                adapter = ConcatAdapter(videoFeedItemsAdapter, videoListFooterAdapter)
                itemAnimator = null
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: VideoFeedScreenViewState) {
        @Exhaustive
        when (viewState) {
            is VideoFeedScreenViewState.Idle -> {}

            is VideoFeedScreenViewState.FeedLoaded -> {
                binding.apply {
                    includeLayoutVideoItemsList?.textViewVideosListCount?.text = viewState.items.count().toString()

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
                }
            }
        }
    }

    companion object {
        const val YOUTUBE_WEB_VIEW_MIME_TYPE = "text/html"
        const val YOUTUBE_WEB_VIEW_ENCODING = "utf-8"
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.playingVideoStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is PlayingVideoViewState.Idle -> {}

                    is PlayingVideoViewState.PlayingVideo -> {
                        binding.includeLayoutVideoPlayer.apply {

                            textViewVideoTitle?.text = viewState.title.value
                            textViewVideoDescription?.text = viewState.description?.value ?: ""
                            textViewVideoPublishedDate?.text = viewState.date?.hhmmElseDate()

                            if (viewState.url.isYoutubeVideo()) {

                                videoViewVideoPlayer.gone
                                webViewYoutubeVideoPlayer.visible

                                webViewYoutubeVideoPlayer.settings.apply {
                                    javaScriptEnabled = true
                                }

                                webViewYoutubeVideoPlayer.loadData(
                                    "<iframe width=\"100%\" height=\"100%\" src=\"https://www.youtube-nocookie.com/embed/${viewState.id.youtubeVideoId()}\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture\" allowfullscreen></iframe>",
                                    YOUTUBE_WEB_VIEW_MIME_TYPE,
                                    YOUTUBE_WEB_VIEW_ENCODING
                                )

                            } else {

                                videoViewVideoPlayer.visible
                                webViewYoutubeVideoPlayer.gone

                                videoViewVideoPlayer.setOnPreparedListener {
                                    it.start()
                                }

                                videoViewVideoPlayer.setVideoURI(
                                    viewState.url.value.toUri()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
