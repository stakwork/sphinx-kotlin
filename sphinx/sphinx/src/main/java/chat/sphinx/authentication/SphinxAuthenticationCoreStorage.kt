package chat.sphinx.authentication

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.matthewnelson.android_feature_authentication_core.data.AuthenticationCoreStorageAndroid
import io.matthewnelson.android_feature_authentication_core.data.AuthenticationSharedPrefsName
import io.matthewnelson.android_feature_authentication_core.data.MasterKeyAlias
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SphinxAuthenticationCoreStorage @Inject constructor(
    @ApplicationContext appContext: Context,
    dispatchers: CoroutineDispatchers
): AuthenticationCoreStorageAndroid(
    appContext,
    MasterKeyAlias(AUTHENTICATION_STORAGE_MASTER_KEY),
    AuthenticationSharedPrefsName(AUTHENTICATION_STORAGE_NAME),
    dispatchers
) {
    companion object {
        const val AUTHENTICATION_STORAGE_MASTER_KEY = "_sphinx_master_key"
        const val AUTHENTICATION_STORAGE_NAME = "sphinx_authentication"
    }
}