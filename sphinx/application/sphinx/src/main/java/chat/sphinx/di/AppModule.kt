package chat.sphinx.di

import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.feature_relay.RelayDataHandlerImpl
import chat.sphinx.util.SphinxDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_encryption_key.EncryptionKeyHandler
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideCoroutineDispatchers(
        sphinxDispatchers: SphinxDispatchers
    ): CoroutineDispatchers =
        sphinxDispatchers

    @Provides
    @Singleton
    fun provideRelayDataHandlerImpl(
        authenticationStorage: AuthenticationStorage,
        authenticationCoreManager: AuthenticationCoreManager,
        dispatchers: CoroutineDispatchers,
        encryptionKeyHandler: EncryptionKeyHandler
    ): RelayDataHandlerImpl =
        RelayDataHandlerImpl(
            authenticationStorage,
            authenticationCoreManager,
            dispatchers,
            encryptionKeyHandler
        )

    @Provides
    fun provideRelayDataHandler(
        relayDataHandlerImpl: RelayDataHandlerImpl
    ): RelayDataHandler =
        relayDataHandlerImpl
}