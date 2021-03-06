package chat.sphinx.address_book.ui

import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.address_book.R
import chat.sphinx.address_book.databinding.FragmentAddressBookBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class AddressBookFragment: BaseFragment<
        AddressBookViewState,
        AddressBookViewModel,
        FragmentAddressBookBinding
        >(R.layout.fragment_address_book)
{
    override val viewModel: AddressBookViewModel by viewModels()
    override val binding: FragmentAddressBookBinding by viewBinding(FragmentAddressBookBinding::bind)

    override suspend fun onViewStateFlowCollect(viewState: AddressBookViewState) {
//        TODO("Not yet implemented")
    }
}
