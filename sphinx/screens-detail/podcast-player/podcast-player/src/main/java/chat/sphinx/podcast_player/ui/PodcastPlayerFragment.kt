package chat.sphinx.podcast_player.ui

import android.content.Context
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.podcast_player.R
import chat.sphinx.podcast_player.databinding.FragmentPodcastPlayerBinding
import chat.sphinx.podcast_player.objects.Podcast
import chat.sphinx.podcast_player.ui.adapter.PodcastEpisodesFooterAdapter
import chat.sphinx.podcast_player.ui.adapter.PodcastEpisodesListAdapter
import chat.sphinx.wrapper_common.util.getTimeString
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.annotation.meta.Exhaustive
import javax.inject.Inject


@AndroidEntryPoint
internal class PodcastPlayerFragment : BaseFragment<
        PodcastPlayerViewState,
        PodcastPlayerViewModel,
        FragmentPodcastPlayerBinding
        >(R.layout.fragment_podcast_player) {

    override val viewModel: PodcastPlayerViewModel by viewModels()
    override val binding: FragmentPodcastPlayerBinding by viewBinding(FragmentPodcastPlayerBinding::bind)

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            textViewDismissButton.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }

            root.post {
                val fragmentHeight = root.measuredHeight

                includeLayoutPodcastEpisodesList.layoutConstraintPodcastEpisodesList.apply {
                    kotlin.run {
                        layoutParams.height = fragmentHeight
                        requestLayout()
                    }
                }
            }
        }

        setupEpisodes()
    }

    override suspend fun onViewStateFlowCollect(viewState: PodcastPlayerViewState) {
        @Exhaustive
        when (viewState) {
            is PodcastPlayerViewState.Idle -> {}

            is PodcastPlayerViewState.PodcastLoaded -> {
                viewState.podcast?.let { podcast ->
                    showPodcastInfo(podcast)
                }
            }

            is PodcastPlayerViewState.EpisodePlayed -> {
                viewState.podcast?.let { podcast ->
                    showPodcastInfo(podcast)
                }
            }
        }
    }

    private fun showPodcastInfo(podcast: Podcast) {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            binding.apply {
                textViewEpisodeTitleLabel.text = podcast.getCurrentEpisode().title

                setTimeLabelsAndProgressBar(podcast)

                imageLoader.load(
                    imageViewPodcastImage,
                    podcast.image,
                    ImageLoaderOptions.Builder()
                        .placeholderResId(R.drawable.ic_profile_avatar_circle)
                        .build()
                )

                includeLayoutPodcastEpisodesList.textViewEpisodesListCount.text = podcast.episodesCount.toString()

                includeLayoutEpisodePlaybackControlButtons.apply {
                    buttonPlaybackSpeed.setOnClickListener {
                        showSpeedPopup()
                    }

                    buttonShareClip.setOnClickListener {
                        //TODO share clip feature
                    }

                    buttonReplay15.setOnClickListener {
                        viewModel.seekTo(podcast, podcast.getCurrentEpisode(), podcast.currentTime - 15)
                    }

                    buttonPlayPause.setOnClickListener {
                        val currentEpisode = podcast.getCurrentEpisode()

                        if (currentEpisode.playing) {
                            viewModel.pauseEpisode(podcast, currentEpisode)
                        } else {
                            viewModel.playEpisode(podcast, currentEpisode, podcast.currentTime)
                        }
                    }

                    buttonForward30.setOnClickListener {
                        viewModel.seekTo(podcast, podcast.getCurrentEpisode(), podcast.currentTime + 30)
                    }

                    buttonBoost.setOnClickListener {
                        //TODO Boost podcast
                    }
                }
            }
        }
    }

    private suspend fun setTimeLabelsAndProgressBar(podcast: Podcast) {
        //Reset labels until new duration loads
        binding.includeLayoutEpisodeSliderControl.apply {
            textViewCurrentEpisodeDuration.text = 0.toLong().getTimeString()
            textViewCurrentEpisodeProgress.text = 0.toLong().getTimeString()
        }

        var currentTime = podcast.currentTime.toLong()

        val duration = withContext(viewModel.io) {
            podcast.getEpisodeDuration()
        }

        val progress = ((currentTime * 100) / duration).toInt()

        binding.includeLayoutEpisodeSliderControl.apply {
            textViewCurrentEpisodeDuration.text = duration.getTimeString()
            textViewCurrentEpisodeProgress.text = currentTime.getTimeString()

            seekBarCurrentEpisodeProgress.progress = progress
        }
    }

    private fun setupEpisodes() {
        binding.includeLayoutPodcastEpisodesList.recyclerViewEpisodesList.apply {
            val linearLayoutManager = LinearLayoutManager(context)
            val chatListAdapter = PodcastEpisodesListAdapter(
                this,
                linearLayoutManager,
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel
            )

            val episodesListFooterAdapter = PodcastEpisodesFooterAdapter(requireActivity() as InsetterActivity)
            this.setHasFixedSize(false)
            layoutManager = linearLayoutManager
            adapter = ConcatAdapter(chatListAdapter, episodesListFooterAdapter)
            itemAnimator = null
        }
    }

    private fun showSpeedPopup() {
        binding.includeLayoutEpisodePlaybackControlButtons.apply {
            val wrapper: Context = ContextThemeWrapper(context, R.style.speedMenu)
            val popup = PopupMenu(wrapper, buttonPlaybackSpeed)
            popup.inflate(R.menu.speed_menu)

            popup.setOnMenuItemClickListener { item: MenuItem? ->
                when (item!!.itemId) {
                    R.id.speed0_5 -> {
                        buttonPlaybackSpeed.text = "0.5x"
                        viewModel.adjustSpeed(0.5)
                    }
                    R.id.speed1 -> {
                        buttonPlaybackSpeed.text = "1x"
                        viewModel.adjustSpeed(1.0)
                    }
                    R.id.speed1_5 -> {
                        buttonPlaybackSpeed.text = "1.5x"
                        viewModel.adjustSpeed(1.1)
                    }
                    R.id.speed2 -> {
                        buttonPlaybackSpeed.text = "2x"
                        viewModel.adjustSpeed(2.0)
                    }
                }
                true
            }

            popup.show()
        }
    }
}
