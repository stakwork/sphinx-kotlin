package chat.sphinx.invite_friend.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_network_query_invite.NetworkQueryInvite
import chat.sphinx.concept_repository_connect_manager.ConnectManagerRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.invite_friend.navigation.InviteFriendNavigator
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.wrapper_common.lightning.toSat
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class InviteFriendViewModel @Inject constructor(
    val navigator: InviteFriendNavigator,
    private val networkQueryInvite: NetworkQueryInvite,
    private val contactRepository: ContactRepository,
    private val connectManagerRepository: ConnectManagerRepository,
    private val app: Application,
    dispatchers: CoroutineDispatchers,
): SideEffectViewModel<
        Context,
        InviteFriendSideEffect,
        InviteFriendViewState
        >(dispatchers, InviteFriendViewState.Idle)
{

    init {
        viewModelScope.launch(mainImmediate) {
            networkQueryInvite.getLowestNodePrice().collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {}

                    is Response.Error -> {}

                    is Response.Success -> {
                        loadResponse.value.response?.price?.let { price ->
                            price.toLong().toSat()?.let { sats ->
                                updateViewState(InviteFriendViewState.InviteFriendLowestPrice(sats))
                            }
                        }
                    }
                }
            }
        }
    }

    private var createInviteJob: Job? = null
    fun createNewInvite(
        nickname: String?,
        welcomeMessage: String?,
        sats: Long?
    ) {
        if (createInviteJob?.isActive == true) {
            return
        }

        createInviteJob = viewModelScope.launch(mainImmediate) {

            if (sats != null && sats > 0L && !nickname.isNullOrEmpty()) {
                connectManagerRepository.createInvite(nickname, welcomeMessage ?: "", sats, null)
                updateViewState(InviteFriendViewState.InviteCreationSucceed)
            } else {
                submitSideEffect(InviteFriendSideEffect.EmptySats)
                updateViewState(InviteFriendViewState.InviteCreationFailed)
            }

//            if (nickname == null || nickname.isEmpty()) {
//                submitSideEffect(InviteFriendSideEffect.EmptyNickname)
//                updateViewState(InviteFriendViewState.InviteCreationFailed)
//                return@launch
//            }
//
//            val message = if (welcomeMessage?.trim()?.isNotEmpty() == true) {
//                welcomeMessage
//            } else {
//                app.getString(R.string.invite_friend_message_hint)
//            }
//
//            contactRepository.createNewInvite(nickname, message).collect { loadResponse ->
//                @Exhaustive
//                when (loadResponse) {
//                    is LoadResponse.Loading -> {
//                        updateViewState(InviteFriendViewState.InviteCreationLoading)
//                    }
//
//                    is Response.Error -> {
//                        submitSideEffect(InviteFriendSideEffect.InviteFailed)
//                        updateViewState(InviteFriendViewState.InviteCreationFailed)
//                    }
//
//                    is Response.Success -> {
//                        updateViewState(InviteFriendViewState.InviteCreationSucceed)
//                    }
//                }
//            }
        }
    }
}
