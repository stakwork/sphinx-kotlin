package chat.sphinx.di

import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.feature_network_client.NetworkClientImpl
import chat.sphinx.feature_network_query_chat.NetworkQueryChatImpl
import chat.sphinx.feature_relay.RelayDataHandlerImpl
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.matthewnelson.build_config.BuildConfigDebug
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_encryption_key.EncryptionKeyHandler
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

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

    @Provides
    @Singleton
    fun provideNetworkClientImpl(
        buildConfigDebug: BuildConfigDebug
    ): NetworkClientImpl =
        NetworkClientImpl(buildConfigDebug)

    @Provides
    fun provideNetworkClient(
        networkClientImpl: NetworkClientImpl
    ): NetworkClient =
        networkClientImpl

    @Provides
    @Singleton
    fun provideMoshi(): Moshi =
        Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideNetworkQueryChatImpl(
        dispatchers: CoroutineDispatchers,
        moshi: Moshi,
        networkClient: NetworkClient,
        relayDataHandler: RelayDataHandler
    ): NetworkQueryChatImpl =
        NetworkQueryChatImpl(
            dispatchers,
            moshi,
            networkClient,
            relayDataHandler
        )

    @Provides
    fun provideNetworkQueryChat(
        networkQueryChatImpl: NetworkQueryChatImpl
    ): NetworkQueryChat =
        networkQueryChatImpl
}
