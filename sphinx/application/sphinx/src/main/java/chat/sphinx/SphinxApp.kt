package chat.sphinx

import android.app.Application
import chat.sphinx.authentication.SphinxAuthenticationCoreManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SphinxApp: Application() {

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var sphinxAuthenticationCoreManager: SphinxAuthenticationCoreManager

    override fun onCreate() {
        super.onCreate()
        sphinxAuthenticationCoreManager
    }
}
