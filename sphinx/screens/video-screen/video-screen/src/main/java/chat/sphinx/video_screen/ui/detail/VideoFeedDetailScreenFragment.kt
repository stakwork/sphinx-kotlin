package chat.sphinx.video_screen.ui.detail

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.video_screen.R
import chat.sphinx.video_screen.adapter.VideoFeedItemsAdapter
import chat.sphinx.video_screen.databinding.FragmentVideoDetailScreenBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class VideoFeedDetailScreenFragment: BaseFragment<
        VideoFeedDetailScreenViewState,
        VideoFeedDetailScreenViewModel,
        FragmentVideoDetailScreenBinding
        >(R.layout.fragment_video_detail_screen)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_tribe)
            .build()
    }

    override val binding: FragmentVideoDetailScreenBinding by viewBinding(FragmentVideoDetailScreenBinding::bind)
    override val viewModel: VideoFeedDetailScreenViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInsetterPadding()
        setupVideoFeedDetail()
        setupVideoEpisodeListAdapter()
    }

    private fun setupInsetterPadding() {
        val activity = (requireActivity() as InsetterActivity)
        binding.apply {

            activity.addStatusBarPadding(constraintLayoutScrollViewContent)
        }
    }

    private fun setupVideoFeedDetail() {
        binding.includeLayoutVideoFeedDetails.apply {

            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                val videoFeed = viewModel.videoFeedSharedFlow.firstOrNull()

                videoFeed?.let { videoFeed ->
                    textViewCurrentVideoDescription.text = videoFeed.descriptionToShow
                    textViewContributorName.text = videoFeed.author?.value
                    videoFeed.imageUrlToShow?.let {
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

    private fun setupVideoEpisodeListAdapter() {
        binding.includeLayoutVideoEpisodesList.recyclerViewEpisodesList.apply {
            val videoFeedItemsAdapter = VideoFeedItemsAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel,
                viewModel
            )
//            this.setHasFixedSize(false)
            adapter = videoFeedItemsAdapter
            itemAnimator = null
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.feedItemsHolderViewStateFlow.collect { feedItems ->
                binding.includeLayoutVideoEpisodesList.textViewEpisodesListCount.text = feedItems.size.toString()
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: VideoFeedDetailScreenViewState) {
        //TODO implement view state collector
    }
}
