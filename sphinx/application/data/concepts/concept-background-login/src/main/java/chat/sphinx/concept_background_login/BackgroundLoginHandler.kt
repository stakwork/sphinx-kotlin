package chat.sphinx.concept_background_login

import io.matthewnelson.concept_encryption_key.EncryptionKey

abstract class BackgroundLoginHandler {

    /**
     * [updateLastLoginTimeOnSuccess] will update the AuthenticationStorage with a successful login
     * such that last login time can be updated only when desired (on a splash screen,
     * and not if handling notifications or something).
     * */
    abstract suspend fun attemptBackgroundLogin(
        updateLastLoginTimeOnSuccess: Boolean = false
    ): EncryptionKey?

    abstract suspend fun updateLoginTime(): Boolean

    /**
     * Will update the user setting. As this updates SharedPreferences for
     * Android, be careful to only call updateSetting on `KeyUp` if using a
     * slider, or upon `save settings`.
     * */
    abstract suspend fun updateSetting(pinTimeOutHours: Int): Boolean

    /**
     * Returns the current user setting for pin timeout
     * */
    abstract suspend fun getTimeOutSetting(): Int
}
