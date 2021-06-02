package chat.sphinx.authentication

import android.content.Context
import io.matthewnelson.android_feature_authentication_core.data.AuthenticationCoreStorageAndroid
import io.matthewnelson.android_feature_authentication_core.data.AuthenticationSharedPrefsName
import io.matthewnelson.android_feature_authentication_core.data.MasterKeyAlias
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

open class SphinxAuthenticationCoreStorage(
    context: Context,
    dispatchers: CoroutineDispatchers
): AuthenticationCoreStorageAndroid(
    context.applicationContext,
    MasterKeyAlias(AUTHENTICATION_STORAGE_MASTER_KEY),
    AuthenticationSharedPrefsName(AUTHENTICATION_STORAGE_NAME),
    dispatchers
) {
    companion object {
        const val AUTHENTICATION_STORAGE_MASTER_KEY = "_sphinx_master_key"
        const val AUTHENTICATION_STORAGE_NAME = "sphinx_authentication"
    }

    /**
     * This should only be called from [SphinxKeyRestore] upon failure. This should
     * **never** be called elsewhere, except for good reason, thus it being only in
     * the implementation which lower layered modules have no idea about.
     * */
    suspend fun clearAuthenticationStorage() {
        withContext(dispatchers.io) {
            authenticationPrefs.edit().clear().let { editor ->
                if (!editor.commit()) {
                    editor.apply()
                    delay(100L)
                }
            }
        }
    }
}
