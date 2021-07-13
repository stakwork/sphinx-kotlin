package chat.sphinx.profile.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
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
import chat.sphinx.menu_bottom.model.MenuBottomOption
import chat.sphinx.menu_bottom.ui.BottomMenu
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.profile.R
import chat.sphinx.profile.databinding.FragmentProfileBinding
import chat.sphinx.profile.navigation.ProfileNavigator
import chat.sphinx.resources.getColor
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_contact.isTrue
import chat.sphinx.wrapper_contact.toPrivatePhoto
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.updateViewState
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

    private val bottomMenu: BottomMenu by lazy(LazyThreadSafetyMode.NONE) {
        BottomMenu(
            viewModel.dispatchers,
            onStopSupervisor,
            viewModel.profileMenuViewStateContainer,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        BackPressHandler(viewLifecycleOwner, requireActivity())

        setupProfileHeader()
        setupProfileTabs()
        setupProfile()

        bottomMenu.newBuilder(binding.includeLayoutMenuBottomProfilePic, viewLifecycleOwner)
            .setHeaderText(R.string.profile_menu_header_text)
            .setOptions(
                setOf(
                    MenuBottomOption(
                        text = R.string.profile_menu_option_camera,
                        textColor = R.color.primaryBlueFontColor,
                        onClick = {
                            viewModel.menuBottomProfilePicCamera()
                        }
                    ),
                    MenuBottomOption(
                        text = R.string.profile_menu_option_photo_library,
                        textColor = R.color.primaryBlueFontColor,
                        onClick = {
                            viewModel.bottomMenuProfilePicPhotoLibrary()
                        }
                    )
                )
            )
            .build()
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
            if (viewModel.profileMenuViewStateContainer.value is MenuBottomViewState.Open) {
                viewModel.profileMenuViewStateContainer.updateViewState(MenuBottomViewState.Closed)
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
                viewModel.profileMenuViewStateContainer.updateViewState(MenuBottomViewState.Open)
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

    override suspend fun onSideEffectCollect(sideEffect: ProfileSideEffect) {
        sideEffect.execute(requireActivity())
    }
}
