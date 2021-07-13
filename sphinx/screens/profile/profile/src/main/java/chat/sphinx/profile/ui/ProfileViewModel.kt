package chat.sphinx.profile.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.concept_background_login.BackgroundLoginHandler
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.PrivatePhoto
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_message_media.MediaType
import chat.sphinx.wrapper_message_media.toMediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_authentication.coordinator.AuthenticationCoordinator
import io.matthewnelson.concept_authentication.coordinator.AuthenticationRequest
import io.matthewnelson.concept_authentication.coordinator.AuthenticationResponse
import io.matthewnelson.concept_authentication.coordinator.ConfirmedPinListener
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_encryption_key.EncryptionKey
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.crypto_common.annotations.RawPasswordAccess
import io.matthewnelson.crypto_common.clazzes.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okio.base64.encodeBase64
import org.cryptonode.jncryptor.AES256JNCryptor
import org.cryptonode.jncryptor.CryptorException
import java.io.FileInputStream
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
internal class ProfileViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val app: Application,
    private val authenticationCoordinator: AuthenticationCoordinator,
    private val backgroundLoginHandler: BackgroundLoginHandler,
    private val cameraCoordinator: ViewModelCoordinator<CameraRequest, CameraResponse>,
    private val contactRepository: ContactRepository,
    private val lightningRepository: LightningRepository,
    private val relayDataHandler: RelayDataHandler,
): SideEffectViewModel<
        Context,
        ProfileSideEffect,
        ProfileViewState>(dispatchers, ProfileViewState.Basic)
{

    val profileMenuViewStateContainer: ViewStateContainer<MenuBottomViewState> by lazy {
        ViewStateContainer(MenuBottomViewState.Closed)
    }

    private var resetPINJob: Job? = null
    fun resetPIN() {
        if (resetPINJob?.isActive == true) return

        resetPINJob = viewModelScope.launch(mainImmediate) {
            authenticationCoordinator.submitAuthenticationRequest(
                AuthenticationRequest.ResetPassword()
            ).firstOrNull()?.let { response ->
                @Exhaustive
                when (response) {
                    is AuthenticationResponse.Failure -> {
                        // handle
                    }
                    is AuthenticationResponse.Success.Authenticated -> {
                        // handle
                    }
                    is AuthenticationResponse.Success.Key -> {}
                }
            }
        }
    }

    suspend fun getAccountBalance(): StateFlow<NodeBalance?> =
        lightningRepository.getAccountBalance()

    suspend fun updateOwner(
        alias: String?,
        privatePhoto: PrivatePhoto?,
        tipAmount: Sat?
    ): Response<Any, ResponseError> =
        contactRepository.updateOwner(alias, privatePhoto, tipAmount)

    fun persistPINTimeout() {
        _pinTimeoutStateFlow.value?.let { timeout ->
            viewModelScope.launch(mainImmediate) {
                if (!backgroundLoginHandler.updateSetting(timeout)) {
                    _pinTimeoutStateFlow.value = backgroundLoginHandler.getTimeOutSetting()
                    // TODO: Side effect, failed to persist setting
                }
            }
        }
    }

    fun updatePINTimeOutStateFlow(progress: Int) {
        _pinTimeoutStateFlow.value = progress
    }

    fun backupKeys() {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(ProfileSideEffect.BackupKeysPinNeeded)

            var passwordPin: Password? = null

            authenticationCoordinator.submitAuthenticationRequest(
                AuthenticationRequest.ConfirmPin(object : ConfirmedPinListener() {
                    override suspend fun doWithConfirmedPassword(password: Password) {
                        passwordPin = password
                    }
                })
            ).firstOrNull().let { response ->
                @Exhaustive
                when (response) {
                    null,
                    is AuthenticationResponse.Failure -> {
                        submitSideEffect(ProfileSideEffect.WrongPIN)
                    }
                    is AuthenticationResponse.Success.Authenticated -> {
                        authenticationCoordinator.submitAuthenticationRequest(
                            AuthenticationRequest.GetEncryptionKey()
                        ).firstOrNull().let { keyResponse ->
                            @Exhaustive
                            when (keyResponse) {
                                null,
                                is AuthenticationResponse.Failure -> {
                                    submitSideEffect(ProfileSideEffect.BackupKeysFailed)
                                }
                                is AuthenticationResponse.Success.Authenticated -> {

                                }
                                is AuthenticationResponse.Success.Key -> {
                                    passwordPin?.let {
                                        encryptKeysAndExport(keyResponse.encryptionKey, it)
                                    }
                                }
                            }
                        }
                    }
                    is AuthenticationResponse.Success.Key -> { }
                }

                passwordPin?.clear()
            }
        }
    }

    @OptIn(RawPasswordAccess::class)
    private suspend fun encryptKeysAndExport(encryptionKey: EncryptionKey, passwordPin: Password) {
        val relayUrl = relayDataHandler.retrieveRelayUrl()?.value
        val authToken = relayDataHandler.retrieveAuthorizationToken()?.value
        val privateKey = String(encryptionKey.privateKey.value)
        val publicKey = String(encryptionKey.publicKey.value)

        val keysString = "$privateKey::$publicKey::${relayUrl}::${authToken}"

        try {
            val encryptedString = AES256JNCryptor()
                .encryptData(keysString.toByteArray(), passwordPin.value)
                .encodeBase64()

            val finalString = "keys::${encryptedString}"
                .toByteArray()
                .encodeBase64()
                .replace("(.{64})".toRegex(), "$1\n")

            submitSideEffect(ProfileSideEffect.CopyBackupToClipboard(finalString))
        } catch (e: CryptorException) {
            submitSideEffect(ProfileSideEffect.BackupKeysFailed)
        } catch (e: IllegalArgumentException) {
            submitSideEffect(ProfileSideEffect.BackupKeysFailed)
        }
    }

    private var cameraJob: Job? = null

    @Suppress("BlockingMethodInNonBlockingContext")
    fun menuBottomProfilePicCamera() {
        if (cameraJob?.isActive == true) {
            return
        }

        cameraJob = viewModelScope.launch(mainImmediate) {
            val response = cameraCoordinator.submitRequest(CameraRequest)

            @Exhaustive
            when (response) {
                is Response.Error -> {

                }
                is Response.Success -> {

                    @Exhaustive
                    when (response.value) {
                        is CameraResponse.Image -> {
                            val ext = response.value.value.extension
                            val mediaType = MediaType.Image(MediaType.IMAGE + "/$ext")

                            val stream: FileInputStream? = try {
                                FileInputStream(response.value.value)
                            } catch (e: Exception) {
                                // TODO: Handle error
                                null
                            }

                            if (stream != null) {
                                viewModelScope.launch(mainImmediate) {
                                    val repoResponse = contactRepository.updateOwnerProfilePic(
                                        stream,
                                        mediaType,
                                        response.value.value.name,
                                        ext
                                    )

                                    @Exhaustive
                                    when (repoResponse) {
                                        is Response.Error -> {

                                        }
                                        is Response.Success -> {

                                        }
                                    }

                                    // TODO:
                                    //  - Use stream, MediaType, filename, extension
                                    //  - Delete file from cache on success

                                    // TODO: On success
                                    profileMenuViewStateContainer.updateViewState(MenuBottomViewState.Closed)

                                    // TODO: Remove upon implementation
                                    try {
                                        stream.close()
                                    } catch (e: Exception) {}

                                    response.value.value.delete()
                                }.join()
                            }
                        }
                    }
                }
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun handleActivityResultUri(uri: Uri?) {
        if (uri == null) {
            return
        }

        val cr = app.contentResolver

        cr.getType(uri)?.let { crType ->

            MimeTypeMap.getSingleton().getExtensionFromMimeType(crType)?.let { ext ->

                crType.toMediaType().let { mType ->

                    @Exhaustive
                    when (mType) {
                        is MediaType.Image -> {
                            val stream: InputStream? = try {
                                cr.openInputStream(uri)
                            } catch (e: Exception) {
                                // TODO: Handle Error
                                null
                            }

                            if (stream != null) {
                                viewModelScope.launch(mainImmediate) {
                                    val repoResponse = contactRepository.updateOwnerProfilePic(
                                        stream,
                                        mType,
                                        "image",
                                        ext
                                    )

                                    @Exhaustive
                                    when (repoResponse) {
                                        is Response.Error -> {

                                        }
                                        is Response.Success -> {

                                        }
                                    }
                                    // TODO: Use stream, MediaType, filename, extension

                                    // TODO: On success
                                    profileMenuViewStateContainer.updateViewState(MenuBottomViewState.Closed)

                                    // TODO: Remove upon implementation
                                    try {
                                        stream.close()
                                    } catch (e: Exception) {}
                                }
                            }
                        }
                        is MediaType.Audio,
                        is MediaType.Pdf,
                        is MediaType.Text,
                        is MediaType.Unknown,
                        is MediaType.Video -> {
                            // do nothing
                        }
                    }

                }

            }

        }
    }

    private val _relayUrlStateFlow: MutableStateFlow<String?> by lazy {
        MutableStateFlow(null)
    }
    private val _pinTimeoutStateFlow: MutableStateFlow<Int?> by lazy {
        MutableStateFlow(null)
    }

    val relayUrlStateFlow: StateFlow<String?>
        get() = _relayUrlStateFlow.asStateFlow()
    val pinTimeoutStateFlow: StateFlow<Int?>
        get() = _pinTimeoutStateFlow.asStateFlow()
    val accountOwnerStateFlow: StateFlow<Contact?>
        get() = contactRepository.accountOwner

    init {
        viewModelScope.launch(mainImmediate) {
            _relayUrlStateFlow.value = relayDataHandler.retrieveRelayUrl()?.value
            _pinTimeoutStateFlow.value = backgroundLoginHandler.getTimeOutSetting()
        }
    }
}
