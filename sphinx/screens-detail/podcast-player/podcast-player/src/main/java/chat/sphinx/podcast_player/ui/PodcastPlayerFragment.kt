package chat.sphinx.podcast_player.ui

import android.content.Context
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.podcast_player.R
import chat.sphinx.podcast_player.databinding.FragmentPodcastPlayerBinding
import chat.sphinx.podcast_player.objects.Podcast
import chat.sphinx.podcast_player.objects.PodcastEpisode
import chat.sphinx.podcast_player.ui.adapter.PodcastEpisodesFooterAdapter
import chat.sphinx.podcast_player.ui.adapter.PodcastEpisodesListAdapter
import chat.sphinx.wrapper_common.util.getTimeString
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    override suspend fun onViewStateFlowCollect(viewState: PodcastPlayerViewState) {
        @Exhaustive
        when (viewState) {
            is PodcastPlayerViewState.Idle -> {}

            is PodcastPlayerViewState.ServiceLoading -> {
                toggleLoadingWheel(true)
            }
            is PodcastPlayerViewState.ServiceInactive -> {
                togglePlayPauseButton(false)
            }

            is PodcastPlayerViewState.PodcastLoaded -> {
                showPodcastInfo(viewState.podcast)
                addPodcastOnClickListeners(viewState.podcast)
            }

            is PodcastPlayerViewState.LoadingEpisode -> {
                loadingEpisode(viewState.episode)
            }

            is PodcastPlayerViewState.EpisodePlayed -> {
                showPodcastInfo(viewState.podcast)
            }

            is PodcastPlayerViewState.MediaStateUpdate -> {
                toggleLoadingWheel(false)
                showPodcastInfo(viewState.podcast)
            }

        }
    }

    private suspend fun showPodcastInfo(podcast: Podcast) {
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

            togglePlayPauseButton(podcast.isPlaying)
        }
    }

    private fun toggleLoadingWheel(show: Boolean) {
        binding.includeLayoutEpisodeSliderControl.apply {
            progressBarAudioLoading.goneIfFalse(show)
        }
    }

    private fun togglePlayPauseButton(playing: Boolean) {
        binding.apply {
            includeLayoutEpisodePlaybackControlButtons.apply {
                buttonPlayPause.background =
                    ContextCompat.getDrawable(root.context,
                        if (playing) R.drawable.ic_podcast_pause_circle else R.drawable.ic_podcast_play_circle
                    )
            }
        }
    }

    private fun addPodcastOnClickListeners(podcast: Podcast) {
        binding.apply {
            includeLayoutEpisodeSliderControl.apply {
                seekBarCurrentEpisodeProgress.setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {

                        override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            if (fromUser) {
                                val duration = podcast.getCurrentEpisodeDuration()
                                setTimeLabelsAndProgressBarTo(duration, null, progress)
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                                seekTo(podcast, seekBar?.progress ?: 0)
                            }
                        }
                    }
                )
            }

            includeLayoutEpisodePlaybackControlButtons.apply {
                buttonPlaybackSpeed.setOnClickListener {
                    showSpeedPopup()
                }

                buttonShareClip.setOnClickListener {
                    //TODO share clip feature
                }

                buttonReplay15.setOnClickListener {
                    viewModel.seekTo(podcast.currentTime - 15000)
                    updateViewAfterSeek(podcast)
                }

                buttonPlayPause.setOnClickListener {
                    val currentEpisode = podcast.getCurrentEpisode()

                    if (currentEpisode.playing) {
                        viewModel.pauseEpisode(currentEpisode)
                    } else {
                        viewModel.playEpisode(currentEpisode, podcast.currentTime)
                    }
                }

                buttonForward30.setOnClickListener {
                    viewModel.seekTo(podcast.currentTime + 30000)
                    updateViewAfterSeek(podcast)
                }

                buttonBoost.setOnClickListener {
                    //TODO Boost podcast
                }
            }
        }
    }

    private fun loadingEpisode(episode: PodcastEpisode) {
        binding.apply {
            textViewEpisodeTitleLabel.text = episode.title

            includeLayoutEpisodeSliderControl.apply {
                textViewCurrentEpisodeDuration.text = 0.toLong().getTimeString()
                textViewCurrentEpisodeProgress.text = 0.toLong().getTimeString()

                seekBarCurrentEpisodeProgress.progress = 0

                toggleLoadingWheel(true)
            }
        }
    }

    private suspend fun seekTo(podcast: Podcast, progress: Int) {
        val duration = withContext(viewModel.io) {
            podcast.getCurrentEpisodeDuration()
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
        var currentTime = podcast.currentTime.toLong()

        val duration = withContext(viewModel.io) {
            podcast.getCurrentEpisodeDuration()
        }
        val progress: Int =
            try {
                ((currentTime * 100) / duration).toInt()
            } catch (e: ArithmeticException) {
                0
            }

        setTimeLabelsAndProgressBarTo(duration, currentTime, progress)
    }

    private fun setTimeLabelsAndProgressBarTo(duration: Long, currentTime: Long? = null, progress: Int) {
        binding.includeLayoutEpisodeSliderControl.apply {
            val currentT: Double = currentTime?.toDouble() ?: (duration.toDouble() * (progress.toDouble()) / 100)

            textViewCurrentEpisodeDuration.text = duration.getTimeString()
            textViewCurrentEpisodeProgress.text = currentT.toLong().getTimeString()

            seekBarCurrentEpisodeProgress.progress = progress
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
                    R.id.speed0_8 -> {
                        buttonPlaybackSpeed.text = "0.8x"
                        viewModel.adjustSpeed(0.8)
                    }
                    R.id.speed1 -> {
                        buttonPlaybackSpeed.text = "1x"
                        viewModel.adjustSpeed(1.0)
                    }
                    R.id.speed1_2 -> {
                        buttonPlaybackSpeed.text = "1.2x"
                        viewModel.adjustSpeed(1.2)
                    }
                    R.id.speed1_5 -> {
                        buttonPlaybackSpeed.text = "1.5x"
                        viewModel.adjustSpeed(1.5)
                    }
                    R.id.speed2_1 -> {
                        buttonPlaybackSpeed.text = "2.1x"
                        viewModel.adjustSpeed(2.1)
                    }
                }
                true
            }

            popup.show()
        }
    }
}
