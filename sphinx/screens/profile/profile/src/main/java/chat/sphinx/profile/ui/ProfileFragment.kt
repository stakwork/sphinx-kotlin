package chat.sphinx.profile.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.profile.R
import chat.sphinx.profile.databinding.FragmentProfileBinding
import chat.sphinx.profile.navigation.ProfileNavigator
import chat.sphinx.resources.getColor
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_contact.isTrue
import chat.sphinx.wrapper_contact.toPrivatePhoto
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.updateViewState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class ProfileFragment: BaseFragment<
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupProfileHeader()
        setupProfileTabs()
        setupProfile()
    }

    private fun setupProfileHeader() {
        val activity = (requireActivity() as InsetterActivity)

        binding.apply {
            activity.addStatusBarPadding(layoutProfileHeader.root)
                .addNavigationBarPadding(layoutProfileBasicContainerHolder.layoutScrollViewContent)
                .addNavigationBarPadding(layoutProfileAdvancedContainerHolder.layoutScrollViewContent)

            layoutProfileHeader.apply {
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
        binding.layoutProfileTabsHolder.basicTabButton.setOnClickListener {
            viewModel.updateViewState(ProfileViewState.Basic)
        }

        binding.layoutProfileTabsHolder.advancedTabButton.setOnClickListener {
            viewModel.updateViewState(ProfileViewState.Advanced)
        }
    }

    private fun setupProfile() {
        binding.apply {
            layoutProfileBasicContainerHolder.apply {

                profileUserNameEditText.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        return@setOnFocusChangeListener
                    }

                    updateOwnerDetails()
                }

                profileTipEditText.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        return@setOnFocusChangeListener
                    }

                    updateOwnerDetails()
                }

                pinSwitch.setOnCheckedChangeListener { _, _ ->
                    updateOwnerDetails()
                }

                qrCodeButton.setOnClickListener {
                    viewModel.accountOwnerStateFlow.value?.nodePubKey?.let { pubKey ->
                        lifecycleScope.launch(viewModel.mainImmediate) {
                            profileNavigator.toQRCodeDetail(pubKey.value)
                        }
                    }
                }
            }

            layoutProfileAdvancedContainerHolder.seekBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    viewModel.updatePINTimeOutStateFlow(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // only persist when tracking is stopped (key up)
                    viewModel.persistPINTimeout()
                }
            })
        }
    }

    override fun onStart() {
        super.onStart()
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.getAccountBalance().collect { nodeBalance ->
                if (nodeBalance == null) return@collect

                nodeBalance.balance.asFormattedString().let { balance ->
                    binding.layoutProfileBasicInfoHolder.profileTextViewSatBalance.text = balance
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.relayUrlStateFlow.collect { relayUrl ->
                binding.layoutProfileAdvancedContainerHolder.profileServerUrlEditText.setText(
                    relayUrl
                )
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.pinTimeoutStateFlow.collect { pinTimeout ->
                pinTimeout?.let {
                    binding.layoutProfileAdvancedContainerHolder.seekBar.progress = it

                    setPINTimeoutString(it)
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.accountOwnerStateFlow.collect { contactOwner ->
                contactOwner?.let { owner ->
                    binding.apply {
                        layoutProfileBasicInfoHolder.apply {
                            owner.photoUrl?.value?.let { url ->
                                imageLoader.load(
                                    profileImageViewPicture,
                                    url,
                                    ImageLoaderOptions.Builder()
                                        .placeholderResId(R.drawable.ic_profile_avatar_circle)
                                        .transformation(Transformation.CircleCrop)
                                        .build()
                                )
                            } ?: profileImageViewPicture.setImageDrawable(
                                ContextCompat.getDrawable(
                                    binding.root.context,
                                    R.drawable.ic_profile_avatar_circle
                                )
                            )

                            profileTextViewName.text = owner.alias?.value ?: ""
                        }

                        layoutProfileBasicContainerHolder.apply {
                            profileUserNameEditText.setText(owner.alias?.value ?: "")
                            profileAddressEditText.setText(owner.nodePubKey?.value ?: "")
                            profileRouteHintEditText.setText(owner.routeHint?.value ?: "")
                            profileTipEditText.setText("${owner.tipAmount?.value ?: "100"}")
                            pinSwitch.isChecked = !owner.privatePhoto.value.toPrivatePhoto().isTrue()
                        }
                    }
                }
            }
        }
    }

    private fun setPINTimeoutString(progress: Int) {
        if (progress == 0) {
            binding.layoutProfileAdvancedContainerHolder.profilePinTimeoutValueTextView.text = "Always require PIN"
        } else {
            binding.layoutProfileAdvancedContainerHolder.profilePinTimeoutValueTextView.text = "${progress} hours"
        }
    }

    private fun updateOwnerDetails() {
        val alias = binding.layoutProfileBasicContainerHolder.profileUserNameEditText.text.toString()
        val tipAmount = binding.layoutProfileBasicContainerHolder.profileTipEditText.text.toString().toLong()
        val privatePhoto = !binding.layoutProfileBasicContainerHolder.pinSwitch.isChecked

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.updateOwner(
                alias,
                privatePhoto.toPrivatePhoto(),
                Sat(value = tipAmount)
            )
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: ProfileViewState) {
        binding.apply {

            @Exhaustive
            when (viewState) {
                is ProfileViewState.Advanced -> {
                    layoutProfileTabsHolder.apply {
                        basicTabButton.setBackgroundColor(getColor(R.color.body))
                        advancedTabButton.setBackgroundColor(getColor(R.color.primaryBlue))
                    }
                    layoutProfileBasicContainerHolder.root.gone
                    layoutProfileAdvancedContainerHolder.root.visible
                }
                is ProfileViewState.Basic -> {
                    layoutProfileTabsHolder.apply {
                        basicTabButton.setBackgroundColor(getColor(R.color.primaryBlue))
                        advancedTabButton.setBackgroundColor(getColor(R.color.body))
                    }
                    layoutProfileBasicContainerHolder.root.visible
                    layoutProfileAdvancedContainerHolder.root.gone
                }
            }

        }
    }
}
