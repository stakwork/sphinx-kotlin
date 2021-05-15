package chat.sphinx.new_contact.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.new_contact.R
import chat.sphinx.new_contact.databinding.FragmentNewContactBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_contact.ContactAlias
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_toast_utils.ToastUtils
import io.matthewnelson.android_feature_toast_utils.show

@AndroidEntryPoint
internal class NewContactFragment : SideEffectFragment<
        Context,
        NewContactSideEffect,
        NewContactViewState,
        NewContactViewModel,
        FragmentNewContactBinding
        >(R.layout.fragment_new_contact)
{

    companion object {
        const val PASTE_REGEX = "^${LightningNodePubKey.REGEX}:${LightningRouteHint.REGEX}\$"
    }

    override val viewModel: NewContactViewModel by viewModels()
    override val binding: FragmentNewContactBinding by viewBinding(FragmentNewContactBinding::bind)

    override suspend fun onViewStateFlowCollect(viewState: NewContactViewState) {
        @Exhaustive
        when (viewState) {
            is NewContactViewState.Saving -> {
                binding.newContactSaveProgress.visibility = View.VISIBLE
            }

            is NewContactViewState.Saved -> {
                binding.newContactSaveProgress.visibility = View.GONE

                viewModel.navigator.popBackStack()
                viewModel.navigator.popBackStack()
            }

            is NewContactViewState.Error -> {
                binding.newContactSaveProgress.visibility = View.GONE
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.layoutNewContactHeader.textViewNewContactHeaderNavBack.setOnClickListener {
            lifecycleScope.launch { viewModel.navigator.popBackStack() }
        }

        binding.layoutNewContactHeader.textViewNewContactClose.setOnClickListener {
            lifecycleScope.launch {
                viewModel.navigator.popBackStack()
                viewModel.navigator.popBackStack()
            }
        }


        binding.layoutGroupPinView.newContactPinQuestionMarkTextView.setOnClickListener {
            ToastUtils().show(
                binding.root.context,
                R.string.new_contact_privacy_setting_help
            )
        }

        binding.newContactAddressEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                binding.newContactAddressEditText.removeTextChangedListener(this)

                pastePubKey(s)

                binding.newContactAddressEditText.addTextChangedListener(this)
            }
        })

        binding.newContactRouteHintEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                binding.newContactRouteHintEditText.removeTextChangedListener(this)

                pastePubKey(s)

                binding.newContactRouteHintEditText.addTextChangedListener(this)
            }
        })

        binding.buttonAlreadyOnSphinx.setOnClickListener {
            viewModel.addContact(
                binding.newContactAddressEditText.text?.toString() ?: "",
                binding.newContactNicknameEditText.text?.toString() ?: "",
                binding.newContactRouteHintEditText.text?.toString()
            )
        }

        (requireActivity() as InsetterActivity).addNavigationBarPadding(binding.layoutConstraintNewContact)
    }

    private fun pastePubKey(s: Editable?) {
        if (!s.toString().trim().matches(PASTE_REGEX.toRegex())) {
            return
        }

        s?.let {
            val splitText = it.trim().split(":")
            binding.newContactAddressEditText.setText(splitText[0])
            binding.newContactRouteHintEditText.setText("${splitText[1]}:${splitText[2]}")
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: NewContactSideEffect) {
        sideEffect.execute(binding.root.context)
    }
}
