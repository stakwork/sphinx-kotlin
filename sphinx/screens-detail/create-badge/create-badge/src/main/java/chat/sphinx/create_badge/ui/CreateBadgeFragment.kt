package chat.sphinx.create_badge.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.create_badge.R
import chat.sphinx.create_badge.databinding.FragmentCreateBadgeBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.resources.getString
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class CreateBadgeFragment: SideEffectDetailFragment<
        Context,
        CreateBadgeSideEffect,
        CreateBadgeViewState,
        CreateBadgeViewModel,
        FragmentCreateBadgeBinding
        >(R.layout.fragment_create_badge)
{
    override val viewModel: CreateBadgeViewModel by viewModels()
    override val binding: FragmentCreateBadgeBinding by viewBinding(FragmentCreateBadgeBinding::bind)

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            (requireActivity() as InsetterActivity)
                .addNavigationBarPadding(binding.root)

            includeCreateBadgeHeader.textViewDetailScreenClose.gone
            includeCreateBadgeHeader.textViewDetailScreenHeaderNavBack.visible
            includeCreateBadgeHeader.textViewDetailScreenHeaderNavBack.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
                }
            }

            switchDeactivateBadge.setOnCheckedChangeListener { compoundButton, _ ->
                if(!compoundButton.isPressed) {
                    return@setOnCheckedChangeListener
                }
                viewModel.toggleBadgeState()
            }

            layoutConstraintButtonCreateBadge.setOnClickListener {
                viewModel.createBadge(
                    amount = quantityNumber.text.toString().toIntOrNull() ?: 100,
                    description = textViewBadgeDescription.text.toString()
                )
            }

            buttonBadgesQuantityMinus.setOnClickListener {
                viewModel.decreaseQuantity()
            }

            buttonBadgesQuantityPlus.setOnClickListener {
                viewModel.increaseQuantity()
            }
        }
    }

    override fun closeDetailsScreen() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.navigator.closeDetailScreen()
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: CreateBadgeViewState) {
        when (viewState) {
            is CreateBadgeViewState.Idle -> {}
            is CreateBadgeViewState.LoadingCreateBadge -> {
                binding.apply {
                    progressBarCreatingBadge.visible
                    layoutConstraintButtonCreateBadge.isEnabled = false
                }
            }
            is CreateBadgeViewState.ToggleBadge -> {
                binding.apply {

                    progressBarCreatingBadge.gone
                    layoutConstraintButtonCreateBadge.isEnabled = false

                    layoutConstraintToggleBadgeState.visible
                    layoutConstraintCreateBadge.gone

                    imageLoader.load(
                        imageViewBadgeImage,
                        viewState.badge.imageUrl,
                        viewModel.imageLoaderDefaults,
                    )

                    val badgesAmount = (viewState.badge.amountCreated?.minus(viewState.badge.amountIssued ?: 0)).toString()
                    val badgesLeft = String.format(getString(R.string.badges_left), viewState.badge.amountCreated)

                    textViewBadgeName.text = viewState.badge.name
                    textViewBadgesRowCount.text = badgesAmount
                    textViewBadgeDescription.text = viewState.badge.description
                    textViewBadgesLeft.text = badgesLeft
                    switchDeactivateBadge.isChecked = viewState.badge.isActive
                }
            }
            is CreateBadgeViewState.CreateBadge -> {
                binding.apply {

                    progressBarCreatingBadge.gone
                    layoutConstraintButtonCreateBadge.isEnabled = true

                    layoutConstraintToggleBadgeState.gone
                    layoutConstraintCreateBadge.visible

                    viewState.badgeTemplate.imageUrl?.let {
                        imageLoader.load(
                            imageViewBadgeImage,
                            it,
                            viewModel.imageLoaderDefaults,
                        )
                    }
                    textViewBadgeName.text = viewState.badgeTemplate.name
                    textViewBadgeDescription.text = viewState.badgeTemplate.description

                    quantityNumber.text = viewState.currentQuantity.toString()
                    textViewSatsPerBadge.text = viewState.pricePerBadge.toString()
                    textViewTotalSatsAmount.text = (viewState.currentQuantity * viewState.pricePerBadge).toString()
                }
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: CreateBadgeSideEffect) {
        sideEffect.execute(binding.root.context)
    }
}