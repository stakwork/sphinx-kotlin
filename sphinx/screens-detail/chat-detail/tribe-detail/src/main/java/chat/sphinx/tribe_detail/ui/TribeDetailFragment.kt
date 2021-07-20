package chat.sphinx.tribe_detail.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
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
        (requireActivity() as InsetterActivity).addNavigationBarPadding(
            binding.constraintLayoutTribeDetailLayout
        )
    }

    private fun setupTribeDetailFunctionality() {

        viewModel.load()
    }

    override suspend fun onViewStateFlowCollect(viewState: TribeDetailViewState) {
        when(viewState) {
            is TribeDetailViewState.Idle -> {

            }
            is TribeDetailViewState.Tribe -> {
                binding.apply {
                    textViewTribeName.text = viewState.chat.name?.value ?: "Unnamed Tribe"
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
                        imageLoader.load(
                            imageViewProfilePicture,
                            it.value,
                            viewModel.imageLoaderDefaults,
                        )
                    }

                    viewState.podcast?.let {
                        constrainLayoutPodcastLightningControls.visible
                        textViewPodcastSatsPerMinuteValue.text = it.satsPerMinute.toString()
//                        seekBarSatsPerMinute.min = 0
//                        seekBarSatsPerMinute.max = 100
                    }
                }
            }
        }
    }
}
