package chat.sphinx.contact.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavArgs
import androidx.viewbinding.ViewBinding
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.contact.R
import chat.sphinx.contact.databinding.LayoutContactBinding
import chat.sphinx.contact.databinding.LayoutContactDetailScreenHeaderBinding
import chat.sphinx.contact.databinding.LayoutContactSaveBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addKeyboardPadding
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.keyboard_inset_fragment.KeyboardInsetLayoutDetailFragment
import chat.sphinx.resources.getRandomHexCode
import chat.sphinx.resources.inputMethodManager
import chat.sphinx.resources.setBackgroundRandomColor
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import chat.sphinx.wrapper_common.lightning.getPubKey
import chat.sphinx.wrapper_common.lightning.getRouteHint
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_common.lightning.toVirtualLightningNodeAddress
import chat.sphinx.wrapper_common.util.getInitials
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import kotlinx.coroutines.launch

abstract class ContactFragment<
        VB: ViewBinding,
        ARGS: NavArgs,
        VM: ContactViewModel<ARGS>,
        >(@LayoutRes layoutId: Int) : KeyboardInsetLayoutDetailFragment<
        Context,
        ContactSideEffect,
        ContactViewState,
        VM,
        VB
        >(layoutId)
{
    abstract val headerBinding: LayoutContactDetailScreenHeaderBinding
    abstract val contactBinding: LayoutContactBinding
    abstract val contactSaveBinding: LayoutContactSaveBinding

    abstract val userColorsHelper: UserColorsHelper

    override suspend fun onViewStateFlowCollect(viewState: ContactViewState) {
        @Exhaustive
        when (viewState) {
            is ContactViewState.Idle -> {}

            is ContactViewState.Saving -> {
                contactSaveBinding.progressBarContactSave.visible
            }
            is ContactViewState.Saved -> {
                contactSaveBinding.progressBarContactSave.gone
                viewModel.navigator.closeDetailScreen()
            }
            is ContactViewState.Error -> {
                contactSaveBinding.progressBarContactSave.gone
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        headerBinding.apply {

            textViewDetailScreenHeaderName.text = getHeaderText()

            textViewDetailScreenHeaderNavBack.apply {
                
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

            textViewDetailScreenSubscribe.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }

        contactBinding.apply {
            contactSaveBinding.buttonSave.text = getSaveButtonText()
            layoutGroupPinView.newContactPinQuestionMarkTextView.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.submitSideEffect(ContactSideEffect.Notify.PrivacyPinHelp)
                }
            }

            scanAddressButton.setOnClickListener {
                viewModel.requestScanner()
            }

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

            addDoneKeyHandler(editTextContactNickname)
            addDoneKeyHandler(editTextContactAddress)
            addDoneKeyHandler(editTextContactRouteHint)

            contactSaveBinding.buttonSave.setOnClickListener {
                viewModel.saveContact(
                    editTextContactNickname.text?.toString(),
                    editTextContactAddress.text?.toString(),
                    editTextContactRouteHint.text?.toString()
                )
            }

            (requireActivity() as InsetterActivity).addNavigationBarPadding(layoutConstraintContact)
            (requireActivity() as InsetterActivity).addNavigationBarPadding(contactSaveBinding.layoutConstraintSaveContact)
        }

        viewModel.initContactDetails()
    }

    private fun addDoneKeyHandler(editText: AppCompatEditText) {
        editText.setOnEditorActionListener(object:
            TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                    editText.let { editText ->
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
    }

    override fun onKeyboardToggle() {
        (requireActivity() as InsetterActivity).addKeyboardPadding(contactBinding.layoutConstraintContact)
    }

    override fun closeDetailsScreen() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.navigator.closeDetailScreen()
        }
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
                headerBinding.apply {
                    textViewDetailScreenHeaderNavBack.gone

                    textViewDetailScreenSubscribe.visible

                    textViewDetailScreenSubscribe.text = if (sideEffect.subscribed) {
                        getString(R.string.edit_contact_header_subscribed_button)
                    } else {
                        getString(R.string.edit_contact_header_subscribe_button)
                    }

                    textViewDetailScreenSubscribe.backgroundTintList = if (sideEffect.subscribed) {
                        ContextCompat.getColorStateList(root.context, R.color.secondaryText)
                    } else {
                        ContextCompat.getColorStateList(root.context, R.color.primaryBlue)
                    }
                }

                contactBinding.apply {
                    editTextContactAddress.isEnabled = false

                    scanAddressButton.gone
                    buttonQrCode.visible

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

                    layoutConstraintExistingContactProfilePicture.visible

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
                        val key = sideEffect.routeHint?.let { routeHint ->
                            "${sideEffect.pubKey.value}:${routeHint.value}"
                        } ?: sideEffect.pubKey.value
                        viewModel.toQrCodeLightningNodePubKey(key)
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
