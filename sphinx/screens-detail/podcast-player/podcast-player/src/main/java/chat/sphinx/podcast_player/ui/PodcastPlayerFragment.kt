package chat.sphinx.podcast_player.ui

import android.animation.Animator
import android.content.Context
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_connectivity_helper.ConnectivityHelper
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.podcast_player.R
import chat.sphinx.podcast_player.databinding.FragmentPodcastPlayerBinding
import chat.sphinx.podcast_player.ui.adapter.PodcastEpisodesFooterAdapter
import chat.sphinx.podcast_player.ui.adapter.PodcastEpisodesListAdapter
import chat.sphinx.podcast_player.ui.viewstates.BoostAnimationViewState
import chat.sphinx.podcast_player.ui.viewstates.PodcastPlayerViewState
import chat.sphinx.resources.inputMethodManager
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.isTrue
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_common.util.getHHMMSSString
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastEpisode
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@AndroidEntryPoint
internal class PodcastPlayerFragment : SideEffectFragment<
        FragmentActivity,
        PodcastPlayerSideEffect,
        PodcastPlayerViewState,
        PodcastPlayerViewModel,
        FragmentPodcastPlayerBinding
        >(R.layout.fragment_podcast_player) {

    override val viewModel: PodcastPlayerViewModel by viewModels()
    override val binding: FragmentPodcastPlayerBinding by viewBinding(FragmentPodcastPlayerBinding::bind)

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var connectivityHelper: ConnectivityHelper

    private val args: PodcastPlayerFragmentArgs by navArgs()

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.trackPodcastConsumed()
    }

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
                        layoutParams.height = (fragmentHeight.toDouble() * 0.975).toInt()
                        requestLayout()
                    }
                }
            }
        }

        setupBoost()
        setupEpisodes()
    }

    private fun setupBoost() {
        binding.apply {
            includeLayoutBoostFireworks.apply {
                lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener{
                    override fun onAnimationEnd(animation: Animator?) {
                        root.gone
                    }

                    override fun onAnimationRepeat(animation: Animator?) {}

                    override fun onAnimationCancel(animation: Animator?) {}

                    override fun onAnimationStart(animation: Animator?) {}
                })
            }

            includeLayoutEpisodePlaybackControls.includeLayoutCustomBoost.apply {
                removeFocusOnEnter(editTextCustomBoost)
            }
        }
    }

    private fun removeFocusOnEnter(editText: EditText?) {
        editText?.setOnEditorActionListener(object:
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

    private fun setupEpisodes() {
        binding.includeLayoutPodcastEpisodesList.recyclerViewEpisodesList.apply {
            val linearLayoutManager = LinearLayoutManager(context)
            val chatListAdapter = PodcastEpisodesListAdapter(
                this,
                linearLayoutManager,
                imageLoader,
                connectivityHelper,
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

    private var dragging: Boolean = false
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

            includeLayoutEpisodePlaybackControls.apply {
                textViewPlaybackSpeedButton.setOnClickListener {
                    showSpeedPopup()
                }

                textViewShareClipButton.setOnClickListener {
                    viewModel.shouldShareClip()
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

                includeLayoutCustomBoost.apply customBoost@ {
                    this@customBoost.imageViewFeedBoostButton.setOnClickListener {
                        val amount = editTextCustomBoost.text.toString()
                            .replace(" ", "")
                            .toLongOrNull()?.toSat() ?: Sat(0)

                        viewModel.sendPodcastBoost(
                            amount,
                            fireworksCallback = {
                                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                                    setupBoostAnimation(null, amount)

                                    includeLayoutBoostFireworks.apply fireworks@ {
                                        this@fireworks.root.visible
                                        this@fireworks.lottieAnimationView.playAnimation()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            textViewSubscribeButton.setOnClickListener {
                viewModel.toggleSubscribeState()
            }
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
                toggleLoadingWheel(true)
                showPodcastInfo(viewState.podcast)
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

            val notLinkedToChat = podcast.chatId.value == ChatId.NULL_CHAT_ID.toLong()
            textViewSubscribeButton.goneIfFalse(notLinkedToChat)

            textViewSubscribeButton.text = if (podcast.subscribed.isTrue()) {
                getString(R.string.unsubscribe)
            } else {
                getString(R.string.subscribe)
            }

            var currentEpisode: PodcastEpisode? = podcast.getCurrentEpisode()
            currentEpisode = if (connectivityHelper.isNetworkConnected() || currentEpisode?.downloaded == true) {
                currentEpisode
            } else {
                podcast.getLastDownloadedEpisode()
            }

            textViewEpisodeTitleLabel.text = currentEpisode?.title?.value ?: ""

            podcast.imageToShow?.value?.let { podcastImage ->
                imageLoader.load(
                    imageViewPodcastImage,
                    podcastImage,
                    ImageLoaderOptions.Builder()
                        .placeholderResId(R.drawable.ic_podcast_placeholder)
                        .build()
                )
            }

            includeLayoutPodcastEpisodesList.textViewEpisodesListCount.text = podcast.episodesCount.toString()

            includeLayoutEpisodePlaybackControls.apply {

                textViewShareClipButton.apply {
                    val shareClipEnabled = podcast.hasDestinations && !args.fromFeed
                    alpha = if (shareClipEnabled) 1.0F else 0.3F
                    isEnabled = shareClipEnabled
                }

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

    private suspend fun setupBoostAnimation(
        photoUrl: PhotoUrl?,
        amount: Sat?
    ) {

        binding.apply {
            includeLayoutEpisodePlaybackControls.includeLayoutCustomBoost.apply {
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

    private fun toggleLoadingWheel(show: Boolean) {
        binding.apply {
            includeLayoutEpisodeSliderControl.apply layoutEpisodesSlider@ {
                this@layoutEpisodesSlider.progressBarAudioLoading.goneIfFalse(show)
            }
            includeLayoutEpisodePlaybackControls.apply layoutPlaybackControls@ {
                this@layoutPlaybackControls.textViewPlayPauseButton.isEnabled = !show
            }
        }
    }

    private fun togglePlayPauseButton(playing: Boolean) {
        binding.apply {
            includeLayoutEpisodePlaybackControls.apply {
                textViewPlayPauseButton.background =
                    ContextCompat.getDrawable(root.context,
                        if (playing) R.drawable.ic_podcast_pause_circle else R.drawable.ic_podcast_play_circle
                    )
            }
        }
    }

    private suspend fun loadingEpisode(episode: PodcastEpisode) {
        binding.apply {
            textViewEpisodeTitleLabel.text = episode.title.value

            episode.image?.value?.let { podcastImage ->
                imageLoader.load(
                    imageViewPodcastImage,
                    podcastImage,
                    ImageLoaderOptions.Builder()
                        .placeholderResId(R.drawable.ic_podcast_placeholder)
                        .build()
                )
            }

            includeLayoutEpisodeSliderControl.apply {
                textViewCurrentEpisodeDuration.text = 0.toLong().getHHMMSSString()
                textViewCurrentEpisodeProgress.text = 0.toLong().getHHMMSSString()

                seekBarCurrentEpisodeProgress.progress = 0

                toggleLoadingWheel(true)
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
    }

    private fun setTimeLabelsAndProgressBarTo(duration: Long, currentTime: Long? = null, progress: Int) {
        binding.includeLayoutEpisodeSliderControl.apply {
            val currentT: Double = currentTime?.toDouble() ?: (duration.toDouble() * (progress.toDouble()) / 100)

            textViewCurrentEpisodeDuration.text = duration.getHHMMSSString()
            textViewCurrentEpisodeProgress.text = currentT.toLong().getHHMMSSString()

            seekBarCurrentEpisodeProgress.progress = progress
        }
    }

    private fun showSpeedPopup() {
        binding.includeLayoutEpisodePlaybackControls.apply {
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
                            viewState.photoUrl,
                            viewState.amount
                        )
                    }
                }
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: PodcastPlayerSideEffect) {
        sideEffect.execute(requireActivity())
    }
}
