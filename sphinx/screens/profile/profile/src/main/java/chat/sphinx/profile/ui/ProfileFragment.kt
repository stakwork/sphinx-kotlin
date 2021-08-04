package chat.sphinx.profile.ui

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.menu_bottom_profile_pic.BottomMenuPicture
import chat.sphinx.menu_bottom_profile_pic.UpdatingImageViewState
import chat.sphinx.profile.R
import chat.sphinx.profile.databinding.FragmentProfileBinding
import chat.sphinx.profile.navigation.ProfileNavigator
import chat.sphinx.resources.getColor
import chat.sphinx.resources.inputMethodManager
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_contact.isTrue
import chat.sphinx.wrapper_contact.toPrivatePhoto
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class ProfileFragment: SideEffectFragment<
        Context,
        ProfileSideEffect,
        ProfileViewState,
        ProfileViewModel,
        FragmentProfileBinding
        >(R.layout.fragment_profile)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val viewModel: ProfileViewModel by viewModels()
    override val binding: FragmentProfileBinding by viewBinding(FragmentProfileBinding::bind)

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var profileNavigator: ProfileNavigator

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

        setupProfileHeader()
        setupProfileTabs()
        setupProfile()

        bottomMenuPicture.initialize(
            R.string.bottom_menu_profile_pic_header_text,
            binding.includeLayoutMenuBottomProfilePic,
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
            if (viewModel.pictureMenuHandler.viewStateContainer.value is MenuBottomViewState.Open) {
                viewModel.pictureMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Closed)
            } else {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    profileNavigator.popBackStack()
                }
            }
        }
    }

    private fun setupProfileHeader() {
        val activity = (requireActivity() as InsetterActivity)

        binding.apply {
            activity.addStatusBarPadding(includeProfileHeader.root)
                .addNavigationBarPadding(
                    includeProfileBasicContainerHolder
                        .layoutConstraintProfileAdvancedContainerScrollViewContent
                )
                .addNavigationBarPadding(
                    includeProfileAdvancedContainerHolder
                        .layoutConstraintProfileAdvancedContainerScrollViewContent
                )
                .addNavigationBarPadding(
                    includeLayoutMenuBottomProfilePic.includeLayoutMenuBottomOptions.root
                )

            includeProfileNamePictureHolder.imageViewProfilePicture.setOnClickListener {
                viewModel.pictureMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Open)
            }

            includeProfileHeader.apply {
                root.layoutParams.height = root.layoutParams.height + activity.statusBarInsetHeight.top
                root.requestLayout()

                textViewProfileHeaderNavBack.setOnClickListener {
                    lifecycleScope.launch(viewModel.mainImmediate) {
                        profileNavigator.popBackStack()
                    }
                }
            }
        }
    }

    private fun setupProfileTabs() {
        binding.includeProfileTabsHolder.apply {
            buttonProfileTabBasic.setOnClickListener {
                viewModel.updateViewState(ProfileViewState.Basic)
            }
            buttonProfileTabAdvanced.setOnClickListener {
                viewModel.updateViewState(ProfileViewState.Advanced)
            }
        }
    }

    private fun setupProfile() {
        binding.apply {
            includeProfileBasicContainerHolder.apply {

                editTextProfileBasicContainerUserName.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        return@setOnFocusChangeListener
                    }
                    updateOwnerDetails()
                }

                editTextProfileBasicContainerTip.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        return@setOnFocusChangeListener
                    }
                    updateOwnerDetails()
                }

                switchProfileBasicContainerPin.setOnCheckedChangeListener { _, _ ->
                    updateOwnerDetails()
                }

                buttonProfileBasicContainerQrCode.setOnClickListener {
                    viewModel.accountOwnerStateFlow.value?.let { owner ->
                        lifecycleScope.launch(viewModel.mainImmediate) {
                            owner.nodePubKey?.let { pubKey ->
                                val key = owner.routeHint?.let { routeHint ->
                                    "${pubKey.value}:${routeHint.value}"
                                } ?: pubKey.value
                                
                                profileNavigator.toQRCodeDetail(key, getString(R.string.profile_qr_code_header_name))
                            }
                        }
                    }
                }

                buttonProfileBasicContainerKeyBackup.setOnClickListener {
                    viewModel.backupKeys()
                }
            }

            includeProfileAdvancedContainerHolder.apply {
                editTextProfileAdvancedContainerServerUrl.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        return@setOnFocusChangeListener
                    }
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        viewModel.updateRelayUrl(
                            editTextProfileAdvancedContainerServerUrl.text?.toString()
                        )
                    }
                }

                editTextProfileAdvancedContainerServerUrl.setOnEditorActionListener(object:
                    TextView.OnEditorActionListener {
                    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
                        if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                            editTextProfileAdvancedContainerServerUrl.let { editText ->
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

                seekBarProfileAdvancedContainerPinTimeout.setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            viewModel.updatePINTimeOutStateFlow(progress)
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            // only persist when tracking is stopped (key up)
                            viewModel.persistPINTimeout()
                        }
                    }
                )

                buttonProfileAdvancedContainerChangePin.setOnClickListener {
                    viewModel.resetPIN()
                }

            }
        }
    }

    override fun onStart() {
        super.onStart()
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.getAccountBalance().collect { nodeBalance ->
                if (nodeBalance == null) return@collect

                nodeBalance.balance.asFormattedString().let { balance ->
                    binding.includeProfileNamePictureHolder.textViewProfileSatBalance.text = balance
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.relayUrlStateFlow.collect { relayUrl ->
                binding
                    .includeProfileAdvancedContainerHolder
                    .editTextProfileAdvancedContainerServerUrl
                    .setText(relayUrl)
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.pinTimeoutStateFlow.collect { pinTimeout ->
                pinTimeout?.let { nnTimeout ->
                    binding.includeProfileAdvancedContainerHolder.apply {
                        seekBarProfileAdvancedContainerPinTimeout.progress = nnTimeout
                        textViewProfileAdvancedContainerPinTimeoutValue.text =
                            if (nnTimeout == 0) {
                                getString(R.string.profile_always_require_pin)
                            } else {
                                "$nnTimeout hours"
                            }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.accountOwnerStateFlow.collect { contactOwner ->
                contactOwner?.let { owner ->
                    binding.apply {
                        includeProfileNamePictureHolder.apply {
                            owner.photoUrl?.value?.let { url ->
                                imageLoader.load(
                                    imageViewProfilePicture,
                                    url,
                                    ImageLoaderOptions.Builder()
                                        .placeholderResId(R.drawable.ic_profile_avatar_circle)
                                        .transformation(Transformation.CircleCrop)
                                        .build()
                                )
                            } ?: imageViewProfilePicture.setImageDrawable(
                                ContextCompat.getDrawable(
                                    binding.root.context,
                                    R.drawable.ic_profile_avatar_circle
                                )
                            )

                            textViewProfileName.text = owner.alias?.value ?: ""
                        }

                        includeProfileBasicContainerHolder.apply {
                            editTextProfileBasicContainerUserName.setText(owner.alias?.value ?: "")
                            editTextProfileBasicContainerAddress.setText(owner.nodePubKey?.value ?: "")
                            editTextProfileBasicContainerRouteHint.setText(owner.routeHint?.value ?: "")
                            editTextProfileBasicContainerTip.setText("${owner.tipAmount?.value ?: "100"}")
                            switchProfileBasicContainerPin.isChecked = !owner.privatePhoto.value.toPrivatePhoto().isTrue()
                        }
                    }
                }
            }
        }
    }

    private fun updateOwnerDetails() {
        binding.includeProfileBasicContainerHolder.let {
            try {
                val alias = it.editTextProfileBasicContainerUserName.text?.toString()
                val privatePhoto = !it.switchProfileBasicContainerPin.isChecked
                val tipAmount = it.editTextProfileBasicContainerTip.text?.toString()?.toLong()

                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                    viewModel.updateOwner(
                        alias,
                        privatePhoto.toPrivatePhoto(),
                        tipAmount?.toSat()
                    )
                }
            } catch (e: NumberFormatException) {}
        }

    }

    override suspend fun onViewStateFlowCollect(viewState: ProfileViewState) {
        binding.apply {

            @Exhaustive
            when (viewState) {
                is ProfileViewState.Advanced -> {
                    includeProfileTabsHolder.apply {
                        buttonProfileTabBasic.setBackgroundColor(getColor(R.color.body))
                        buttonProfileTabAdvanced.setBackgroundColor(getColor(R.color.primaryBlue))
                    }
                    includeProfileBasicContainerHolder.root.gone
                    includeProfileAdvancedContainerHolder.root.visible
                }
                is ProfileViewState.Basic -> {
                    includeProfileTabsHolder.apply {
                        buttonProfileTabBasic.setBackgroundColor(getColor(R.color.primaryBlue))
                        buttonProfileTabAdvanced.setBackgroundColor(getColor(R.color.body))
                    }
                    includeProfileBasicContainerHolder.root.visible
                    includeProfileAdvancedContainerHolder.root.gone
                }
            }

        }
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.updatingImageViewStateContainer.collect { viewState ->
                binding.includeProfileNamePictureHolder.apply {
                    @Exhaustive
                    when (viewState) {
                        is UpdatingImageViewState.Idle -> {}
                        is UpdatingImageViewState.UpdatingImage -> {
                            layoutConstraintUploadingPicture.visible

                            imageViewProfilePicture.setImageDrawable(
                                ContextCompat.getDrawable(
                                    binding.root.context,
                                    R.drawable.ic_profile_avatar_circle
                                )
                            )
                        }
                        is UpdatingImageViewState.UpdatingImageFailed -> {
                            layoutConstraintUploadingPicture.gone

                            viewModel.submitSideEffect(
                                ProfileSideEffect.FailedToProcessImage
                            )
                        }
                        is UpdatingImageViewState.UpdatingImageSucceed -> {
                            layoutConstraintUploadingPicture.gone

                            viewModel.submitSideEffect(
                                ProfileSideEffect.ImageUpdatedSuccessfully
                            )
                        }
                    }
                }
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: ProfileSideEffect) {
        sideEffect.execute(requireActivity())
    }
}
