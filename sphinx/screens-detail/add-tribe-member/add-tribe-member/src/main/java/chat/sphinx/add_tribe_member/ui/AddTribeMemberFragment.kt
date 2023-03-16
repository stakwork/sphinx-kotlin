package chat.sphinx.add_tribe_member.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.webkit.URLUtil
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.add_tribe_member.databinding.FragmentAddTribeMemberBinding
import chat.sphinx.add_tribe_member.R
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.OnImageLoadListener
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.menu_bottom_profile_pic.BottomMenuPicture
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class AddTribeMemberFragment: SideEffectDetailFragment<
        Context,
        AddTribeMemberSideEffect,
        AddTribeMemberViewState,
        AddTribeMemberViewModel,
        FragmentAddTribeMemberBinding
        >(R.layout.fragment_add_tribe_member)
{
    override val viewModel: AddTribeMemberViewModel by viewModels()
    override val binding: FragmentAddTribeMemberBinding by viewBinding(FragmentAddTribeMemberBinding::bind)

    private val bottomMenuPicture: BottomMenuPicture by lazy(LazyThreadSafetyMode.NONE) {
        BottomMenuPicture(
            this,
            onStopSupervisor,
            viewModel
        )
    }

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.includeAddMemberHeader.apply {
            textViewDetailScreenHeaderNavBack.visible
            textViewDetailScreenHeaderName.text = getString(R.string.add_member_header_name)

            textViewDetailScreenClose.gone

            textViewDetailScreenHeaderNavBack.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
                }
            }
        }

        setupAddMember()
        setupFragmentLayout()

        bottomMenuPicture.initialize(
            R.string.bottom_menu_member_pic_header_text,
            binding.includeLayoutMenuBottomMemberPic,
            viewLifecycleOwner
        )
    }

    override fun closeDetailsScreen() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.navigator.closeDetailScreen()
        }
    }

    private fun setupFragmentLayout() {
        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.constraintLayoutAddMemberFragment)
            .addNavigationBarPadding(binding.includeLayoutMenuBottomMemberPic.root)
    }

    private fun setupAddMember() {
        binding.apply {
            editTextMemberAlias.addTextChangedListener {
                viewModel.addTribeMemberBuilder.setAlias(it.toString())
                updateCreateButtonState()
            }

            editTextMemberPublicKey.addTextChangedListener {
                viewModel.addTribeMemberBuilder.setPublicKey(it.toString())
                updateCreateButtonState()
            }

            editTextMemberRouteHint.addTextChangedListener {
                viewModel.addTribeMemberBuilder.setRouteHint(it.toString())
            }

            editTextMemberContactKey.addTextChangedListener {
                viewModel.addTribeMemberBuilder.setContactKey(it.toString())
                updateCreateButtonState()
            }

            editTextMemberImage.addTextChangedListener {
                viewModel.addTribeMemberBuilder.setPhotoUrl(it.toString())
            }
            editTextMemberImage.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    return@setOnFocusChangeListener
                }
                editTextMemberImage.text?.toString().let { imageUrl ->
                    if (URLUtil.isValidUrl(imageUrl)) {
                        showMemberImage(imageUrl)
                    } else {
                        showMemberImage(null)
                        editTextMemberImage.setText("")

                        invalidUrlAlert()
                    }
                }
            }
            constraintLayoutMemberImageContainer.setOnClickListener {
                viewModel.pictureMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Open)
            }

            buttonAddMember.setOnClickListener {
                viewModel.addTribeMember()
            }
        }
    }

    private fun invalidUrlAlert() {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.submitSideEffect(
                AddTribeMemberSideEffect.InvalidUrl
            )
        }
    }

    private fun showMemberImage(url: String?) {
        if (url == null) {
            if (viewModel.addTribeMemberBuilder.isImageSet) {
                return
            }

            binding.imageViewMemberPicture.setImageDrawable(
                ContextCompat.getDrawable(
                    binding.root.context,
                    R.drawable.ic_media_library
                ))

            val padding = resources.getDimensionPixelSize(R.dimen.default_medium_layout_margin)
            binding.imageViewMemberPicture.setPadding(padding, padding, padding, padding)
            return
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            imageLoader.load(
                binding.imageViewMemberPicture,
                url,
                ImageLoaderOptions.Builder()
                    .placeholderResId(R.drawable.ic_media_library)
                    .transformation(Transformation.CircleCrop)
                    .build(),
                object: OnImageLoadListener {
                    override fun onSuccess() {
                        super.onSuccess()

                        binding.imageViewMemberPicture.setPadding(0, 0, 0, 0)
                    }
                }
            )
        }
    }

    private fun updateCreateButtonState() {
        requireActivity().let {
            binding.apply {
                buttonAddMember.alpha = if (viewModel.addTribeMemberBuilder.hasRequiredFields) {
                    1.0f
                } else {
                    0.5f
                }

                buttonAddMember.isEnabled = viewModel.addTribeMemberBuilder.hasRequiredFields
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: AddTribeMemberViewState) {
        @Exhaustive
        when (viewState) {
            AddTribeMemberViewState.Idle -> {
                binding.progressBarAddMember.gone
            }
            is AddTribeMemberViewState.MemberImageUpdated -> {
                imageLoader.load(
                    binding.imageViewMemberPicture,
                    viewState.imageFile,
                    viewModel.imageLoaderDefaults
                )
                binding.imageViewMemberPicture.setPadding(0, 0, 0, 0)
            }
            AddTribeMemberViewState.SavingMember -> {
                binding.progressBarAddMember.visible
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: AddTribeMemberSideEffect) {
        sideEffect.execute(requireActivity())
    }
}
