package chat.sphinx.onboard_connecting.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.key_restore.KeyRestore
import chat.sphinx.key_restore.KeyRestoreResponse
import chat.sphinx.onboard_common.model.RedemptionCode
import chat.sphinx.onboard_connecting.navigation.OnBoardConnectingNavigator
import chat.sphinx.wrapper_invite.InviteString
import chat.sphinx.wrapper_invite.toValidInviteStringOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.MotionLayoutViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

internal inline val OnBoardConnectingFragmentArgs.restoreCode: RedemptionCode.AccountRestoration?
    get() {
        val redemptionCode = RedemptionCode.decode(argCode)

        if (redemptionCode is RedemptionCode.AccountRestoration) {
            return redemptionCode
        }
        return null
    }

internal inline val OnBoardConnectingFragmentArgs.connectionCode: RedemptionCode.NodeInvite?
    get() {
        val redemptionCode = RedemptionCode.decode(argCode)

        if (redemptionCode is RedemptionCode.NodeInvite) {
            return redemptionCode
        }
        return null
    }

internal inline val OnBoardConnectingFragmentArgs.inviteCode: InviteString?
    get() = argCode.toValidInviteStringOrNull()

@HiltViewModel
internal class OnBoardConnectingViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
    val navigator: OnBoardConnectingNavigator,
    private val keyRestore: KeyRestore,
): MotionLayoutViewModel<
        Any,
        Context,
        OnBoardConnectingSideEffect,
        OnBoardConnectingViewState
        >(dispatchers, OnBoardConnectingViewState.Connecting)
{

    private val args: OnBoardConnectingFragmentArgs by handle.navArgs()

    init {
        viewModelScope.launch(mainImmediate) {
            delay(500L)
            processCode()
        }
    }

    private fun processCode() {
        args.restoreCode?.let { restoreCode ->
            updateViewState(
                OnBoardConnectingViewState.Transition_Set2_DecryptKeys(restoreCode)
            )
            return
        } ?: args.connectionCode?.let { connectionCode ->
            return
        } ?: args.inviteCode?.let { inviteCode ->
            return
        }

        viewModelScope.launch(mainImmediate) {
            submitSideEffect(OnBoardConnectingSideEffect.InvalidCode)
            navigator.popBackStack()
        }
    }

    private var decryptionJob: Job? = null
    fun decryptInput(viewState: OnBoardConnectingViewState.Set2_DecryptKeys) {
        // TODO: Replace with automatic launching upon entering the 6th PIN character
        //  when Authentication View's Layout gets incorporated
        if (viewState.pinWriter.size() != 6 /*TODO: https://github.com/stakwork/sphinx-kotlin/issues/9*/) {
            viewModelScope.launch(mainImmediate) {
                submitSideEffect(OnBoardConnectingSideEffect.InvalidPinLength)
            }
        }

        if (decryptionJob?.isActive == true) {
            return
        }

        var decryptionJobException: Exception? = null
        decryptionJob = viewModelScope.launch(default) {
            try {
                val pin = viewState.pinWriter.toCharArray()

                val decryptedCode: RedemptionCode.AccountRestoration.DecryptedRestorationCode =
                    viewState.restoreCode.decrypt(pin, dispatchers)

                // TODO: Ask to use Tor before any network calls go out.
                // TODO: Hit relayUrl to verify creds work

                var success: KeyRestoreResponse.Success? = null
                keyRestore.restoreKeys(
                    privateKey = decryptedCode.privateKey,
                    publicKey = decryptedCode.publicKey,
                    userPin = pin,
                    relayUrl = decryptedCode.relayUrl,
                    authorizationToken = decryptedCode.authorizationToken,
                ).collect { flowResponse ->
                    // TODO: Implement in Authentication View when it get's built/refactored
                    if (flowResponse is KeyRestoreResponse.Success) {
                        success = flowResponse
                    }
                }

                success?.let { _ ->
                    // Overwrite PIN
                    viewState.pinWriter.reset()
                    repeat(6) {
                        viewState.pinWriter.append('0')
                    }

                    navigator.toOnBoardConnectedScreen()

                } ?: updateViewState(
                    OnBoardConnectingViewState.Set2_DecryptKeys(viewState.restoreCode)
                ).also {
                    submitSideEffect(OnBoardConnectingSideEffect.InvalidPin)
                }

            } catch (e: Exception) {
                decryptionJobException = e
            }
        }

        viewModelScope.launch(mainImmediate) {
            decryptionJob?.join()
            decryptionJobException?.let { exception ->
                updateViewState(
                    // reset view state
                    OnBoardConnectingViewState.Set2_DecryptKeys(viewState.restoreCode)
                )
                exception.printStackTrace()
                submitSideEffect(OnBoardConnectingSideEffect.DecryptionFailure)
            }
        }
    }

    override suspend fun onMotionSceneCompletion(value: Any) {
        return
    }
}