package chat.sphinx.feature_background_login

import chat.sphinx.concept_background_login.BackgroundLoginHandler
import io.matthewnelson.concept_authentication.coordinator.AuthenticationRequest
import io.matthewnelson.concept_authentication.coordinator.AuthenticationResponse
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_encryption_key.EncryptionKey
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import io.matthewnelson.crypto_common.annotations.RawPasswordAccess
import io.matthewnelson.crypto_common.clazzes.Password
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BackgroundLoginHandlerImpl(
    private val authenticationManager: AuthenticationCoreManager,
    private val authenticationStorage: AuthenticationStorage
): BackgroundLoginHandler() {

    companion object {
        const val BACKGROUND_LOGIN_KEY = "BACKGROUND_LOGIN"
        private const val DELIMINATOR = "|-SAFU-|"
        private const val NULL = "NULL"
        const val DEFAULT_TIMEOUT = 12

        @Volatile
        private var timeoutSettingsCache: Int? = null
    }

    private val lock = Mutex()

    override suspend fun attemptBackgroundLogin(
        updateLastLoginTimeOnSuccess: Boolean
    ): EncryptionKey? {

        // Check if we're already logged in
        authenticationManager.getEncryptionKey()?.let { encryptionKey ->
            if (updateLastLoginTimeOnSuccess) {
                timeoutSettingsCache?.let { timeout ->
                    // if our cache isn't null, update our string with the new login time
                    lock.withLock {
                        updateSettingsImpl(timeout, encryptionKey)
                    }
                } ?: updateLoginTime()
            }
            return encryptionKey
        }


        lock.withLock {
            // Try to pull key from AuthenticationStorage and login with it
            return authenticationStorage.getString(BACKGROUND_LOGIN_KEY, null)?.let { bgLoginString ->

                bgLoginString.split(DELIMINATOR).let { splits ->

                    splits.elementAtOrNull(0)?.let { timeoutSettingHours ->

                        // update cache while we're here
                        timeoutSettingsCache = timeoutSettingHours.toInt()

                        splits.elementAtOrNull(1)?.let { lastLoginTimeMillis ->

                            splits.elementAtOrNull(2)?.let { privateKeyString ->

                                when {
                                    privateKeyString == NULL -> {
                                        null
                                    }
                                    timeoutSettingHours.toInt() == 0 -> {
                                        // private key string is not NULL, so clear it.
                                        updateSettingsImpl(0, null)
                                        null
                                    }
                                    (System.currentTimeMillis() - lastLoginTimeMillis.toLong()) <
                                            (timeoutSettingHours.toLong() * 3_600_000) -> {

                                        val privateKey = Password(privateKeyString.toCharArray())
                                        val request = AuthenticationRequest.LogIn(privateKey)
                                        authenticationManager.authenticate(
                                            privateKey,
                                            request
                                        ).firstOrNull().let { response ->
                                            if (response is AuthenticationResponse.Success.Key) {
                                                // Update our persisted string value with new
                                                // login time.
                                                if (updateLastLoginTimeOnSuccess) {
                                                    updateSettingsImpl(
                                                        timeoutSettingHours.toInt(),
                                                        response.encryptionKey
                                                    )
                                                }
                                                response.encryptionKey
                                            } else {
                                                // Error validating the private key stored here
                                                // to login with, so clear it to require user
                                                // authentication
                                                updateSettingsImpl(
                                                    timeoutSettingHours.toInt(),
                                                    null
                                                )
                                                null
                                            }
                                        }

                                    }
                                    else -> {
                                        // private key string is not null, so clear it.
                                        updateSettingsImpl(timeoutSettingHours.toInt(), null)
                                        null
                                    }
                                }

                            } // private key string was null

                        } // last login time was null

                    } // timeout settings was null

                }

            }
        }
    }

    override suspend fun updateLoginTime(): Boolean {
        return authenticationManager.getEncryptionKey()?.let { encryptionKey ->
            // We're logged in and can update the setting, otherwise, don't
            lock.withLock {
                val timeoutSetting = timeoutSettingsCache
                    ?: authenticationStorage.getString(BACKGROUND_LOGIN_KEY, null)
                        ?.split(DELIMINATOR)
                        ?.elementAtOrNull(0)
                        ?.toInt()
                    ?: DEFAULT_TIMEOUT

                updateSettingsImpl(timeoutSetting, encryptionKey)
                true
            }
        } ?: false
    }

    override suspend fun getTimeOutSetting(): Int {
        return lock.withLock {
            timeoutSettingsCache ?: let {
                authenticationStorage.getString(BACKGROUND_LOGIN_KEY, null)
                    ?.split(DELIMINATOR)
                    ?.elementAtOrNull(0)
                    ?.toInt()
                    ?: DEFAULT_TIMEOUT
                        .also { timeoutSettingsCache = it }
            }
        }
    }

    override suspend fun updateSetting(pinTimeOutHours: Int): Boolean {
        return authenticationManager.getEncryptionKey()?.let { encryptionKey ->
            lock.withLock {
                // We're logged in and can update the setting, otherwise, don't
                updateSettingsImpl(pinTimeOutHours, encryptionKey)
                true
            }
        } ?: false
    }

    /**
     * Used also to clear keys.
     *
     * Format (single line string):
     *     <current pin time out setting (Int)>
     *     DELIMINATOR
     *     <current clock time (Long)>
     *     DELIMINATOR
     *     <private key>
     * */
    @OptIn(RawPasswordAccess::class)
    private suspend fun updateSettingsImpl(pinTimeOutHours: Int, encryptionKey: EncryptionKey?) {
        val sb = StringBuilder()

        // append to string and set cache variable
        timeoutSettingsCache = if (pinTimeOutHours < 0) {
            sb.append(0)
            0
        } else {
            sb.append(pinTimeOutHours)
            pinTimeOutHours
        }

        sb.append(DELIMINATOR)
        sb.append(System.currentTimeMillis())
        sb.append(DELIMINATOR)

        if (pinTimeOutHours <= 0) {
            sb.append(NULL)
        } else {
            sb.append(encryptionKey?.privateKey?.value?.joinToString("") ?: NULL)
        }

        authenticationStorage.putString(BACKGROUND_LOGIN_KEY, sb.toString())
    }
}
