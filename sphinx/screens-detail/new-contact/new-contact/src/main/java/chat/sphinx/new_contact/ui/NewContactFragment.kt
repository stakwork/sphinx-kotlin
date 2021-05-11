package chat.sphinx.new_contact.ui

import android.os.Bundle
import android.view.View
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
//        TODO("Not yet implemented")
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

        binding.newContactPinLabelField.setOnClickListener {
            ToastUtils().show(
                binding.root.context,
                R.string.new_contact_privacy_setting_help
            )
        }

        binding.buttonAlreadyOnSphinx.setOnClickListener {
            var pubkey = binding.newContactAddressField.text.toString().trim()
            var nickname = binding.newContactNicknameField.text.toString().trim()
            var routeHint = binding.newContactRouteHintField.text.toString().trim()

            if (binding.newContactNicknameField.text.isNullOrBlank() || pubkey.isNullOrBlank()) {
                ToastUtils().show(
                    binding.root.context,
                    R.string.new_contact_nickname_address_empty
                )
                return@setOnClickListener
            }

            val lightningNodePubKey = LightningNodePubKey(pubkey)
            val lightningRouteHint = LightningRouteHint(routeHint)

            if (!lightningNodePubKey.isValid) {
                ToastUtils().show(
                    binding.root.context,
                    R.string.new_contact_invalid_pubkey_error
                )
                return@setOnClickListener
            }

            if (!routeHint.isNullOrBlank() && !lightningRouteHint.isValid) {
                ToastUtils().show(
                    binding.root.context,
                    R.string.new_contact_invalid_pubkey_error
                )
                return@setOnClickListener
            }

            ToastUtils().show(binding.root.context,  "Adding Contact")

            viewModel.addContact(ContactAlias(nickname), lightningNodePubKey, lightningRouteHint)
        }

        (requireActivity() as InsetterActivity).addNavigationBarPadding(binding.layoutConstraintNewContact)
    }

}
