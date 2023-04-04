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
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
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
                binding.apply {
                    textViewTitleHeader.text = viewState.feedItem.titleToShow
                    textViewDescriptionEpisode.text = viewState.feedItem.descriptionToShow
                    textViewDescriptionEpisodeTitle.text = viewState.podcastTitle
                    imageViewItemRowEpisodeType.setImageDrawable((ContextCompat.getDrawable(root.context, getFeedItemDrawableType(viewState.feedType))))
                    viewState.feedItem.imageUrlToShow?.value?.let {image ->
                        imageLoader.load(
                            imageViewEpisodeDetailImage,
                            image,
                            ImageLoaderOptions.Builder()
                                .placeholderResId(R.drawable.ic_podcast_placeholder)
                                .build()
                        )
                    }
                }
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
