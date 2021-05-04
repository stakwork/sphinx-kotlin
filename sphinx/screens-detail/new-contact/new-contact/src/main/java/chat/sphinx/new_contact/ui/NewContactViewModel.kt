package chat.sphinx.new_contact.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import javax.inject.Inject

@HiltViewModel
internal class NewContactViewModel @Inject constructor(

): BaseViewModel<NewContactViewState>(NewContactViewState.Idle)
{
}
