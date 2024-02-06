package chat.sphinx.create_tribe.ui

import android.content.Context
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.View
import android.webkit.URLUtil
import android.widget.ImageView
import android.widget.PopupMenu
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
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import chat.sphinx.wrapper_common.feed.FeedType
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class CreateTribeFragment: SideEffectDetailFragment<
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
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.includeCreateTribeHeader.apply {
            textViewDetailScreenHeaderName.text = viewModel.headerText()

            textViewDetailScreenClose.goneIfFalse(!viewModel.isEditingTribe())
            textViewDetailScreenHeaderNavBack.goneIfFalse(viewModel.isEditingTribe())

            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }

            textViewDetailScreenHeaderNavBack.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
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

    override fun closeDetailsScreen() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.navigator.closeDetailScreen()
        }
    }

    private fun setupFragmentLayout() {
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
                editTextTribeImage.text?.toString().let { imageUrl ->
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
                        it.toString().toLongOrNull()
                    }
                )
            }
            editTextTribePricePerMessage.addTextChangedListener {
                viewModel.createTribeBuilder.setPricePerMessage(
                    if (it.isNullOrEmpty()) {
                        null
                    } else {
                        it.toString().toLongOrNull()
                    }
                )
            }
            editTextTribeAmountToStake.addTextChangedListener {
                viewModel.createTribeBuilder.setEscrowAmount(
                    if (it.isNullOrEmpty()) {
                        null
                    } else {
                        it.toString().toLongOrNull()
                    }
                )
            }
            editTextTribeTimeToStake.addTextChangedListener {
                viewModel.createTribeBuilder.setEscrowMillis(
                    if (it.isNullOrEmpty()) {
                        null
                    } else {
                        (it.toString().toLongOrNull() ?: 0) * MILLISECONDS_IN_AN_HOUR
                    }
                )
            }
            editTextTribeAppUrl.addTextChangedListener {
                viewModel.createTribeBuilder.setAppUrl(it.toString())
            }
            editTextTribeFeedUrl.addTextChangedListener {
                viewModel.createTribeBuilder.setFeedUrl(it.toString())

                updateCreateButtonState()
            }

            textViewTribeFeedContentTypeValue.setOnClickListener {
                showFeedContentTypePopup()
            }

            switchTribeListingOnSphinx.setOnCheckedChangeListener { _, isChecked ->
                viewModel.createTribeBuilder.setUnlisted(!isChecked)
            }
            switchTribeApproveMembershipOnSphinx.setOnCheckedChangeListener { _, isChecked ->
                viewModel.createTribeBuilder.setPrivate(!isChecked)
            }

            buttonCreateTribe.setOnClickListener {
                viewModel.saveTribe()
            }
        }
    }

    private fun showFeedContentTypePopup() {
        binding.apply {
            if (editTextTribeFeedUrl.text?.isEmpty() == true) {
                return
            }

            val wrapper: Context = ContextThemeWrapper(context, R.style.feedTypeMenu)
            val popup = PopupMenu(wrapper, textViewTribeFeedContentTypeValue)
            popup.inflate(R.menu.feed_type_menu)

            popup.setOnMenuItemClickListener { item: MenuItem? ->
                when (item!!.itemId) {
                    R.id.podcast -> {
                        textViewTribeFeedContentTypeValue.text = getString(R.string.feed_type_podcast)
                        viewModel.createTribeBuilder.setFeedType(FeedType.Podcast.value)
                    }
                    R.id.video -> {
                        textViewTribeFeedContentTypeValue.text = getString(R.string.feed_type_video)
                        viewModel.createTribeBuilder.setFeedType(FeedType.Video.value)
                    }
                    R.id.newsletter -> {
                        textViewTribeFeedContentTypeValue.text = getString(R.string.feed_type_newsletter)
                        viewModel.createTribeBuilder.setFeedType(FeedType.Newsletter.value)
                    }
                }
                updateCreateButtonState()

                true
            }
            popup.show()
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

                binding.editTextTribeName.setText(viewState.name)

                viewState.imageUrl?.let { imgUrl ->
                    if (imgUrl.isNotEmpty()) {
                        binding.editTextTribeImage.setText(imgUrl)
                        imageLoader.load(
                            binding.imageViewTribePicture,
                            imgUrl,
                            viewModel.imageLoaderDefaults
                        )
                    }
                }

                binding.editTextTribeTags.text = viewState.tags.joinToString { it }

                binding.editTextTribeDescription.setText(viewState.description)

                binding.editTextTribePriceToJoin.setText(viewState.priceToJoin)
                binding.editTextTribePricePerMessage.setText(viewState.pricePerMessage)
                binding.editTextTribeAmountToStake.setText(viewState.escrowAmount)
                binding.editTextTribeTimeToStake.setText(viewState.hourToStake)

                binding.editTextTribeAppUrl.setText(viewState.appUrl ?: "")
                binding.editTextTribeFeedUrl.setText(viewState.feedUrl ?: "")

                viewState.feedTypeDescriptionRes?.let { feedTypeDescriptionResource ->
                    binding.textViewTribeFeedContentTypeValue.text = getString(
                        feedTypeDescriptionResource
                    )
                }

                binding.switchTribeListingOnSphinx.isChecked = viewState.unlisted == false
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
