package chat.sphinx.tribe_detail.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.tribe.BottomMenuTribe
import chat.sphinx.tribe_detail.R
import chat.sphinx.tribe_detail.databinding.FragmentTribeDetailBinding
import chat.sphinx.wrapper_common.eeemmddhmma
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class TribeDetailFragment: BaseFragment<
        TribeDetailViewState,
        TribeDetailViewModel,
        FragmentTribeDetailBinding
        >(R.layout.fragment_tribe_detail)
{
    override val viewModel: TribeDetailViewModel by viewModels()
    override val binding: FragmentTribeDetailBinding by viewBinding(FragmentTribeDetailBinding::bind)

    @Inject
    lateinit var imageLoaderInj: ImageLoader<ImageView>

    val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj

    private val bottomMenuTribe: BottomMenuTribe by lazy(LazyThreadSafetyMode.NONE) {
        BottomMenuTribe(
            this,
            onStopSupervisor,
            viewModel
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.includeTribeDetailHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.tribe_detail_header_name)
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }

        setupFragmentLayout()
        setupTribeDetailFunctionality()


    }

    private fun setupFragmentLayout() {
        val insetterActivity = requireActivity() as InsetterActivity

        insetterActivity.addNavigationBarPadding(
            binding.constraintLayoutTribeDetailLayout
        )
        insetterActivity.addNavigationBarPadding(
            binding.includeLayoutMenuBottomTribe.root
        )
    }

    private fun setupTribeDetailFunctionality() {

        binding.apply {
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
            imageViewMenuButton.setOnClickListener {
                viewModel.tribeMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Open)
            }
        }
        viewModel.load()
    }

    override suspend fun onViewStateFlowCollect(viewState: TribeDetailViewState) {
        when(viewState) {
            is TribeDetailViewState.Idle -> { }
            is TribeDetailViewState.Tribe -> {
                binding.apply {
                    bottomMenuTribe.initialize(
                        viewState.chat,
                        viewState.accountOwner,
                        includeLayoutMenuBottomTribe,
                        viewLifecycleOwner
                    )
                    imageViewMenuButton.visible

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
                    editTextProfileAliasValue.setText(viewState.chat.myAlias?.value)

                    viewState.chat.photoUrl?.let {
                        imageLoader.load(
                            imageViewTribePicture,
                            it.value,
                            viewModel.imageLoaderDefaults,
                        )
                    }

                    viewState.chat.myPhotoUrl?.let {
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
}
