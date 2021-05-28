package chat.sphinx.join_tribe.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_network_query_chat.model.TribeDto
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.join_tribe.R
import chat.sphinx.join_tribe.databinding.FragmentJoinTribeBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class JoinTribeFragment: BaseFragment<
        JoinTribeViewState,
        JoinTribeViewModel,
        FragmentJoinTribeBinding
        >(R.layout.fragment_join_tribe)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val viewModel: JoinTribeViewModel by viewModels()
    override val binding: FragmentJoinTribeBinding by viewBinding(FragmentJoinTribeBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as InsetterActivity).addNavigationBarPadding(binding.layoutScrollViewContent)

        binding.includeJoinTribeHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.join_tribe_header_name)
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch { viewModel.navigator.closeDetailScreen() }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.accountOwnerStateFlow.collect { owner ->
                owner?.alias?.value.let { ownerAlias ->
                    binding.includeTribeMemberInfo.tribeMemberAliasEditText.setText(ownerAlias.toString())
                }
            }
        }

        viewModel.loadTribeData()
    }

    override suspend fun onViewStateFlowCollect(viewState: JoinTribeViewState) {
        @Exhaustive
        when (viewState) {
            is JoinTribeViewState.LoadingTribeInfo -> {
                binding.loadingTribeInfoContent.goneIfFalse(true)
            }
            is JoinTribeViewState.LoadingTribeFailed -> {
                viewModel.navigator.closeDetailScreen()
            }
            is JoinTribeViewState.TribeInfo -> {
                val tribeDto = viewState.tribe
                showTribeData(tribeDto)
            }
        }
    }

    private fun showTribeData(tribe: TribeDto) {
        binding.apply {
            loadTribeImage(tribe)

            textViewTribeName.text = tribe.name ?: "No Name"
            textViewTribeDescription.text = tribe.description ?: "No Description"
            includeTribePrice.textViewPricePerMessage.text = (tribe.price_per_message ?: 0).toString()
            includeTribePrice.textViewPriceToJoin.text = (tribe.price_to_join ?: 0).toString()
            includeTribePrice.textViewAmountToStake.text = (tribe.escrow_amount ?: 0).toString()
            includeTribePrice.textViewTimeToStake.text = tribe.hourToStake.toString()

            loadingTribeInfoContent.goneIfFalse(false)
        }
    }

    private fun loadTribeImage(tribe: TribeDto) {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            tribe.img?.let { img ->
                imageLoader.load(
                    binding.imageViewTribePicture,
                    img,
                    ImageLoaderOptions.Builder()
                        .placeholderResId(R.drawable.ic_tribe_placeholder)
                        .transformation(Transformation.CircleCrop)
                        .build()
                )
            } ?: binding.imageViewTribePicture
                .setImageDrawable(
                    ContextCompat.getDrawable(
                        binding.root.context,
                        R.drawable.ic_tribe_placeholder
                    )
                )
        }
    }
}
