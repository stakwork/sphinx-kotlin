package chat.sphinx.onboard.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.concept_network_tor.TorManager
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.onboard.navigation.OnBoardNavigator
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_authentication.coordinator.AuthenticationCoordinator
import io.matthewnelson.concept_authentication.coordinator.AuthenticationRequest
import io.matthewnelson.concept_authentication.coordinator.AuthenticationResponse
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.annotation.meta.Exhaustive
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
internal class OnBoardViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: OnBoardNavigator,
    private val networkQueryContact: NetworkQueryContact,
    private val torManager: TorManager,
    private val relayDataHandler: RelayDataHandler,
    private val authenticationCoordinator: AuthenticationCoordinator
): SideEffectViewModel<
        Context,
        OnBoardSideEffect,
        OnBoardViewState
        >(dispatchers, OnBoardViewState.Idle)
{
    fun generateToken(ip: String, code: String) {
        viewModelScope.launch(mainImmediate) {
            val authToken = generateToken()
            val relayUrl = parseRelayUrl(RelayUrl(ip))

            networkQueryContact.generateToken(relayUrl, authToken, code).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                    }
                    is Response.Error -> {
                        updateViewState(OnBoardViewState.Error)

                        submitSideEffect(OnBoardSideEffect.GenerateTokenFailed)
                    }
                    is Response.Success -> {
                        authenticationCoordinator.submitAuthenticationRequest(
                            AuthenticationRequest.LogIn()
                        ).firstOrNull().let { response ->
                            @Exhaustive
                            when (response) {
                                null,
                                is AuthenticationResponse.Failure -> {
                                    // will not be returned as back press for handling
                                    // a LogIn request minimizes the application until
                                    // User has authenticated
                                }
                                is AuthenticationResponse.Success.Authenticated -> {
                                    relayDataHandler.persistAuthorizationToken(authToken)
                                    relayDataHandler.persistRelayUrl(relayUrl)

                                    goToOnBoardNameScreen()
                                }
                                is AuthenticationResponse.Success.Key -> {
                                    // will never be returned
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun goToOnBoardNameScreen() {
        viewModelScope.launch(mainImmediate) {
            navigator.toOnBoardNameScreen()
        }
    }

    private fun generateToken(): AuthorizationToken {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val token = (1..20)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")

        return AuthorizationToken(token)
    }

    private inline val String.isOnionAddress: Boolean
        get() = matches("([a-z2-7]{56}).onion.*".toRegex())

    suspend fun parseRelayUrl(relayUrl: RelayUrl): RelayUrl {
        return try {
            val httpUrl = relayUrl.value.toHttpUrl()
            torManager.setTorRequired(httpUrl.host.isOnionAddress)

            // is a valid url with scheme
            relayUrl
        } catch (e: IllegalArgumentException) {

            // does not contain http, https... check if it's an onion address
            if (relayUrl.value.isOnionAddress) {
                // only use http if it is an onion address
                torManager.setTorRequired(true)
                RelayUrl("http://${relayUrl.value}")
            } else {
                torManager.setTorRequired(false)
                RelayUrl("http://${relayUrl.value}")
            }
        }
    }
}
