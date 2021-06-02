package chat.sphinx.join_tribe.ui

import android.content.Context
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
import chat.sphinx.wrapper_contact.toContactAlias
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class JoinTribeFragment: SideEffectFragment<
        Context,
        JoinTribeSideEffect,
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

        binding.buttonJoin.setOnClickListener {
            joinTribe()
        }

        viewModel.loadTribeData()
    }

    override fun onStart() {
        super.onStart()
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.accountOwnerStateFlow.collect { owner ->
                owner?.alias?.value.let { ownerAlias ->
                    binding.includeTribeMemberInfo.tribeMemberAliasEditText.setText(ownerAlias.toString())
                }
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: JoinTribeViewState) {
        @Exhaustive
        when (viewState) {
            is JoinTribeViewState.LoadingTribe -> {
                binding.loadingTribeInfoContent.goneIfFalse(true)
            }
            is JoinTribeViewState.ErrorLoadingTribe -> {
                viewModel.navigator.closeDetailScreen()
            }
            is JoinTribeViewState.TribeLoaded -> {
                showTribeData(viewState.tribe)
            }

            is JoinTribeViewState.JoiningTribe -> {
                binding.buttonJoin.isEnabled = false
                binding.joinTribeSaveProgress.goneIfFalse(true)
            }
            is JoinTribeViewState.ErrorJoiningTribe -> {
                viewModel.navigator.closeDetailScreen()
            }
            is JoinTribeViewState.TribeJoined -> {
                viewModel.navigator.closeDetailScreen()
            }
        }
    }

    private fun showTribeData(tribe: TribeDto) {
        binding.apply {
            loadTribeImage(tribe)

            textViewTribeName.text = tribe.name
            textViewTribeDescription.text = tribe.description
            includeTribePrice.textViewPricePerMessage.text = tribe.price_per_message.toString()
            includeTribePrice.textViewPriceToJoin.text = tribe.price_to_join.toString()
            includeTribePrice.textViewAmountToStake.text = tribe.escrow_amount.toString()
            includeTribePrice.textViewTimeToStake.text = tribe.hourToStake.toString()

            loadingTribeInfoContent.goneIfFalse(false)
        }
    }

    private fun joinTribe() {
        val aliasString = binding.includeTribeMemberInfo.tribeMemberAliasEditText.text.toString()
        viewModel.joinTribe(aliasString)
    }

    private fun loadTribeImage(tribe: TribeDto) {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            tribe.img?.let { img ->
                if (img.isNotEmpty()) {
                    imageLoader.load(
                        binding.imageViewTribePicture,
                        img,
                        ImageLoaderOptions.Builder()
                            .placeholderResId(R.drawable.ic_tribe_placeholder)
                            .transformation(Transformation.CircleCrop)
                            .build()
                    )
                }
            } ?: binding.imageViewTribePicture
                .setImageDrawable(
                    ContextCompat.getDrawable(
                        binding.root.context,
                        R.drawable.ic_tribe_placeholder
                    )
                )
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: JoinTribeSideEffect) {
        sideEffect.execute(binding.root.context)
    }
}
