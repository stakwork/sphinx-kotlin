package chat.sphinx.new_contact.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.new_contact.navigation.NewContactNavigator
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_common.lightning.toLightningRouteHint
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_contact.toContactAlias
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
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
): SideEffectViewModel<
        Context,
        NewContactSideEffect,
        NewContactViewState
        >(dispatchers, NewContactViewState.Idle)
{
    fun addContact(
        contactAlias: String,
        lightningNodePubKey: String,
        lightningRouteHint: String?,
    ) {
        viewModelScope.launch(mainImmediate) {
            val alias: ContactAlias = contactAlias.trim().toContactAlias() ?: let {
                submitSideEffect(NewContactSideEffect.NicknameAndAddressRequired)
                return@launch
            }
            val pubKey: LightningNodePubKey = lightningNodePubKey.trim().toLightningNodePubKey() ?: let {
                submitSideEffect(NewContactSideEffect.InvalidLightningNodePublicKey)
                return@launch
            }
            val routeHint: LightningRouteHint? = lightningRouteHint?.trim()?.let {
                if (it.isEmpty()) {
                    // can potentially be passed an empty string if the EditText
                    // in which case we will treat it as null.
                    null
                } else {
                    val hint = it.toLightningRouteHint()
                    if (hint == null) {
                        submitSideEffect(NewContactSideEffect.InvalidRouteHint)
                        return@launch
                    } else {
                        hint
                    }
                }
            }

            contactRepository.createContact(
                alias,
                pubKey,
                routeHint
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
