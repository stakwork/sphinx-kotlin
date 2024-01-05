package chat.sphinx.authentication

import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.feature_relay.RelayDataHandlerImpl
import chat.sphinx.key_restore.KeyRestore
import chat.sphinx.key_restore.KeyRestoreResponse
import chat.sphinx.wrapper_rsa.RsaPublicKey
import io.matthewnelson.concept_authentication.coordinator.AuthenticationRequest
import io.matthewnelson.concept_authentication.coordinator.AuthenticationResponse
import io.matthewnelson.feature_authentication_core.model.AuthenticateFlowResponse
import io.matthewnelson.crypto_common.annotations.RawPasswordAccess
import io.matthewnelson.crypto_common.clazzes.Password
import io.matthewnelson.crypto_common.clazzes.compare
import kotlinx.coroutines.flow.*

class SphinxKeyRestore(
    private val authenticationManager: SphinxAuthenticationCoreManager,
    private val authenticationStorage: SphinxAuthenticationCoreStorage,
    private val encryptionKeyHandler: SphinxEncryptionKeyHandler,
    private val relayDataHandlerImpl: RelayDataHandlerImpl
): KeyRestore() {

    @OptIn(RawPasswordAccess::class)
    override fun restoreKeys(
        privateKey: Password,
        publicKey: Password,
        userPin: CharArray,
        relayUrl: RelayUrl,
        authorizationToken: AuthorizationToken,
        transportKey: RsaPublicKey?,
    ): Flow<KeyRestoreResponse> = flow {
        when {
            authenticationManager.isAnEncryptionKeySet() -> {
                emit(KeyRestoreResponse.Error.KeysAlreadyPresent)
            }
            privateKey.value.isEmpty() -> {
                emit(KeyRestoreResponse.Error.PrivateKeyWasEmpty)
            }
            publicKey.value.isEmpty() -> {
                emit(KeyRestoreResponse.Error.PublicKeyWasEmpty)
            }
            else -> {
                emitAll(
                    restoreKeysImpl(
                        privateKey,
                        publicKey,
                        userPin,
                        relayUrl,
                        authorizationToken,
                        transportKey
                    )
                )
            }
        }
    }

    private fun restoreKeysImpl(
        privateKey: Password,
        publicKey: Password,
        userPin: CharArray,
        relayUrl: RelayUrl,
        jwt: AuthorizationToken,
        transportKey: RsaPublicKey?,
    ): Flow<KeyRestoreResponse> = flow {
        // authenticating the first time will return a SetKeyFirstTime response
        val request = AuthenticationRequest.LogIn(privateKey = null)
        val input = authenticationManager.getNewUserInput()

        var userPinBuilt = true
        for (c in userPin) {
            try {
                input.addCharacter(c)
            } catch (e: IllegalArgumentException) {
                emit(KeyRestoreResponse.Error.InvalidUserPin)
                userPinBuilt = false
                break
            }
        }

        if (userPinBuilt) {
            var confirmToSetPin: AuthenticateFlowResponse.ConfirmInputToSetForFirstTime? = null
            authenticationManager.authenticate(input, listOf(request)).collect { flowResponse ->
                if (flowResponse is AuthenticateFlowResponse.ConfirmInputToSetForFirstTime) {
                    confirmToSetPin = flowResponse
                }
            }

            confirmToSetPin?.let { setPin ->
                // We know the pin is validated, now we can save relay information
                // before setting keys such that it can be overwritten next attempt,
                // in case the user closes the application in the middle of encrypting
                // keys before they can be persisted.
                emit(KeyRestoreResponse.NotifyState.EncryptingRelayUrl)
                relayDataHandlerImpl.persistRelayUrlImpl(relayUrl, privateKey)

                emit(KeyRestoreResponse.NotifyState.EncryptingJavaWebToken)
                relayDataHandlerImpl.persistJavaWebTokenImpl(jwt, privateKey)

                transportKey?.let { key ->
                    relayDataHandlerImpl.persistRelayTransportKeyImpl(key, privateKey)
                }

                // Set keys that will be consumed when generateEncryptionKey is called
                encryptionKeyHandler.setKeysToRestore(privateKey, publicKey)

                emit(KeyRestoreResponse.NotifyState.EncryptingKeysWithUserPin)

                var completionResponse: AuthenticationResponse.Success.Authenticated? = null
                authenticationManager.setPasswordFirstTime(setPin, input, listOf(request))
                    .collect { flowResponse ->
                        if (
                            flowResponse is AuthenticateFlowResponse.Success &&
                            flowResponse.requests.size == 1 &&
                            flowResponse.requests[0] is AuthenticationResponse.Success.Authenticated
                        ) {
                            completionResponse = flowResponse.requests[0] as
                                    AuthenticationResponse.Success.Authenticated
                        }

                        if (flowResponse is AuthenticateFlowResponse.Error) {
                            // TODO: Implement
                        }
                }

                completionResponse?.let { _ ->
                    authenticationManager.getEncryptionKey()?.let { encryptionKey ->
                        // validate encryption key correctness
                        if (
                            !encryptionKey.privateKey.compare(privateKey) ||
                            !encryptionKey.publicKey.compare(publicKey)
                        ) {
                            authenticationStorage.clearAuthenticationStorage()
                            authenticationManager.logOut()
                            encryptionKeyHandler.clearKeysToRestore()
                            emit(KeyRestoreResponse.Error.KeysThatWereSetDidNotMatch)
                        } else {
                            encryptionKeyHandler.clearKeysToRestore()
                            emit(KeyRestoreResponse.Success)
                        }
                    } ?: let {
                        // TODO: Why is user not logged in. Should never be the case, but...
                    }
                } ?: let {
                    // TODO: verify the state of things and restore to blank slate
                }

            } ?: emit(KeyRestoreResponse.Error.InvalidUserPin)
        }
    }

    override suspend fun clearAll() {
        authenticationStorage.clearAuthenticationStorage()
        authenticationManager.logOut()
        encryptionKeyHandler.clearKeysToRestore()
    }
}
