package chat.sphinx.address_book.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import javax.inject.Inject

@HiltViewModel
internal class AddressBookViewModel @Inject constructor(

): BaseViewModel<AddressBookViewState>(AddressBookViewState.Idle)
{
}