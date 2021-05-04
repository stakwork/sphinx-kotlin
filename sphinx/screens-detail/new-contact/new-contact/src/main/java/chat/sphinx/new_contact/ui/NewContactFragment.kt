package chat.sphinx.new_contact.ui

import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.new_contact.R
import chat.sphinx.new_contact.databinding.FragmentNewContactBinding
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

internal class NewContactFragment: BaseFragment<
        NewContactViewState,
        NewContactViewModel,
        FragmentNewContactBinding
        >(R.layout.fragment_new_contact)
{
    override val viewModel: NewContactViewModel by viewModels()
    override val binding: FragmentNewContactBinding by viewBinding(FragmentNewContactBinding::bind)

    override suspend fun onViewStateFlowCollect(viewState: NewContactViewState) {
//        TODO("Not yet implemented")
    }
}
