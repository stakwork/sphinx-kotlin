package chat.sphinx.contact.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.annotation.LayoutRes
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavArgs
import androidx.viewbinding.ViewBinding
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.contact.R
import chat.sphinx.contact.databinding.LayoutContactBinding
import chat.sphinx.detail_resources.databinding.LayoutDetailScreenHeaderBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.resources.getRandomHexCode
import chat.sphinx.resources.setBackgroundRandomColor
import chat.sphinx.wrapper_common.lightning.getPubKey
import chat.sphinx.wrapper_common.lightning.getRouteHint
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_common.lightning.toVirtualLightningNodeAddress
import chat.sphinx.wrapper_common.util.getInitials
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.goneIfTrue
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import kotlinx.coroutines.launch

abstract class ContactFragment<
        VB: ViewBinding,
        ARGS: NavArgs,
        VM: ContactViewModel<ARGS>,
        >(@LayoutRes layoutId: Int) : SideEffectFragment<
        Context,
        ContactSideEffect,
        ContactViewState,
        VM,
        VB
        >(layoutId)
{
    abstract val headerBinding: LayoutDetailScreenHeaderBinding
    abstract val contactBinding: LayoutContactBinding

    abstract val userColorsHelper: UserColorsHelper

    override suspend fun onViewStateFlowCollect(viewState: ContactViewState) {
        @Exhaustive
        when (viewState) {
            is ContactViewState.Idle -> {}

            is ContactViewState.Saving -> {
                contactBinding.progressBarContactSave.visible
            }
            is ContactViewState.Saved -> {
                contactBinding.progressBarContactSave.gone
                viewModel.navigator.closeDetailScreen()
            }
            is ContactViewState.Error -> {
                contactBinding.progressBarContactSave.gone
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        headerBinding.apply {

            textViewDetailScreenHeaderName.text = getHeaderText()

            textViewDetailScreenHeaderNavBack.apply {
                goneIfFalse(viewModel.isFromAddFriend())
                
                setOnClickListener {
                    lifecycleScope.launch(viewModel.mainImmediate) {
                        viewModel.navigator.popBackStack()
                    }
                }
            }

            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }

        contactBinding.apply {
            buttonSave.text = getSaveButtonText()
            layoutGroupPinView.newContactPinQuestionMarkTextView.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.submitSideEffect(ContactSideEffect.Notify.PrivacyPinHelp)
                }
            }

            editTextContactAddress.isEnabled = !viewModel.isExistingContact()

            scanAddressButton.goneIfTrue(viewModel.isExistingContact())
            scanAddressButton.setOnClickListener {
                viewModel.requestScanner()
            }

            buttonQrCode.goneIfFalse(viewModel.isExistingContact())

            layoutConstraintExistingContactProfilePicture.goneIfFalse(viewModel.isExistingContact())

            editTextContactAddress.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    editTextContactAddress.removeTextChangedListener(this)

                    pastePubKey(s)

                    editTextContactAddress.addTextChangedListener(this)
                }
            })

            editTextContactRouteHint.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    editTextContactRouteHint.removeTextChangedListener(this)

                    pastePubKey(s)

                    editTextContactRouteHint.addTextChangedListener(this)
                }
            })

            buttonSave.setOnClickListener {
                viewModel.contactFormBuilder.setContactAlias(editTextContactNickname.text.toString())
                viewModel.contactFormBuilder.setLightningNodePubKey(editTextContactAddress.text.toString())
                viewModel.contactFormBuilder.setLightningRouteHint(editTextContactRouteHint.text.toString())


                viewModel.saveContact()
            }

            (requireActivity() as InsetterActivity).addNavigationBarPadding(layoutConstraintContact)
        }

        viewModel.initContactDetails()
    }

    @SuppressLint("SetTextI18n")
    private fun pastePubKey(s: Editable?) {
        s?.toString()?.toLightningNodePubKey()?.let { nnPubKey ->
            contactBinding.editTextContactAddress.setText(nnPubKey.value)
        }
        s?.toString()?.toVirtualLightningNodeAddress()?.let { nnVirtualAddress ->
            contactBinding.editTextContactAddress.setText(nnVirtualAddress.getPubKey()?.value)
            contactBinding.editTextContactRouteHint.setText(nnVirtualAddress.getRouteHint()?.value ?: "")
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: ContactSideEffect) {
        when (sideEffect) {
            is ContactSideEffect.ContactInfo -> {
                contactBinding.apply {
                    editTextContactAddress.setText(sideEffect.pubKey.value)
                    editTextContactRouteHint.setText(sideEffect.routeHint?.value ?: "")
                }
            }

            is ContactSideEffect.ExistingContact -> {
                contactBinding.apply {
                    editTextContactNickname.setText(sideEffect.nickname)
                    editTextContactAddress.setText(sideEffect.pubKey.value)
                    editTextContactRouteHint.setText(sideEffect.routeHint?.value ?: "")

                    sideEffect.colorKey?.let { colorKey ->
                        textViewInitials.apply {
                            visible
                            text = sideEffect.nickname?.getInitials() ?: ""
                            setBackgroundRandomColor(
                                R.drawable.chat_initials_circle,
                                Color.parseColor(
                                    userColorsHelper.getHexCodeForKey(
                                        colorKey,
                                        root.context.getRandomHexCode(),
                                    )
                                ),
                            )
                        }
                    }

                    sideEffect.photoUrl?.let {
                        viewModel.imageLoader.load(
                            imageViewProfilePicture,
                            sideEffect.photoUrl.value,
                            ImageLoaderOptions.Builder()
                                .placeholderResId(R.drawable.ic_profile_avatar_circle)
                                .transformation(Transformation.CircleCrop)
                                .build()
                        )
                    }

                    buttonQrCode.setOnClickListener {
                        viewModel.toQrCodeLightningNodePubKey(sideEffect.pubKey.value)
                    }
                }
            }
            else -> {
                sideEffect.execute(binding.root.context)
            }
        }
    }

    abstract fun getHeaderText(): String

    abstract fun getSaveButtonText(): String
}
