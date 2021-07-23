package chat.sphinx.contact_detail.ui

import chat.sphinx.contact_detail.navigation.ContactDetailNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class ContactDetailViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: ContactDetailNavigator,
): BaseViewModel<ContactDetailViewState>(dispatchers, ContactDetailViewState.Idle)
{
}
