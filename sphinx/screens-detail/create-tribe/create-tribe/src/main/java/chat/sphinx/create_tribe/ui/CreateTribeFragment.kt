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
import chat.sphinx.concept_network_query_chat.model.getStringOrEmpty
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
import javax.annotation.meta.Exhaustive
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
            textViewDetailScreenHeaderName.text = viewModel.headerText()
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }

            binding.buttonCreateTribe.text = viewModel.submitButtonText()
        }

        setupFragmentLayout()
        setupCreateTribe()

        bottomMenuPicture.initialize(
            R.string.bottom_menu_tribe_pic_header_text,
            binding.includeLayoutMenuBottomTribePic,
            viewLifecycleOwner
        )

        viewModel.load()
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
                    val selectedTags = viewModel.createTribeBuilder.selectedTags()
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
                viewModel.saveTribe()
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
        @Exhaustive
        when (viewState) {
            CreateTribeViewState.Idle -> {
                binding.progressBarCreateTribe.gone
                binding.progressBarLoadTribeContainer.gone
            }
            is CreateTribeViewState.TribeImageUpdated -> {
                imageLoader.load(
                    binding.imageViewTribePicture,
                    viewState.imageFile,
                    viewModel.imageLoaderDefaults
                )
            }
            CreateTribeViewState.SavingTribe -> {
                binding.progressBarCreateTribe.visible
            }
            is CreateTribeViewState.LoadingExistingTribe -> {
                binding.progressBarLoadTribeContainer.visible
            }
            is CreateTribeViewState.ExistingTribe -> {
                binding.progressBarLoadTribeContainer.gone

                val tribe = viewState.tribe

                binding.editTextTribeName.setText(tribe.name)

                tribe.img?.let { imgUrl ->
                    if (imgUrl.isNotEmpty()) {
                        binding.editTextTribeImage.setText(imgUrl)
                        imageLoader.load(
                            binding.imageViewTribePicture,
                            imgUrl,
                            viewModel.imageLoaderDefaults
                        )
                    }
                }

                binding.editTextTribeTags.text = tribe.tags.joinToString { it }

                binding.editTextTribeDescription.setText(tribe.description)

                binding.editTextTribePriceToJoin.setText(tribe.price_to_join?.getStringOrEmpty())
                binding.editTextTribePricePerMessage.setText(tribe.price_per_message?.getStringOrEmpty())
                binding.editTextTribeAmountToStake.setText(tribe.escrow_amount?.getStringOrEmpty())
                binding.editTextTribeTimeToStake.setText(tribe.hourToStake.getStringOrEmpty())

                binding.editTextTribeAppUrl.setText(tribe.app_url ?: "")
                binding.editTextTribeFeedUrl.setText(tribe.feed_url ?: "")
                binding.switchTribeListingOnSphinx.isChecked = tribe.unlisted == false
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
