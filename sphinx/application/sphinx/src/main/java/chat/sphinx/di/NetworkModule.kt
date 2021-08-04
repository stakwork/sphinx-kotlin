package chat.sphinx.di

import android.app.Application
import android.content.Context
import chat.sphinx.concept_network_call.NetworkCall
import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_client_cache.NetworkClientCache
import chat.sphinx.concept_network_query_meme_server.NetworkQueryMemeServer
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_invite.NetworkQueryInvite
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.concept_network_query_subscription.NetworkQuerySubscription
import chat.sphinx.concept_network_query_version.NetworkQueryVersion
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.concept_network_tor.TorManager
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_socket_io.SocketIOManager
import chat.sphinx.feature_network_client.NetworkClientImpl
import chat.sphinx.feature_network_query_meme_server.NetworkQueryMemeServerImpl
import chat.sphinx.feature_network_query_chat.NetworkQueryChatImpl
import chat.sphinx.feature_network_query_contact.NetworkQueryContactImpl
import chat.sphinx.feature_network_query_invite.NetworkQueryInviteImpl
import chat.sphinx.feature_network_query_lightning.NetworkQueryLightningImpl
import chat.sphinx.feature_network_query_message.NetworkQueryMessageImpl
import chat.sphinx.feature_network_query_subscription.NetworkQuerySubscriptionImpl
import chat.sphinx.feature_network_query_version.NetworkQueryVersionImpl
import chat.sphinx.feature_network_relay_call.NetworkRelayCallImpl
import chat.sphinx.feature_network_tor.TorManagerAndroid
import chat.sphinx.feature_relay.RelayDataHandlerImpl
import chat.sphinx.feature_socket_io.SocketIOManagerImpl
import chat.sphinx.feature_sphinx_service.ApplicationServiceTracker
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.wrapper_meme_server.AuthenticationToken
import chat.sphinx.wrapper_relay.AuthorizationToken
import coil.util.CoilUtils
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.matthewnelson.build_config.BuildConfigDebug
import io.matthewnelson.build_config.BuildConfigVersionCode
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_encryption_key.EncryptionKeyHandler
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideTorManagerAndroid(
        application: Application,
        applicationScope: CoroutineScope,
        authenticationStorage: AuthenticationStorage,
        buildConfigDebug: BuildConfigDebug,
        buildConfigVersionCode: BuildConfigVersionCode,
        dispatchers: CoroutineDispatchers,
        LOG: SphinxLogger,
    ): TorManagerAndroid =
        TorManagerAndroid(
            application,
            applicationScope,
            authenticationStorage,
            buildConfigDebug,
            buildConfigVersionCode,
            dispatchers,
            LOG,
        )

    @Provides
    fun provideTorManager(
        torManagerAndroid: TorManagerAndroid
    ): TorManager =
        torManagerAndroid

    @Provides
    fun provideApplicationServiceTracker(
        torManagerAndroid: TorManagerAndroid
    ): ApplicationServiceTracker =
        torManagerAndroid

    @Provides
    @Singleton
    fun provideRelayDataHandlerImpl(
        authenticationStorage: AuthenticationStorage,
        authenticationCoreManager: AuthenticationCoreManager,
        dispatchers: CoroutineDispatchers,
        encryptionKeyHandler: EncryptionKeyHandler,
        torManager: TorManager,
    ): RelayDataHandlerImpl =
        RelayDataHandlerImpl(
            authenticationStorage,
            authenticationCoreManager,
            dispatchers,
            encryptionKeyHandler,
            torManager
        )

    @Provides
    fun provideRelayDataHandler(
        relayDataHandlerImpl: RelayDataHandlerImpl
    ): RelayDataHandler =
        relayDataHandlerImpl

    @Provides
    @Singleton
    fun provideNetworkClientImpl(
        @ApplicationContext appContext: Context,
        buildConfigDebug: BuildConfigDebug,
        torManager: TorManager,
        dispatchers: CoroutineDispatchers,
        LOG: SphinxLogger,
    ): NetworkClientImpl =
        NetworkClientImpl(
            buildConfigDebug,
            CoilUtils.createDefaultCache(appContext),
            dispatchers,
            NetworkClientImpl.RedactedLoggingHeaders(
                listOf(
                    AuthorizationToken.AUTHORIZATION_HEADER,
                    AuthenticationToken.HEADER_KEY
                )
            ),
            torManager,
            LOG,
        )

    @Provides
    fun provideNetworkClient(
        networkClientImpl: NetworkClientImpl
    ): NetworkClient =
        networkClientImpl

    @Provides
    fun provideNetworkClientCache(
        networkClientImpl: NetworkClientImpl
    ): NetworkClientCache =
        networkClientImpl

    @Provides
    @Singleton
    fun provideSocketIOManagerImpl(
        dispatchers: CoroutineDispatchers,
        moshi: Moshi,
        networkClient: NetworkClient,
        relayDataHandler: RelayDataHandler,
        LOG: SphinxLogger,
    ): SocketIOManagerImpl =
        SocketIOManagerImpl(
            dispatchers,
            moshi,
            networkClient,
            relayDataHandler,
            LOG,
        )

    @Provides
    fun provideSocketIOManager(
        socketIOManagerImpl: SocketIOManagerImpl
    ): SocketIOManager =
        socketIOManagerImpl

    @Provides
    @Singleton
    fun provideNetworkRelayCallImpl(
        dispatchers: CoroutineDispatchers,
        moshi: Moshi,
        networkClient: NetworkClient,
        relayDataHandler: RelayDataHandler,
        sphinxLogger: SphinxLogger,
    ): NetworkRelayCallImpl =
        NetworkRelayCallImpl(
            dispatchers,
            moshi,
            networkClient,
            relayDataHandler,
            sphinxLogger
        )

    @Provides
    fun provideNetworkRelayCall(
        networkRelayCallImpl: NetworkRelayCallImpl
    ): NetworkRelayCall =
        networkRelayCallImpl

    @Provides
    fun provideNetworkCall(
        networkRelayCallImpl: NetworkRelayCallImpl
    ): NetworkCall =
        networkRelayCallImpl

    @Provides
    @Singleton
    fun provideNetworkQueryChatImpl(
        networkRelayCall: NetworkRelayCall
    ): NetworkQueryChatImpl =
        NetworkQueryChatImpl(networkRelayCall)

    @Provides
    fun provideNetworkQueryChat(
        networkQueryChatImpl: NetworkQueryChatImpl
    ): NetworkQueryChat =
        networkQueryChatImpl

    @Provides
    @Singleton
    fun provideNetworkQueryContactImpl(
        networkRelayCall: NetworkRelayCall
    ): NetworkQueryContactImpl =
        NetworkQueryContactImpl(networkRelayCall)

    @Provides
    fun provideNetworkQueryContact(
        networkQueryContactImpl: NetworkQueryContactImpl
    ): NetworkQueryContact =
        networkQueryContactImpl

    @Provides
    @Singleton
    fun provideNetworkQueryInviteImpl(
        networkRelayCall: NetworkRelayCall,
    ): NetworkQueryInviteImpl =
        NetworkQueryInviteImpl(networkRelayCall)

    @Provides
    fun provideNetworkQueryInvite(
        networkQueryInviteImpl: NetworkQueryInviteImpl
    ): NetworkQueryInvite =
        networkQueryInviteImpl

    @Provides
    @Singleton
    fun provideNetworkQueryLightningImpl(
        networkRelayCall: NetworkRelayCall
    ): NetworkQueryLightningImpl =
        NetworkQueryLightningImpl(networkRelayCall)

    @Provides
    fun provideNetworkQueryLightning(
        networkQueryLightningImpl: NetworkQueryLightningImpl
    ): NetworkQueryLightning =
        networkQueryLightningImpl

    @Provides
    @Singleton
    fun provideNetworkQueryMessageImpl(
        networkRelayCall: NetworkRelayCall
    ): NetworkQueryMessageImpl =
        NetworkQueryMessageImpl(networkRelayCall)

    @Provides
    fun provideNetworkQueryMessage(
        networkQueryMessageImpl: NetworkQueryMessageImpl
    ): NetworkQueryMessage =
        networkQueryMessageImpl

    @Provides
    @Singleton
    fun provideNetworkQuerySubscriptionImpl(
        networkRelayCall: NetworkRelayCall
    ): NetworkQuerySubscriptionImpl =
        NetworkQuerySubscriptionImpl(networkRelayCall)

    @Provides
    fun provideNetworkQuerySubscription(
        networkQuerySubscriptionImpl: NetworkQuerySubscriptionImpl
    ): NetworkQuerySubscription =
        networkQuerySubscriptionImpl

    @Provides
    @Singleton
    fun provideNetworkQueryMemeServerImpl(
        dispatchers: CoroutineDispatchers,
        networkRelayCall: NetworkRelayCall,
    ): NetworkQueryMemeServerImpl =
        NetworkQueryMemeServerImpl(dispatchers, networkRelayCall)

    @Provides
    fun provideNetworkQueryMemeServer(
        networkQueryMemeServerImpl: NetworkQueryMemeServerImpl
    ): NetworkQueryMemeServer =
        networkQueryMemeServerImpl

    @Provides
    @Singleton
    fun provideNetworkQueryVersionImpl(
        networkRelayCall: NetworkRelayCall
    ): NetworkQueryVersionImpl =
        NetworkQueryVersionImpl(networkRelayCall)

    @Provides
    fun provideNetworkQueryVersion(
        networkQueryVersionImpl: NetworkQueryVersionImpl
    ): NetworkQueryVersion =
        networkQueryVersionImpl
}
