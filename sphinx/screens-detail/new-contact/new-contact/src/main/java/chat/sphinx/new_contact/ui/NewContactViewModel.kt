package chat.sphinx.new_contact.ui

import chat.sphinx.new_contact.navigation.NewContactNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import javax.inject.Inject

@HiltViewModel
internal class NewContactViewModel @Inject constructor(
    val navigator: NewContactNavigator
): BaseViewModel<NewContactViewState>(NewContactViewState.Idle)
{
}
