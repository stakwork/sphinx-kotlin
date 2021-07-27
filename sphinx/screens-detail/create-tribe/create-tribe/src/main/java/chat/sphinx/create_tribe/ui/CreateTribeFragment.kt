package chat.sphinx.create_tribe.ui

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
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.create_tribe.R
import chat.sphinx.create_tribe.databinding.FragmentCreateTribeBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.menu_bottom_profile_pic.BottomMenuPicture
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class CreateTribeFragment: SideEffectFragment<
        Context,
        CreateTribeSideEffect,
        CreateTribeViewState,
        CreateTribeViewModel,
        FragmentCreateTribeBinding
        >(R.layout.fragment_create_tribe)
{
    override val viewModel: CreateTribeViewModel by viewModels()
    override val binding: FragmentCreateTribeBinding by viewBinding(FragmentCreateTribeBinding::bind)

    private val bottomMenuPicture: BottomMenuPicture by lazy(LazyThreadSafetyMode.NONE) {
        BottomMenuPicture(
            this,
            onStopSupervisor,
            viewModel
        )
    }

    @Inject
    lateinit var imageLoaderInj: ImageLoader<ImageView>

    private val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.includeCreateTribeHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.create_tribe_header_name)
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }

        setupFragmentLayout()
        setupCreateTribe()

        bottomMenuPicture.initialize(
            R.string.bottom_menu_tribe_pic_header_text,
            binding.includeLayoutMenuBottomTribePic,
            viewLifecycleOwner
        )
    }

    fun setupFragmentLayout() {
        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.constraintLayoutCreateTribeFragment)
            .addNavigationBarPadding(binding.includeLayoutMenuBottomTribePic.root)
    }

    private fun setupCreateTribe() {
        binding.apply {
            editTextTribeName.addTextChangedListener {
                viewModel.createTribeBuilder.setName(it.toString())

                updateCreateButtonState()
            }
            editTextTribeImage.addTextChangedListener {
                viewModel.createTribeBuilder.setImageUrl(it.toString())
            }
            editTextTribeImage.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    return@setOnFocusChangeListener
                }
                editTextTribeImage.text.toString()?.let { imageUrl ->
                    if (URLUtil.isValidUrl(imageUrl)) {
                        showTribeImage(imageUrl)
                    } else {
                        showTribeImage("")
                        editTextTribeImage.setText("")

                        invalidUrlAlert()
                    }
                }
            }
            constraintLayoutTribeImageContainer.setOnClickListener {
                viewModel.pictureMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Open)
            }
            editTextTribeDescription.addTextChangedListener {
                viewModel.createTribeBuilder.setDescription(it.toString())

                updateCreateButtonState()
            }

            constraintLayoutTribeTagsContainer.setOnClickListener {
                viewModel.selectTags() {
                    val selectedTags = viewModel.createTribeBuilder.tags.filter {
                        it.isSelected
                    }
                    if (selectedTags.isNotEmpty()) {
                        editTextTribeTags.text = (
                            selectedTags.joinToString {
                                it.name
                            }
                        )
                    } else {
                        editTextTribeTags.text = ""
                    }

                }
            }
            editTextTribePriceToJoin.addTextChangedListener {
                viewModel.createTribeBuilder.setPriceToJoin(
                    if (it.isNullOrEmpty()) {
                        null
                    } else {
                        it.toString().toLong()
                    }
                )
            }
            editTextTribePricePerMessage.addTextChangedListener {
                viewModel.createTribeBuilder.setPricePerMessage(
                    if (it.isNullOrEmpty()) {
                        null
                    } else {
                        it.toString().toLong()
                    }
                )
            }
            editTextTribeAmountToStake.addTextChangedListener {
                viewModel.createTribeBuilder.setEscrowAmount(
                    if (it.isNullOrEmpty()) {
                        null
                    } else {
                        it.toString().toLong()
                    }
                )
            }
            editTextTribeTimeToStake.addTextChangedListener {
                viewModel.createTribeBuilder.setEscrowMillis(
                    if (it.isNullOrEmpty()) {
                        null
                    } else {
                        it.toString().toLong() * Companion.MILLISECONDS_IN_AN_HOUR
                    }
                )
            }
            editTextTribeAppUrl.addTextChangedListener {
                viewModel.createTribeBuilder.setAppUrl(it.toString())
            }
            editTextTribeFeedUrl.addTextChangedListener {
                viewModel.createTribeBuilder.setFeedUrl(it.toString())
            }
            switchTribeListingOnSphinx.setOnCheckedChangeListener { _, isChecked ->
                viewModel.createTribeBuilder.setUnlisted(!isChecked)
            }
            switchTribeApproveMembershipOnSphinx.setOnCheckedChangeListener { _, isChecked ->
                viewModel.createTribeBuilder.setUnlisted(isChecked)
            }

            buttonCreateTribe.setOnClickListener {
                viewModel.createTribe()
            }
        }
    }

    private fun invalidUrlAlert() {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.submitSideEffect(
                CreateTribeSideEffect.InvalidUrl
            )
        }
    }

    private fun showTribeImage(url: String?) {
        if (url == null) {
            binding.imageViewTribePicture.setImageDrawable(
                ContextCompat.getDrawable(
                binding.root.context,
                R.drawable.ic_tribe
            ))
            return
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            imageLoader.load(
                binding.imageViewTribePicture,
                url,
                ImageLoaderOptions.Builder()
                    .placeholderResId(R.drawable.ic_tribe)
                    .transformation(Transformation.CircleCrop)
                    .build()
            )
        }
    }

    private fun updateCreateButtonState() {
        requireActivity().let {
            binding.apply {
                buttonCreateTribe.alpha = if (viewModel.createTribeBuilder.hasRequiredFields) {
                    1.0f
                } else {
                    0.5f
                }

                buttonCreateTribe.isEnabled = viewModel.createTribeBuilder.hasRequiredFields
            }
        }
    }
    override suspend fun onViewStateFlowCollect(viewState: CreateTribeViewState) {
        when (viewState) {
            CreateTribeViewState.Idle -> {
                binding.progressBarCreateTribe.gone
            }
            is CreateTribeViewState.TribeImageUpdated -> {
                imageLoader.load(
                    binding.imageViewTribePicture,
                    viewState.imageFile,
                    viewModel.imageLoaderDefaults
                )
            }
            CreateTribeViewState.CreatingTribe -> {
                binding.progressBarCreateTribe.visible
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: CreateTribeSideEffect) {
        sideEffect.execute(requireActivity())
    }

    companion object {
        private const val MILLISECONDS_IN_AN_HOUR = 3_600_000L
    }
}
