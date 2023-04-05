package chat.sphinx.episode_description.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_connectivity_helper.ConnectivityHelper
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.create_description.R
import chat.sphinx.create_description.databinding.FragmentEpisodeDescriptionBinding
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_feed.isNewsletter
import chat.sphinx.wrapper_feed.isPodcast
import chat.sphinx.wrapper_feed.isVideo
import chat.sphinx.wrapper_podcast.toHrAndMin
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.goneIfTrue
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class EpisodeDescriptionFragment: SideEffectFragment<
        Context,
        EpisodeDescriptionSideEffect,
        EpisodeDescriptionViewState,
        EpisodeDescriptionViewModel,
        FragmentEpisodeDescriptionBinding
        >(R.layout.fragment_episode_description)
{
    override val viewModel: EpisodeDescriptionViewModel by viewModels()
    override val binding: FragmentEpisodeDescriptionBinding by viewBinding(FragmentEpisodeDescriptionBinding::bind)

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var connectivityHelper: ConnectivityHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BackPressHandler(viewLifecycleOwner, requireActivity())
        setAllClickListeners()
    }

    private inner class BackPressHandler(
        owner: LifecycleOwner,
        activity: FragmentActivity,
    ) : OnBackPressedCallback(true) {
        init {
            activity.apply {
                onBackPressedDispatcher.addCallback(
                    owner,
                    this@BackPressHandler,
                )
            }
        }
        override fun handleOnBackPressed() {
            lifecycleScope.launch(viewModel.mainImmediate) {
                viewModel.navigator.popBackStack()
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: EpisodeDescriptionViewState) {
        when(viewState) {
            is EpisodeDescriptionViewState.Idle -> {}
            is EpisodeDescriptionViewState.FeedItemDescription -> {
                bindFeedItemDescription(viewState)
            }
        }
    }


    private suspend fun bindFeedItemDescription(viewState: EpisodeDescriptionViewState.FeedItemDescription) {
        binding.apply {
            textViewTitleHeader.text = viewState.feedItem.titleToShow
            textViewDescriptionEpisode.text = viewState.feedItem.descriptionToShow
            textViewDescriptionEpisodeTitle.text = viewState.feed?.title?.value
            imageViewItemRowEpisodeType.setImageDrawable((ContextCompat.getDrawable(root.context, getFeedItemDrawableType(viewState.feed?.feedType))))
            viewState.feedItem.imageUrlToShow?.value?.let {image ->
                imageLoader.load(
                    imageViewEpisodeDetailImage,
                    image,
                    ImageLoaderOptions.Builder()
                        .placeholderResId(R.drawable.ic_podcast_placeholder)
                        .build()
                )
            }
            val duration = viewState.podcastEpisode?.getUpdatedContentEpisodeStatus()?.duration?.value?.toInt()?.toHrAndMin()
            textViewEpisodeDate.text = viewState.podcastEpisode?.dateString
            textViewItemEpisodeTime.text = duration
            binding.textViewDescriptionEpisode.post {
                val numberOfLines = textViewDescriptionEpisode.lineCount
                binding.constraintShowMoreContainer.goneIfTrue(numberOfLines < 5)
            }

            if (viewState.podcastEpisode?.downloaded == true) {
                imageDownloadedEpisodeArrow.visible
                buttonDownloadArrow.gone
                progressBarEpisodeDownload.gone
                buttonStop.gone
            }

            if (viewState.podcastEpisode?.downloaded == false) {
                buttonDownloadArrow.visible
                buttonStop.gone
                progressBarEpisodeDownload.gone
                imageDownloadedEpisodeArrow.gone
            }

            val isFeedItemDownloadInProgress = viewState.isFeedItemDownloadInProgress && viewState.podcastEpisode?.downloaded == false

            if (isFeedItemDownloadInProgress) {
                buttonDownloadArrow.gone
                progressBarEpisodeDownload.visible
                imageDownloadedEpisodeArrow.gone
                buttonStop.visible
            }

            if (viewState.isEpisodeSoundPlaying) {
                buttonPlayEpisode.setImageDrawable(
                    ContextCompat.getDrawable(binding.root.context, R.drawable.ic_pause_episode)
                )
            } else {
                buttonPlayEpisode.setImageDrawable(
                    ContextCompat.getDrawable(binding.root.context, R.drawable.ic_play_episode)
                )
            }
        }
    }

    private fun setAllClickListeners() {
        binding.buttonNavBack.setOnClickListener {
            lifecycleScope.launch(viewModel.mainImmediate) {
                viewModel.navigator.popBackStack()
            }
        }
        binding.constraintShowMoreContainer.setOnClickListener {
            toggleShowMore()
        }
        binding.buttonEpisodeShare.setOnClickListener {
            viewModel.share(binding.root.context, getString(R.string.episode_detail_clipboard))
        }
        binding.buttonDownloadArrow.setOnClickListener {
            viewModel.downloadMedia()
        }
        binding.buttonPlayEpisode.setOnClickListener {
            if (connectivityHelper.isNetworkConnected()) {
                viewModel.playEpisodeFromDescription()
            }
        }
    }

    private fun toggleShowMore() {
        binding.apply {
            if (textViewDescriptionEpisode.maxLines > 5) {
                textViewDescriptionEpisode.maxLines = 5
                textViewShowMore.text = getString(R.string.episode_description_show_more)
            }
            else {
                textViewDescriptionEpisode.maxLines = Int.MAX_VALUE
                textViewShowMore.text = getString(R.string.episode_description_show_less)
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: EpisodeDescriptionSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    private fun getFeedItemDrawableType(feedType: FeedType?): Int {
        return when (feedType) {
            is FeedType.Podcast -> R.drawable.ic_podcast_type
            is FeedType.Video -> R.drawable.ic_youtube_type
            else -> {
                R.drawable.ic_podcast_placeholder
            }
        }
    }
}
