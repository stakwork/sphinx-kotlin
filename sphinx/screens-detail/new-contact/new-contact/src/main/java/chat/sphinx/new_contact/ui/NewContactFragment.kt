package chat.sphinx.new_contact.ui

import android.opengl.Visibility
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ProgressBar
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.new_contact.R
import chat.sphinx.new_contact.databinding.FragmentNewContactBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import kotlinx.coroutines.launch
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_common.lightning.isValid
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_contact.ContactStatus
import io.matthewnelson.android_feature_toast_utils.ToastUtils
import io.matthewnelson.android_feature_toast_utils.show

@AndroidEntryPoint
internal class NewContactFragment : BaseFragment<
        NewContactViewState,
        NewContactViewModel,
        FragmentNewContactBinding
        >(R.layout.fragment_new_contact) {
    override val viewModel: NewContactViewModel by viewModels()
    override val binding: FragmentNewContactBinding by viewBinding(FragmentNewContactBinding::bind)

    override suspend fun onViewStateFlowCollect(viewState: NewContactViewState) {
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

        binding.newContactPinQuestionMarkTextView.setOnClickListener {
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
            var pubkey = binding.newContactAddressEditText.text.toString().trim()
            var nickname = binding.newContactNicknameEditText.text.toString().trim()
            var routeHint = binding.newContactRouteHintEditText.text.toString().trim()

            if (binding.newContactNicknameEditText.text.isNullOrBlank() || pubkey.isNullOrBlank()) {
                ToastUtils().show(
                    binding.root.context,
                    R.string.new_contact_nickname_address_empty
                )
                return@setOnClickListener
            }

            val lightningNodePubKey = LightningNodePubKey(pubkey)
            var lightningRouteHint: LightningRouteHint? = null

            if (!lightningNodePubKey.isValid) {
                ToastUtils().show(
                    binding.root.context,
                    R.string.new_contact_invalid_public_key_error
                )
                return@setOnClickListener
            }

            if (!routeHint.isNullOrBlank()) {
                lightningRouteHint = LightningRouteHint(routeHint)

                if (!lightningRouteHint.isValid) {
                    ToastUtils().show(
                        binding.root.context,
                        R.string.new_contact_invalid_public_key_error
                    )
                }
                return@setOnClickListener
            }

            viewModel.addContact(ContactAlias(nickname), lightningNodePubKey, lightningRouteHint)
        }

        (requireActivity() as InsetterActivity).addNavigationBarPadding(binding.layoutConstraintNewContact)
    }

    private fun pastePubKey(s: Editable?) {
        if (!s.toString().trim().matches("^[A-F0-9a-f]{66}:[A-F0-9a-f]{66}:[0-9]+\$".toRegex())) {
            return
        }

        s?.let {
            val splitText = it.trim().split(":")
            binding.newContactAddressEditText.setText(splitText[0])
            binding.newContactRouteHintEditText.setText("${splitText[1]}:${splitText[2]}")
        }
    }
}
