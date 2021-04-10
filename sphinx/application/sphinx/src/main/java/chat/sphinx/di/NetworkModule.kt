package chat.sphinx.di

import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_invite.NetworkQueryInvite
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.concept_network_query_subscription.NetworkQuerySubscription
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.feature_network_client.NetworkClientImpl
import chat.sphinx.feature_network_query_chat.NetworkQueryChatImpl
import chat.sphinx.feature_network_query_contact.NetworkQueryContactImpl
import chat.sphinx.feature_network_query_invite.NetworkQueryInviteImpl
import chat.sphinx.feature_network_query_lightning.NetworkQueryLightningImpl
import chat.sphinx.feature_network_query_message.NetworkQueryMessageImpl
import chat.sphinx.feature_network_query_subscription.NetworkQuerySubscriptionImpl
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
        encryptionKeyHandler: EncryptionKeyHandler,
    ): RelayDataHandlerImpl =
        RelayDataHandlerImpl(
            authenticationStorage,
            authenticationCoreManager,
            dispatchers,
            encryptionKeyHandler,
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
    fun provideNetworkQueryChatImpl(
        dispatchers: CoroutineDispatchers,
        moshi: Moshi,
        networkClient: NetworkClient,
        relayDataHandler: RelayDataHandler,
    ): NetworkQueryChatImpl =
        NetworkQueryChatImpl(
            dispatchers,
            moshi,
            networkClient,
            relayDataHandler,
        )

    @Provides
    fun provideNetworkQueryChat(
        networkQueryChatImpl: NetworkQueryChatImpl
    ): NetworkQueryChat =
        networkQueryChatImpl

    @Provides
    @Singleton
    fun provideNetworkQueryContactImpl(
        dispatchers: CoroutineDispatchers,
        moshi: Moshi,
        networkClient: NetworkClient,
        relayDataHandler: RelayDataHandler,
    ): NetworkQueryContactImpl =
        NetworkQueryContactImpl(
            dispatchers,
            moshi,
            networkClient,
            relayDataHandler,
        )

    @Provides
    fun provideNetworkQueryContact(
        networkQueryContactImpl: NetworkQueryContactImpl
    ): NetworkQueryContact =
        networkQueryContactImpl

    @Provides
    @Singleton
    fun provideNetworkQueryInviteImpl(
        dispatchers: CoroutineDispatchers,
        moshi: Moshi,
        networkClient: NetworkClient,
        relayDataHandler: RelayDataHandler
    ): NetworkQueryInviteImpl =
        NetworkQueryInviteImpl(
            dispatchers,
            moshi,
            networkClient,
            relayDataHandler,
        )

    @Provides
    fun provideNetworkQueryInvite(
        networkQueryInviteImpl: NetworkQueryInviteImpl
    ): NetworkQueryInvite =
        networkQueryInviteImpl

    @Provides
    @Singleton
    fun provideNetworkQueryLightningImpl(
        dispatchers: CoroutineDispatchers,
        moshi: Moshi,
        networkClient: NetworkClient,
        relayDataHandler: RelayDataHandler,
    ): NetworkQueryLightningImpl =
        NetworkQueryLightningImpl(
            dispatchers,
            moshi,
            networkClient,
            relayDataHandler,
        )

    @Provides
    fun provideNetworkQueryLightning(
        networkQueryLightningImpl: NetworkQueryLightningImpl
    ): NetworkQueryLightning =
        networkQueryLightningImpl

    @Provides
    @Singleton
    fun provideNetworkQueryMessageImpl(
        dispatchers: CoroutineDispatchers,
        moshi: Moshi,
        networkClient: NetworkClient,
        relayDataHandler: RelayDataHandler,
    ): NetworkQueryMessageImpl =
        NetworkQueryMessageImpl(
            dispatchers,
            moshi,
            networkClient,
            relayDataHandler,
        )

    @Provides
    fun provideNetworkQueryMessage(
        networkQueryMessageImpl: NetworkQueryMessageImpl
    ): NetworkQueryMessage =
        networkQueryMessageImpl

    @Provides
    @Singleton
    fun provideNetworkQuerySubscriptionImpl(
        dispatchers: CoroutineDispatchers,
        moshi: Moshi,
        networkClient: NetworkClient,
        relayDataHandler: RelayDataHandler,
    ): NetworkQuerySubscriptionImpl =
        NetworkQuerySubscriptionImpl(
            dispatchers,
            moshi,
            networkClient,
            relayDataHandler,
        )

    @Provides
    fun provideNetworkQuerySubscription(
        networkQuerySubscriptionImpl: NetworkQuerySubscriptionImpl
    ): NetworkQuerySubscription =
        networkQuerySubscriptionImpl
}
