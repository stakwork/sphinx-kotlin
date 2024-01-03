package chat.sphinx.di

import android.content.Context
import chat.sphinx.concept_crypto_rsa.RSA
import chat.sphinx.concept_meme_input_stream.MemeInputStreamHandler
import chat.sphinx.concept_meme_server.MemeServerTokenHandler
import chat.sphinx.concept_network_query_action_track.NetworkQueryActionTrack
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_discover_tribes.NetworkQueryDiscoverTribes
import chat.sphinx.concept_network_query_invite.NetworkQueryInvite
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_meme_server.NetworkQueryMemeServer
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.concept_network_query_feed_search.NetworkQueryFeedSearch
import chat.sphinx.concept_network_query_feed_status.NetworkQueryFeedStatus
import chat.sphinx.concept_network_query_people.NetworkQueryPeople
import chat.sphinx.concept_network_query_redeem_badge_token.NetworkQueryRedeemBadgeToken
import chat.sphinx.concept_network_query_relay_keys.NetworkQueryRelayKeys
import chat.sphinx.concept_network_query_subscription.NetworkQuerySubscription
import chat.sphinx.concept_network_query_verify_external.NetworkQueryAuthorizeExternal
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_repository_actions.ActionsRepository
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_connect_manager.ConnectManagerRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_subscription.SubscriptionRepository
import chat.sphinx.concept_socket_io.SocketIOManager
import chat.sphinx.concept_wallet.WalletDataHandler
import chat.sphinx.database.SphinxCoreDBImpl
import chat.sphinx.example.concept_connect_manager.ConnectManager
import chat.sphinx.feature_coredb.CoreDBImpl
import chat.sphinx.feature_meme_server.MemeServerTokenHandlerImpl
import chat.sphinx.feature_repository.mappers.contact.toContact
import chat.sphinx.feature_repository_android.SphinxRepositoryAndroid
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.notification.SphinxNotificationManager
import chat.sphinx.wrapper_contact.Contact
import com.squareup.moshi.Moshi
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.matthewnelson.build_config.BuildConfigDebug
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideSphinxCoreDBImpl(
        @ApplicationContext appContext: Context,
        buildConfigDebug: BuildConfigDebug,
        moshi: Moshi,
    ): SphinxCoreDBImpl =
        SphinxCoreDBImpl(
            appContext,
            buildConfigDebug,
            moshi
        )

    @Provides
    fun provideCoreDBImpl(
        sphinxCoreDBImpl: SphinxCoreDBImpl
    ): CoreDBImpl =
        sphinxCoreDBImpl

    @Provides
    @Singleton
    fun provideAccountOwnerFlow(
        applicationScope: CoroutineScope,
        coreDBImpl: CoreDBImpl,
        dispatchers: CoroutineDispatchers,
    ): StateFlow<Contact?> = flow {
        emitAll(
            coreDBImpl.getSphinxDatabaseQueries().contactGetOwner()
                .asFlow()
                .mapToOneOrNull(dispatchers.io)
                .map { it?.toContact() }
        )
    }.stateIn(
        applicationScope,
        SharingStarted.WhileSubscribed(5_000),
        null
    )

    @Provides
    @Singleton
    fun provideMemeServerTokenHandlerImpl(
        accountOwner: StateFlow<Contact?>,
        applicationScope: CoroutineScope,
        authenticationStorage: AuthenticationStorage,
        dispatchers: CoroutineDispatchers,
        networkQueryMemeServer: NetworkQueryMemeServer,
        LOG: SphinxLogger,
    ): MemeServerTokenHandlerImpl =
        MemeServerTokenHandlerImpl(
            accountOwner,
            applicationScope,
            authenticationStorage,
            dispatchers,
            networkQueryMemeServer,
            LOG,
        )

    @Provides
    fun provideMemeServerTokenHandler(
        memeServerTokenHandlerImpl: MemeServerTokenHandlerImpl
    ): MemeServerTokenHandler =
        memeServerTokenHandlerImpl

    @Provides
    @Singleton
    fun provideSphinxRepositoryAndroid(
        accountOwner: StateFlow<Contact?>,
        applicationScope: CoroutineScope,
        authenticationCoreManager: AuthenticationCoreManager,
        authenticationStorage: AuthenticationStorage,
        relayDataHandler: RelayDataHandler,
        coreDBImpl: CoreDBImpl,
        dispatchers: CoroutineDispatchers,
        moshi: Moshi,
        mediaCacheHandler: MediaCacheHandler,
        memeInputStreamHandler: MemeInputStreamHandler,
        memeServerTokenHandler: MemeServerTokenHandler,
        networkQueryMemeServer: NetworkQueryMemeServer,
        networkQueryActionTrack: NetworkQueryActionTrack,
        networkQueryDiscoverTribes: NetworkQueryDiscoverTribes,
        networkQueryChat: NetworkQueryChat,
        networkQueryContact: NetworkQueryContact,
        networkQueryLightning: NetworkQueryLightning,
        networkQueryMessage: NetworkQueryMessage,
        networkQueryInvite: NetworkQueryInvite,
        networkQueryAuthorizeExternal: NetworkQueryAuthorizeExternal,
        networkQueryPeople: NetworkQueryPeople,
        networkQueryRedeemBadgeToken: NetworkQueryRedeemBadgeToken,
        networkQuerySubscription: NetworkQuerySubscription,
        networkQueryFeedSearch: NetworkQueryFeedSearch,
        networkQueryRelayKeys: NetworkQueryRelayKeys,
        networkQueryFeedStatus: NetworkQueryFeedStatus,
        connectManager: ConnectManager,
        walletDataHandler: WalletDataHandler,
        rsa: RSA,
        socketIOManager: SocketIOManager,
        sphinxNotificationManager: SphinxNotificationManager,
        sphinxLogger: SphinxLogger,
    ): SphinxRepositoryAndroid =
        SphinxRepositoryAndroid(
            accountOwner,
            applicationScope,
            authenticationCoreManager,
            authenticationStorage,
            relayDataHandler,
            coreDBImpl,
            dispatchers,
            moshi,
            mediaCacheHandler,
            memeInputStreamHandler,
            memeServerTokenHandler,
            networkQueryActionTrack,
            networkQueryDiscoverTribes,
            networkQueryMemeServer,
            networkQueryChat,
            networkQueryContact,
            networkQueryLightning,
            networkQueryMessage,
            networkQueryInvite,
            networkQueryAuthorizeExternal,
            networkQueryPeople,
            networkQueryRedeemBadgeToken,
            networkQuerySubscription,
            networkQueryFeedSearch,
            networkQueryRelayKeys,
            networkQueryFeedStatus,
            connectManager,
            walletDataHandler,
            rsa,
            socketIOManager,
            sphinxNotificationManager,
            sphinxLogger,
        )

    @Provides
    fun provideConnectManagerRepository(
        sphinxRepositoryAndroid: SphinxRepositoryAndroid
    ): ConnectManagerRepository =
        sphinxRepositoryAndroid

    @Provides
    fun provideChatRepository(
        sphinxRepositoryAndroid: SphinxRepositoryAndroid
    ): ChatRepository =
        sphinxRepositoryAndroid

    @Provides
    fun provideContactRepository(
        sphinxRepositoryAndroid: SphinxRepositoryAndroid
    ): ContactRepository =
        sphinxRepositoryAndroid

    @Provides
    fun provideLightningRepository(
        sphinxRepositoryAndroid: SphinxRepositoryAndroid
    ): LightningRepository =
        sphinxRepositoryAndroid

    @Provides
    fun provideMessageRepository(
        sphinxRepositoryAndroid: SphinxRepositoryAndroid
    ): MessageRepository =
        sphinxRepositoryAndroid

    @Provides
    fun provideSubscriptionRepository(
        sphinxRepositoryAndroid: SphinxRepositoryAndroid
    ): SubscriptionRepository =
        sphinxRepositoryAndroid

    @Provides
    fun provideFeedRepository(
        sphinxRepositoryAndroid: SphinxRepositoryAndroid
    ): FeedRepository =
        sphinxRepositoryAndroid

    @Provides
    @Suppress("UNCHECKED_CAST")
    fun provideRepositoryDashboardAndroid(
        sphinxRepositoryAndroid: SphinxRepositoryAndroid
    ): RepositoryDashboardAndroid<Any> =
        sphinxRepositoryAndroid as RepositoryDashboardAndroid<Any>

    @Provides
    fun provideRepositoryMedia(
        sphinxRepositoryAndroid: SphinxRepositoryAndroid
    ): RepositoryMedia =
        sphinxRepositoryAndroid

    @Provides
    fun provideActionsRepository(
        sphinxRepositoryAndroid: SphinxRepositoryAndroid
    ): ActionsRepository =
        sphinxRepositoryAndroid

}

