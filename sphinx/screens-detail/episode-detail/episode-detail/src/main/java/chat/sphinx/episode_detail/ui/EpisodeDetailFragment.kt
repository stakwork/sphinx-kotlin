package chat.sphinx.episode_detail.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.episode_detail.R
import chat.sphinx.episode_detail.databinding.FragmentEpisodeDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
internal class EpisodeDetailFragment: SideEffectFragment<
        Context,
        EpisodeDetailSideEffect,
        EpisodeDetailViewState,
        EpisodeDetailViewModel,
        FragmentEpisodeDetailBinding
        >(R.layout.fragment_episode_detail)
{
    override val viewModel: EpisodeDetailViewModel by viewModels()
    override val binding: FragmentEpisodeDetailBinding by viewBinding(FragmentEpisodeDetailBinding::bind)

    companion object {
        const val YOUTUBE_TYPE = "Youtube"
        const val PODCAST_TYPE = "Podcast"
    }

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutConstraintCloseContainer.setOnClickListener {
            viewModel.popBackStack()
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: EpisodeDetailSideEffect) {
    }

    override suspend fun onViewStateFlowCollect(viewState: EpisodeDetailViewState) {
        when(viewState) {
            is EpisodeDetailViewState.Idle -> {}
            is EpisodeDetailViewState.ShowEpisode -> {
                commonInfoBinding(viewState)
                binding.apply {
                    when(viewState.episodeDetail.episodeTypeText) {
                        PODCAST_TYPE -> {
                            if (viewState.episodeDetail.feedId == null) {
                                layoutConstraintDownloadRow.gone
                                layoutConstraintCheckMarkRow.gone
                            }
                            else {

                            }
                        }
                        YOUTUBE_TYPE -> {
                            layoutConstraintDownloadRow.gone
                            layoutConstraintCheckMarkRow.gone
                        }
                    }

                }
            }
        }
    }


    private fun commonInfoBinding(viewState: EpisodeDetailViewState.ShowEpisode) {
        binding.apply {
            textViewMainEpisodeTitle.text = viewState.episodeDetail.header
            imageViewItemRowEpisodeType.setImageDrawable(ContextCompat.getDrawable(root.context, viewState.episodeDetail.episodeTypeImage))
            textViewEpisodeType.text = viewState.episodeDetail.episodeTypeText
            textViewEpisodeDate.text = viewState.episodeDetail.episodeDate
            textViewEpisodeDuration.text = viewState.episodeDetail.episodeDuration

            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                imageLoader.load(
                    imageViewEpisodeDetailImage,
                    viewState.episodeDetail.image,
                    ImageLoaderOptions.Builder()
                        .placeholderResId(R.drawable.ic_podcast_placeholder)
                        .transformation(Transformation.CircleCrop)
                        .build()
                )
            }
        }
    }



}
