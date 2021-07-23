package chat.sphinx.create_tribe.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.create_tribe.R
import chat.sphinx.create_tribe.databinding.FragmentCreateTribeBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.menu_bottom_tribe_pic.BottomMenuTribePic
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
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

    private val bottomMenuProfilePic: BottomMenuTribePic by lazy(LazyThreadSafetyMode.NONE) {
        BottomMenuTribePic(
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

        bottomMenuProfilePic.initialize(binding.includeLayoutMenuBottomTribePic, viewLifecycleOwner)
    }

    fun setupFragmentLayout() {
        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.constraintLayoutCreateTribeFragment)
    }

    fun setupCreateTribe() {
        binding.apply {
            editTextTribeName.addTextChangedListener {
                viewModel.createTribeBuilder.setName(it.toString())


                updateCreateButtonState()
            }
            constraintLayoutTribeImageContainer.setOnClickListener {
                viewModel.tribePicMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Open)
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
                        editTextTribeTags.setText(
                            selectedTags.joinToString {
                                it.name
                            }
                        )
                    } else {
                        editTextTribeTags.setText(getString(R.string.no_tags_selected))
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

            includeCreateTribeButton.layoutConstraintButtonCreateTribe.setOnClickListener {
                viewModel.createTribe()
            }
        }
    }

    private fun updateCreateButtonState() {
        requireActivity().let {
            binding.includeCreateTribeButton.apply {
                layoutConstraintButtonCreateTribe.background = if (viewModel.createTribeBuilder.hasRequiredFields) {
                    AppCompatResources.getDrawable(it, R.drawable.button_background_enabled)
                } else {
                    AppCompatResources.getDrawable(it, R.drawable.button_background_disabled)
                }
            }
        }
    }
    override suspend fun onViewStateFlowCollect(viewState: CreateTribeViewState) {
        when (viewState) {
            CreateTribeViewState.Idle -> {
                binding.includeCreateTribeButton.layoutConstraintButtonCreateTribe.visible
                binding.progressBarCreateTribe.gone
            }
            is CreateTribeViewState.TribeImageUpdated -> {
                imageLoader.load(
                    binding.imageViewTribePicture,
                    viewState.imageFile,
                    viewModel.imageLoaderDefaults
                )
                binding.editTextTribeImageValue.setText(getString(R.string.image_selected))
            }
            CreateTribeViewState.CreatingTribe -> {
                binding.progressBarCreateTribe.visible
                binding.includeCreateTribeButton.layoutConstraintButtonCreateTribe.gone
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
