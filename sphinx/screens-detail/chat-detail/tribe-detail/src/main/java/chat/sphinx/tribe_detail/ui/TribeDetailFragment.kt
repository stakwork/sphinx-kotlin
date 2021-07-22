package chat.sphinx.tribe_detail.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.menu_bottom_tribe_profile_pic.BottomMenuTribeProfilePic
import chat.sphinx.tribe.BottomMenuTribe
import chat.sphinx.tribe_detail.R
import chat.sphinx.tribe_detail.databinding.FragmentTribeDetailBinding
import chat.sphinx.wrapper_common.eeemmddhmma
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class TribeDetailFragment: SideEffectFragment<
        Context,
        TribeDetailSideEffect,
        TribeDetailViewState,
        TribeDetailViewModel,
        FragmentTribeDetailBinding
        >(R.layout.fragment_tribe_detail)
{
    override val viewModel: TribeDetailViewModel by viewModels()
    override val binding: FragmentTribeDetailBinding by viewBinding(FragmentTribeDetailBinding::bind)

    @Inject
    lateinit var imageLoaderInj: ImageLoader<ImageView>

    private val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj

    private val bottomMenuTribe: BottomMenuTribe by lazy(LazyThreadSafetyMode.NONE) {
        BottomMenuTribe(
            onStopSupervisor,
            viewModel
        )
    }

    private val bottomMenuTribeProfilePic: BottomMenuTribeProfilePic by lazy(LazyThreadSafetyMode.NONE) {
        BottomMenuTribeProfilePic(
            this,
            onStopSupervisor,
            viewModel
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        BackPressHandler(viewLifecycleOwner, requireActivity())

        binding.includeTribeDetailHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.tribe_detail_header_name)
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }

        setupFragmentLayout()
        setupTribeDetail()

        bottomMenuTribeProfilePic.initialize(binding.includeLayoutMenuBottomTribeProfilePic, viewLifecycleOwner)
    }

    private inner class BackPressHandler(
        owner: LifecycleOwner,
        activity: FragmentActivity,
    ): OnBackPressedCallback(true) {

        init {
            activity.apply {
                onBackPressedDispatcher.addCallback(
                    owner,
                    this@BackPressHandler,
                )
            }
        }

        override fun handleOnBackPressed() {
            if (viewModel.tribeProfilePicMenuHandler.viewStateContainer.value is MenuBottomViewState.Open) {
                viewModel.tribeProfilePicMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Closed)
            } else if (viewModel.tribeMenuHandler.viewStateContainer.value is MenuBottomViewState.Open) {
                viewModel.tribeMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Closed)
            } else {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }
    }

    private fun setupFragmentLayout() {
        val insetterActivity = requireActivity() as InsetterActivity

        insetterActivity.addNavigationBarPadding(
            binding.layoutConstraintTribeDetailLayout
        )
        insetterActivity.addNavigationBarPadding(
            binding.includeLayoutMenuBottomTribe.root
        )
        insetterActivity.addNavigationBarPadding(
            binding.includeLayoutMenuBottomTribeProfilePic.root
        )
    }

    private fun setupTribeDetail() {

        binding.apply {
            editTextProfileAliasValue.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    return@setOnFocusChangeListener
                }

                viewModel.updateProfileAlias(editTextProfileAliasValue.text.toString())
            }
            seekBarSatsPerMinute.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {

                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        textViewPodcastSatsPerMinuteValue.text = progress.toString()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) { }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        seekBar?.let {
                            // TODO: Set satsPerMinute on this podcast
                        }
                    }
                }
            )

            textViewMenuButton.setOnClickListener {
                viewModel.tribeMenuHandler.viewStateContainer.updateViewState(
                    MenuBottomViewState.Open
                )
            }

            buttonProfilePicture.setOnClickListener {
                viewModel.tribeProfilePicMenuHandler.viewStateContainer.updateViewState(
                    MenuBottomViewState.Open
                )
            }
        }
        viewModel.load()
    }

    override suspend fun onViewStateFlowCollect(viewState: TribeDetailViewState) {
        @Exhaustive
        when(viewState) {
            is TribeDetailViewState.Idle -> { }
            is TribeDetailViewState.ErrorUpdatingTribeProfilePicture -> {
                binding.progressBarUploadProfilePicture.gone

                viewModel.tribeProfilePicMenuHandler.viewStateContainer.updateViewState(
                    MenuBottomViewState.Closed
                )
            }
            is TribeDetailViewState.UpdatingTribeProfilePicture -> {
                binding.progressBarUploadProfilePicture.visible

                viewModel.tribeProfilePicMenuHandler.viewStateContainer.updateViewState(
                    MenuBottomViewState.Closed
                )
            }
            is TribeDetailViewState.TribeProfile -> {
                binding.apply {
                    // Hide the upload picture progress indicator
                    progressBarUploadProfilePicture.gone
                    // Initialize the Tribe Specific menu bar with the
                    bottomMenuTribe.initialize(
                        viewState.chat,
                        viewState.accountOwner,
                        includeLayoutMenuBottomTribe,
                        viewLifecycleOwner
                    )
                    textViewMenuButton.visible

                    textViewTribeName.text = viewState.chat.name?.value
                    textViewTribeCreateDate.text = getString(
                        R.string.tribe_created_on,
                        viewState.chat.createdAt.eeemmddhmma()
                    )
                    textViewTribeConfigurations.text = getString(
                        R.string.tribe_costs,
                        viewState.chat.pricePerMessage?.value ?: 0L,
                        viewState.chat.escrowAmount?.value ?: 0L
                    )

                    val userAlias = viewState.chat.myAlias?.value ?: viewState.accountOwner.alias?.value
                    editTextProfileAliasValue.setText(userAlias)

                    viewState.chat.photoUrl?.let {
                        imageLoader.load(
                            imageViewTribePicture,
                            it.value,
                            viewModel.imageLoaderDefaults,
                        )
                    }

                    val userPhotoUrl = viewState.chat.myPhotoUrl ?: viewState.accountOwner.photoUrl
                    userPhotoUrl?.let {
                        editTextProfilePictureValue.setText(it.value)

                        imageLoader.load(
                            imageViewProfilePicture,
                            it.value,
                            viewModel.imageLoaderDefaults,
                        )
                    }

                    viewState.podcast?.let {
                        constrainLayoutPodcastLightningControls.visible
                        seekBarSatsPerMinute.progress = it.satsPerMinute.toInt()
                        textViewPodcastSatsPerMinuteValue.text = it.satsPerMinute.toString()
                    }
                }
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: TribeDetailSideEffect) {
        sideEffect.execute(requireActivity())
    }
}
