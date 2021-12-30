package chat.sphinx.authentication

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.pm.ActivityInfo
import android.os.Bundle
import chat.sphinx.feature_coredb.CoreDBImpl
import chat.sphinx.feature_sphinx_service.ApplicationServiceTracker
import io.matthewnelson.android_feature_authentication_core.components.AuthenticationCoreManagerAndroid
import io.matthewnelson.android_feature_authentication_core.components.AuthenticationManagerInitializerAndroid
import io.matthewnelson.concept_authentication.state.AuthenticationState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_encryption_key.EncryptionKey
import io.matthewnelson.crypto_common.clazzes.HashIterations

class SphinxAuthenticationCoreManager(
    application: Application,
    private val applicationServiceTracker: ApplicationServiceTracker,
    dispatchers: CoroutineDispatchers,
    encryptionKeyHandler: SphinxEncryptionKeyHandler,
    sphinxAuthenticationCoreStorage: SphinxAuthenticationCoreStorage,
    private val sphinxCoreDBImpl: CoreDBImpl,
): AuthenticationCoreManagerAndroid(
    dispatchers,
    HashIterations(250_000),
    encryptionKeyHandler,
    sphinxAuthenticationCoreStorage,
    AuthenticationManagerInitializerAndroid(
        application,
        minimumUserInputLength = 6,
        maximumUserInputLength = 6,
    )
) {
    override val logOutWhenApplicationIsClearedFromRecentsTray: Boolean = false

    override fun onInitialLoginSuccess(encryptionKey: EncryptionKey) {
        super.onInitialLoginSuccess(encryptionKey)
        sphinxCoreDBImpl.initializeDatabase(encryptionKey)
    }

    fun logOut() {
        setAuthenticationStateRequired(AuthenticationState.Required.InitialLogIn)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        super.onActivityCreated(activity, savedInstanceState)

        if (applicationServiceTracker.taskIsRemoved) {
            applicationServiceTracker.taskReturned()
        }
    }
}
