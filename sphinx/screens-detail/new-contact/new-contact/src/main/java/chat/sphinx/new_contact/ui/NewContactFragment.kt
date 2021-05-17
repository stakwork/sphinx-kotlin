package chat.sphinx.new_contact.ui

import android.annotation.SuppressLint
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
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.submitSideEffect

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
            is NewContactViewState.Idle -> {}
            is NewContactViewState.Saving -> {
                binding.newContactSaveProgress.visible
            }
            is NewContactViewState.Saved -> {
                binding.newContactSaveProgress.gone
                viewModel.navigator.closeDetailScreen()
            }
            is NewContactViewState.Error -> {
                binding.newContactSaveProgress.gone
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.includeNewContactHeader.apply {

            textViewDetailScreenHeaderName.text = getString(R.string.new_contact_header_name)

            textViewDetailScreenHeaderNavBack.apply {
                visible
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

        binding.layoutGroupPinView.newContactPinQuestionMarkTextView.setOnClickListener {
            lifecycleScope.launch(viewModel.mainImmediate) {
                viewModel.submitSideEffect(NewContactSideEffect.Notify.PrivacyPinHelp)
            }
        }

        binding.scanAddressButton.setOnClickListener {
            viewModel.requestScanner()
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
                binding.newContactNicknameEditText.text?.toString() ?: "",
                binding.newContactAddressEditText.text?.toString() ?: "",
                binding.newContactRouteHintEditText.text?.toString()
            )
        }

        (requireActivity() as InsetterActivity).addNavigationBarPadding(binding.layoutConstraintNewContact)
    }

    @SuppressLint("SetTextI18n")
    private fun pastePubKey(s: Editable?) {
        if (!s.toString().trim().matches(PASTE_REGEX.toRegex())) {
            return
        }

        s?.let {
            val splitText = it.trim().split(":")
            if (splitText.size == 3) {
                binding.newContactAddressEditText.setText(splitText[0])
                binding.newContactRouteHintEditText.setText("${splitText[1]}:${splitText[2]}")
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: NewContactSideEffect) {
        if (sideEffect is NewContactSideEffect.FromScanner) {
            // TODO: Check if it contains a route hint
            binding.newContactAddressEditText.setText(sideEffect.value.value)
        } else {
            sideEffect.execute(binding.root.context)
        }
    }
}
