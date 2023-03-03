package chat.sphinx.create_badge.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.create_badge.R
import chat.sphinx.create_badge.databinding.FragmentCreateBadgeBinding
import chat.sphinx.resources.getString
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.invisible
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class CreateBadgeFragment: SideEffectFragment<
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

        binding.includeCreateBadgeHeader.textViewDetailScreenClose.setOnClickListener {
            lifecycleScope.launch(viewModel.mainImmediate) {
                viewModel.navigator.popBackStack()
            }
        }

    }

    override suspend fun onViewStateFlowCollect(viewState: CreateBadgeViewState) {
        when (viewState) {
            is CreateBadgeViewState.Idle -> {}
            is CreateBadgeViewState.EditBadge -> {

                binding.apply {

                    layoutConstraintDeactivateBadge.gone
                    layoutConstraintCreateBadge.gone
                    layoutConstraintButtonCreateBadge.gone
                    textViewBadgesRowCount.visible
                    textViewBadgesLeft.visible
                    layoutConstraintDeactivateBadge.visible

                    imageLoader.load(
                        imageViewBadgeImage,
                        viewState.badge.imageUrl,
                        viewModel.imageLoaderDefaults,
                    )

                    val badgesAmount = (viewState.badge.amountCreated?.minus(viewState.badge.amountIssued ?: 0)).toString()
                    val badgesLeft = String.format(getString(R.string.badges_left), viewState.badge.amountCreated)

                    textViewBadgeName.text = viewState.badge.name
                    textViewBadgeEditDescription.text = viewState.badge.description
                    textViewBadgesRowCount.text = badgesAmount
                    textViewBadgesLeft.text = badgesLeft
                    switchDeactivateBadge.isChecked = viewState.badge.isActive
                }
            }
            is CreateBadgeViewState.Template -> {
                binding.apply {

                    layoutConstraintButtonCreateBadge.visible
                    textViewBadgesRowCount.visible
                    textViewBadgesLeft.visible
                    layoutConstraintDeactivateBadge.gone
                    layoutConstraintCreateBadge.visible
                    textViewBadgesRowCount.gone
                    textViewBadgesLeft.gone

                    viewState.badgeTemplate.imageUrl?.let {
                        imageLoader.load(
                            imageViewBadgeImage,
                            it,
                            viewModel.imageLoaderDefaults,
                        )
                    }
                    textViewBadgeName.text = viewState.badgeTemplate.name
                    textViewBadgesRequirementDescription.text = viewState.badgeTemplate.description
                }

            }
        }

    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()
    }

    override suspend fun onSideEffectCollect(sideEffect: CreateBadgeSideEffect) {
    }


}