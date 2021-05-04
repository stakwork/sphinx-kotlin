package chat.sphinx.di

import android.app.Application
import chat.sphinx.authentication.SphinxAuthenticationCoreManager
import chat.sphinx.authentication.SphinxAuthenticationCoreStorage
import chat.sphinx.feature_background_login.BackgroundLoginHandlerImpl
import chat.sphinx.authentication.SphinxEncryptionKeyHandler
import chat.sphinx.authentication.SphinxKeyRestore
import chat.sphinx.concept_background_login.BackgroundLoginHandler
import chat.sphinx.concept_crypto_rsa.RSA
import chat.sphinx.feature_coredb.CoreDBImpl
import chat.sphinx.feature_crypto_rsa.RSAAlgorithm
import chat.sphinx.feature_crypto_rsa.RSAImpl
import chat.sphinx.key_restore.KeyRestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_authentication.state.AuthenticationStateManager
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_encryption_key.EncryptionKeyHandler
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthenticationModule {

    @Provides
    @Singleton
    fun provideRSA(): RSA =
        RSAImpl(RSAAlgorithm.RSA_ECB_PKCS1Padding)

    @Provides
    fun provideAuthenticationCoreManager(
        sphinxAuthenticationCoreManager: SphinxAuthenticationCoreManager
    ): AuthenticationCoreManager =
        sphinxAuthenticationCoreManager

    @Provides
    fun provideAuthenticationStateManager(
        sphinxAuthenticationCoreManager: SphinxAuthenticationCoreManager
    ): AuthenticationStateManager =
        sphinxAuthenticationCoreManager

    @Provides
    fun provideAuthenticationStorage(
        sphinxAuthenticationCoreStorage: SphinxAuthenticationCoreStorage
    ): AuthenticationStorage =
        sphinxAuthenticationCoreStorage

    @Provides
    fun provideEncryptionKeyHandler(
        sphinxEncryptionKeyHandler: SphinxEncryptionKeyHandler
    ): EncryptionKeyHandler =
        sphinxEncryptionKeyHandler

    @Provides
    fun provideBackgroundLoginHandler(
        authenticationCoreManager: AuthenticationCoreManager,
        authenticationStorage: AuthenticationStorage
    ): BackgroundLoginHandler =
        BackgroundLoginHandlerImpl(
            authenticationCoreManager,
            authenticationStorage
        )

    @Provides
    fun provideKeyRestore(
        sphinxKeyRestore: SphinxKeyRestore
    ): KeyRestore =
        sphinxKeyRestore

    @Provides
    @Singleton
    fun provideSphinxAuthenticationCoreManager(
        application: Application,
        dispatchers: CoroutineDispatchers,
        encryptionKeyHandler: SphinxEncryptionKeyHandler,
        sphinxAuthenticationCoreStorage: SphinxAuthenticationCoreStorage,
        coreDBImpl: CoreDBImpl,
    ): SphinxAuthenticationCoreManager =
        SphinxAuthenticationCoreManager(
            application,
            dispatchers,
            encryptionKeyHandler,
            sphinxAuthenticationCoreStorage,
            coreDBImpl,
        )
}
