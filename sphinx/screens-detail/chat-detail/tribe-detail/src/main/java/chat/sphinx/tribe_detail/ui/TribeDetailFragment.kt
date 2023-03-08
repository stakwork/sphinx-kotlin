package chat.sphinx.tribe_detail.ui

import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.menu_bottom_profile_pic.BottomMenuPicture
import chat.sphinx.menu_bottom_profile_pic.UpdatingImageViewState
import chat.sphinx.resources.inputMethodManager
import chat.sphinx.tribe.BottomMenuTribe
import chat.sphinx.tribe_detail.R
import chat.sphinx.tribe_detail.databinding.FragmentTribeDetailBinding
import chat.sphinx.wrapper_chat.fixedAlias
import chat.sphinx.wrapper_chat.isTribeOwnedByAccount
import chat.sphinx.wrapper_common.eeemmddhmma
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject


@AndroidEntryPoint
internal class TribeDetailFragment: SideEffectFragment<
        Context,
        TribeDetailSideEffect,
        TribeDetailViewState,
        TribeDetailViewModel,
        FragmentTribeDetailBinding
        >(R.layout.fragment_tribe_detail)
{
    override val viewModel: TribeDetailViewModel by viewModels()
    override val binding: FragmentTribeDetailBinding by viewBinding(FragmentTribeDetailBinding::bind)

    @Inject
    lateinit var imageLoaderInj: ImageLoader<ImageView>

    private val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj

    private val bottomMenuTribe: BottomMenuTribe by lazy(LazyThreadSafetyMode.NONE) {
        BottomMenuTribe(
            onStopSupervisor,
            viewModel
        )
    }

    private val bottomMenuPicture: BottomMenuPicture by lazy(LazyThreadSafetyMode.NONE) {
        BottomMenuPicture(
            this,
            onStopSupervisor,
            viewModel
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        BackPressHandler(viewLifecycleOwner, requireActivity())

        binding.includeTribeDetailHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.tribe_detail_header_name)
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }

        setupFragmentLayout()
        setupTribeDetail()

        bottomMenuPicture.initialize(
            R.string.bottom_menu_tribe_profile_pic_header_text,
            binding.includeLayoutMenuBottomTribeProfilePic,
            viewLifecycleOwner
        )
    }

    private inner class BackPressHandler(
        owner: LifecycleOwner,
        activity: FragmentActivity,
    ): OnBackPressedCallback(true) {

        init {
            activity.apply {
                onBackPressedDispatcher.addCallback(
                    owner,
                    this@BackPressHandler,
                )
            }
        }

        override fun handleOnBackPressed() {
            when {
                viewModel.pictureMenuHandler.viewStateContainer.value is MenuBottomViewState.Open -> {
                    viewModel.pictureMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Closed)
                }
                viewModel.tribeMenuHandler.viewStateContainer.value is MenuBottomViewState.Open -> {
                    viewModel.tribeMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Closed)
                }
                else -> {
                    lifecycleScope.launch(viewModel.mainImmediate) {
                        viewModel.navigator.closeDetailScreen()
                    }
                }
            }
        }
    }

    private fun setupFragmentLayout() {
        val insetterActivity = requireActivity() as InsetterActivity

        insetterActivity.addNavigationBarPadding(
            binding.layoutConstraintTribeDetailLayout
        )
        insetterActivity.addNavigationBarPadding(
            binding.layoutConstraintAdminViewTribeMembers
        )
        insetterActivity.addNavigationBarPadding(
            binding.includeLayoutMenuBottomTribe.root
        )
        insetterActivity.addNavigationBarPadding(
            binding.includeLayoutMenuBottomTribeProfilePic.root
        )
    }

    private fun addAliasFilter() {
        val filter: InputFilter = object : InputFilter {
            override fun filter(
                source: CharSequence, start: Int,
                end: Int, dest: Spanned?, dstart: Int, dend: Int
            ): CharSequence? {
                for (i in start until end) {
                    if (Character.isSpace(source[i])) {
                        allowedCharactersToast()
                        return "_"
                    }
                    if (!Character.isLetterOrDigit(source[i]) &&
                        source[i].toString() != "_"
                    ) {
                        allowedCharactersToast()
                        return ""
                    }
                }
                return null
            }
        }

        binding.editTextProfileAliasValue.filters = arrayOf(filter)
    }

    private fun allowedCharactersToast() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.submitSideEffect(
                TribeDetailSideEffect.AliasAllowedCharacters
            )
        }
    }

    private fun setupTribeDetail() {
        binding.apply {
            editTextProfileAliasValue.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    editTextProfileAliasValue.setText(editTextProfileAliasValue.text.toString().fixedAlias())
                    addAliasFilter()
                    return@setOnFocusChangeListener
                }
                viewModel.updateProfileAlias(editTextProfileAliasValue.text.toString())
            }

            editTextProfileAliasValue.setOnEditorActionListener(object: OnEditorActionListener {
                override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
                    if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                        editTextProfileAliasValue.let { editText ->
                            binding.root.context.inputMethodManager?.let { imm ->
                                if (imm.isActive(editText)) {
                                    imm.hideSoftInputFromWindow(editText.windowToken, 0)
                                    editText.clearFocus()
                                }
                            }
                        }
                        return true
                    }
                    return false
                }
            })

            textViewMenuButton.setOnClickListener {
                viewModel.tribeMenuHandler.viewStateContainer.updateViewState(
                    MenuBottomViewState.Open
                )
            }

            buttonProfilePicture.setOnClickListener {
                viewModel.pictureMenuHandler.viewStateContainer.updateViewState(
                    MenuBottomViewState.Open
                )
            }

            buttonAdminViewMembers.setOnClickListener {
                viewModel.toTribeMemberList()
            }

            layoutConstraintTribeBadges.setOnClickListener {
                viewModel.goToTribeBadgesScreen()
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: TribeDetailViewState) {
        @Exhaustive
        when(viewState) {
            is TribeDetailViewState.Idle -> { }

            is TribeDetailViewState.TribeProfile -> {
                binding.apply {
                    // Hide the upload picture progress indicator
                    progressBarUploadProfilePicture.gone
                    // Initialize the Tribe Specific menu bar with the
                    bottomMenuTribe.initialize(
                        viewState.chat,
                        viewState.accountOwner,
                        includeLayoutMenuBottomTribe,
                        viewLifecycleOwner
                    )
                    textViewMenuButton.visible

                    textViewTribeName.text = viewState.chat.name?.value
                    textViewTribeCreateDate.text = getString(
                        R.string.tribe_created_on,
                        viewState.chat.createdAt.eeemmddhmma()
                    )
                    textViewTribeConfigurations.text = getString(
                        R.string.tribe_costs,
                        viewState.chat.pricePerMessage?.value ?: 0L,
                        viewState.chat.escrowAmount?.value ?: 0L
                    )

                    val userAlias = viewState.chat.myAlias?.value ?: viewState.accountOwner.alias?.value
                    editTextProfileAliasValue.setText(userAlias)

                    viewState.chat.photoUrl?.let {
                        imageLoader.load(
                            imageViewTribePicture,
                            it.value,
                            viewModel.imageLoaderDefaults,
                        )
                    }

                    val userPhotoUrl = viewState.chat.myPhotoUrl ?: viewState.accountOwner.photoUrl
                    userPhotoUrl?.let {
                        editTextProfilePictureValue.setText(it.value)

                        imageLoader.load(
                            imageViewProfilePicture,
                            it.value,
                            viewModel.imageLoaderDefaults,
                        )
                    }

                    if (viewState.chat.isTribeOwnedByAccount(viewModel.getOwner().nodePubKey)) {
                        buttonAdminViewMembers.visible
                        layoutConstraintTribeBadges.visible
                    } else {
                        buttonAdminViewMembers.gone
                        layoutConstraintTribeBadges.gone
                    }
                }
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.updatingImageViewStateContainer.collect { viewState ->
                binding.apply {
                    @Exhaustive
                    when (viewState) {
                        is UpdatingImageViewState.Idle -> {}
                        is UpdatingImageViewState.UpdatingImage -> {
                            progressBarUploadProfilePicture.visible

                            imageViewProfilePicture.setImageDrawable(
                                ContextCompat.getDrawable(
                                    binding.root.context,
                                    R.drawable.ic_profile_avatar_circle
                                )
                            )
                        }
                        is UpdatingImageViewState.UpdatingImageFailed -> {
                            progressBarUploadProfilePicture.gone
                        }
                        is UpdatingImageViewState.UpdatingImageSucceed -> {
                            progressBarUploadProfilePicture.visible
                        }
                    }
                }
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: TribeDetailSideEffect) {
        sideEffect.execute(requireActivity())
    }

    private fun List<Int>.closestValue(value: Int) = minByOrNull {
        kotlin.math.abs(value - it)
    }
}
