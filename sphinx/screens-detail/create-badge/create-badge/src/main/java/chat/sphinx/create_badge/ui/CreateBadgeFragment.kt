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
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
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
                    imageLoader.load(
                        imageViewBadgeImage,
                        viewState.badgeImage,
                        viewModel.imageLoaderDefaults,
                    )

                    textViewBadgeName.text = viewState.badgeName
                    textViewBadgeEditDescription.text = viewState.badgeDescription
                    textViewBadgesRowCount.text = viewState.badgeAmount
                    textViewBadgesLeft.text = viewState.badgeLeft
                    switchDeactivateBadge.isChecked = setInverseSwitch(viewState.badgeActive)
                }
            }
        }

    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()
    }

    override suspend fun onSideEffectCollect(sideEffect: CreateBadgeSideEffect) {
    }

    private fun setInverseSwitch(isActive: Boolean): Boolean = !isActive



}