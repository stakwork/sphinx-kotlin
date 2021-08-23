package chat.sphinx.chat_tribe.ui

import android.animation.Animator
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_common.databinding.*
import chat.sphinx.chat_common.ui.ChatFragment
import chat.sphinx.chat_tribe.R
import chat.sphinx.chat_tribe.databinding.FragmentChatTribeBinding
import chat.sphinx.chat_tribe.databinding.LayoutPodcastPlayerFooterBinding
import chat.sphinx.chat_tribe.model.TribePodcastData
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.menu_bottom.databinding.LayoutMenuBottomBinding
import chat.sphinx.resources.databinding.LayoutBoostFireworksBinding
import chat.sphinx.resources.getString
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_view.Px
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class ChatTribeFragment: ChatFragment<
        FragmentChatTribeBinding,
        ChatTribeFragmentArgs,
        ChatTribeViewModel,
        >(R.layout.fragment_chat_tribe)
{
    override val binding: FragmentChatTribeBinding by viewBinding(FragmentChatTribeBinding::bind)
    private val podcastPlayerBinding: LayoutPodcastPlayerFooterBinding
        get() = binding.includePodcastPlayerFooter
    private val boostAnimationBinding: LayoutBoostFireworksBinding
        get() = binding.includeLayoutBoostFireworks

    override val footerBinding: LayoutChatFooterBinding
        get() = binding.includeChatTribeFooter
    override val headerBinding: LayoutChatHeaderBinding
        get() = binding.includeChatTribeHeader
    override val replyingMessageBinding: LayoutMessageReplyBinding
        get() = binding.includeChatTribeMessageReply
    override val selectedMessageBinding: LayoutSelectedMessageBinding
        get() = binding.includeChatTribeSelectedMessage
    override val selectedMessageHolderBinding: LayoutMessageHolderBinding
        get() = binding.includeChatTribeSelectedMessage.includeLayoutMessageHolderSelectedMessage
    override val attachmentSendBinding: LayoutAttachmentSendPreviewBinding
        get() = binding.includeChatTribeAttachmentSendPreview
    override val menuBinding: LayoutChatMenuBinding
        get() = binding.includeChatTribeMenu
    override val callMenuBinding: LayoutMenuBottomBinding
        get() = binding.includeLayoutMenuBottomCall
    override val attachmentFullscreenBinding: LayoutAttachmentFullscreenBinding
        get() = binding.includeChatTribeAttachmentFullscreen

    override val menuEnablePayments: Boolean
        get() = false

    override val recyclerView: RecyclerView
        get() = binding.recyclerViewMessages

    override val viewModel: ChatTribeViewModel by viewModels()
    private val podcastViewModel: PodcastViewModel by viewModels()

    @Inject
    @Suppress("ProtectedInFinal", "PropertyName")
    protected lateinit var _userColorsHelper: UserColorsHelper
    override val userColorsHelper: UserColorsHelper
        get() = _userColorsHelper

    @Inject
    @Suppress("ProtectedInFinal", "PropertyName")
    protected lateinit var _imageLoader: ImageLoader<ImageView>
    override val imageLoader: ImageLoader<ImageView>
        get() = _imageLoader

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch(viewModel.mainImmediate) {
            try {
                viewModel.podcastDataStateFlow.collect { data ->
                    @Exhaustive
                    when (data) {
                        is TribePodcastData.Loading -> {}
                        is TribePodcastData.Result -> {
                            podcastViewModel.init(data)
                            throw Exception()
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        podcastPlayerBinding.apply {
            textViewBoostPodcastButton.setOnClickListener {
                podcastViewModel.currentViewState.clickBoost?.let {
                    it.invoke()
                    boostAnimationBinding.apply {
                        root.visible
                        lottieAnimationView.playAnimation()
                    }
                }
            }
            textViewForward30Button.setOnClickListener {
                podcastViewModel.currentViewState.clickFastForward?.invoke()
            }
            textViewPlayPauseButton.setOnClickListener {
                podcastViewModel.currentViewState.clickPlayPause?.invoke()
            }
            textViewEpisodeTitle.setOnClickListener {
                podcastViewModel.currentViewState.clickTitle?.invoke()
            }
        }

        boostAnimationBinding.lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener{
            override fun onAnimationEnd(animation: Animator?) {
                boostAnimationBinding.root.gone
            }

            override fun onAnimationRepeat(animation: Animator?) {}

            override fun onAnimationCancel(animation: Animator?) {}

            override fun onAnimationStart(animation: Animator?) {}
        })
    }

//    private fun configureContributions(contributions: String) {
//        headerBinding.apply {
//            textViewChatHeaderContributionsIcon.visible
//            textViewChatHeaderContributions.apply {
//                visible
//                @SuppressLint("SetTextI18n")
//                text = contributions
//            }
//        }
//    }

    private val progressWidth: Px by lazy {
        Px(binding.root.measuredWidth.toFloat())
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            podcastViewModel.boostAnimationViewStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is BoostAnimationViewState.Idle -> {}

                    is BoostAnimationViewState.BoosAnimationInfo -> {
                        boostAnimationBinding.apply {

                            viewState.photoUrl?.let { photoUrl ->
                                imageLoader.load(
                                    imageViewProfilePicture,
                                    photoUrl.value,
                                    ImageLoaderOptions.Builder()
                                        .placeholderResId(chat.sphinx.podcast_player.R.drawable.ic_profile_avatar_circle)
                                        .transformation(Transformation.CircleCrop)
                                        .build()
                                )
                            }

                            textViewSatsAmount.text = viewState.amount?.asFormattedString()
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            podcastViewModel.collectViewState { viewState ->
                podcastPlayerBinding.apply {
                    when (viewState) {
                        is PodcastViewState.Available -> {

                            textViewPlayPauseButton.text = if (viewState.showPlayButton) {
                                getString(R.string.material_icon_name_play_button)
                            } else {
                                getString(R.string.material_icon_name_pause_button)
                            }

                            val calculatedWidth = progressWidth.value.toDouble() * (viewState.playingProgress / 100.0)
                            progressBar.layoutParams.width = calculatedWidth.toInt()
                            progressBar.requestLayout()

                            textViewEpisodeTitle.text = viewState.title

                            if (viewState.showLoading) {
                                progressBarAudioLoading.visible
                            } else {
                                progressBarAudioLoading.gone
                            }

                            root.visible
                        }
                        is PodcastViewState.NoPodcast -> {
                            root.gone
                        }
                    }
                }
            }
        }
    }
}
