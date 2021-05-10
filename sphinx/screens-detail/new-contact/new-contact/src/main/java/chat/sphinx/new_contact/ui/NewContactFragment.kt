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

//        (requireActivity() as InsetterActivity).addNavigationBarPadding(binding.layoutConstraintSaveContent)
    }
}
