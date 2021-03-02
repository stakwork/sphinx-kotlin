package chat.sphinx.background_login

import io.matthewnelson.concept_encryption_key.EncryptionKey

interface BackgroundLoginHandler {

    suspend fun attemptBackgroundLogin(): EncryptionKey?

    suspend fun updateLoginTime(): Boolean

    /**
     * Will update the user setting. As this updates SharedPreferences for
     * Android, be careful to only call updateSetting on `KeyUp` if using a
     * slider, or upon `save settings`.
     * */
    suspend fun updateSetting(pinTimeOutHours: Int): Boolean

    /**
     * Returns the current user setting for pin timeout
     * */
    suspend fun getTimeOutSetting(): Int
}
