package chat.sphinx.new_contact.ui

import android.util.Log
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.new_contact.navigation.NewContactNavigator
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_contact.ContactAlias
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@HiltViewModel
internal class NewContactViewModel @Inject constructor(
    val navigator: NewContactNavigator,
    dispatchers: CoroutineDispatchers,
    private val contactRepository: ContactRepository,
): BaseViewModel<NewContactViewState>(dispatchers, NewContactViewState.Idle)
{
    fun addContact(
        contactAlias: ContactAlias,
        lightningNodePubKey: LightningNodePubKey,
        lightningRouteHint: LightningRouteHint?,
    ) {
        viewModelScope.launch(mainImmediate) {
            contactRepository.createContact(
                contactAlias,
                lightningNodePubKey,
                lightningRouteHint
            ).collect { loadResponse ->
                @Exhaustive
                when(loadResponse) {
                    LoadResponse.Loading ->
                        viewStateContainer.updateViewState(NewContactViewState.Saving)
                    is Response.Error ->
                        viewStateContainer.updateViewState(NewContactViewState.Error)
                    is Response.Success ->
                        viewStateContainer.updateViewState(NewContactViewState.Saved)
                }
            }
        }
    }
}
