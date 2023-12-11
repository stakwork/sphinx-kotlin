package chat.sphinx.feature_repository_android

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import chat.sphinx.concept_coredb.CoreDB
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
import chat.sphinx.concept_paging.PageSourceWrapper
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_repository_dashboard.DashboardItem
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.concept_socket_io.SocketIOManager
import chat.sphinx.concept_wallet.WalletDataHandler
import chat.sphinx.conceptcoredb.DashboardDbo
import chat.sphinx.conceptcoredb.SphinxDatabaseQueries
import chat.sphinx.example.concept_connect_manager.ConnectManager
import chat.sphinx.feature_repository.SphinxRepository
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.notification.SphinxNotificationManager
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.dashboard.InviteId
import chat.sphinx.wrapper_contact.Contact
import com.squareup.moshi.Moshi
import com.squareup.sqldelight.android.paging3.QueryPagingSource
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

class SphinxRepositoryAndroid(
    accountOwner: StateFlow<Contact?>,
    applicationScope: CoroutineScope,
    authenticationCoreManager: AuthenticationCoreManager,
    authenticationStorage: AuthenticationStorage,
    relayDataHandler: RelayDataHandler,
    coreDB: CoreDB,
    dispatchers: CoroutineDispatchers,
    moshi: Moshi,
    mediaCacheHandler: MediaCacheHandler,
    memeInputStreamHandler: MemeInputStreamHandler,
    memeServerTokenHandler: MemeServerTokenHandler,
    networkQueryActionTrack: NetworkQueryActionTrack,
    networkQueryDiscoverTribes: NetworkQueryDiscoverTribes,
    networkQueryMemeServer: NetworkQueryMemeServer,
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
    LOG: SphinxLogger,
): SphinxRepository(
    accountOwner,
    applicationScope,
    authenticationCoreManager,
    authenticationStorage,
    relayDataHandler,
    coreDB,
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
    LOG,
), RepositoryDashboardAndroid<DashboardDbo>
{
    companion object {
        const val PAGING_DASHBOARD_PAGE_SIZE = 30
        const val PAGING_DASHBOARD_PREFETCH_DISTANCE = PAGING_DASHBOARD_PAGE_SIZE / 2
        const val PAGING_DASHBOARD_INITIAL_LOAD_SIZE = PAGING_DASHBOARD_PREFETCH_DISTANCE
        const val PAGING_DASHBOARD_MAX_SIZE =
            (PAGING_DASHBOARD_PREFETCH_DISTANCE * 2) + PAGING_DASHBOARD_PAGE_SIZE
    }

    private inner class DashboardPageSourceWrapper(
        private val queries: SphinxDatabaseQueries,
        initialSource: PagingSource<Long, DashboardDbo>
    ): PageSourceWrapper<Long, DashboardItem, DashboardDbo>(initialSource) {
        override val config: PagingConfig by lazy {
            PagingConfig(
                pageSize = PAGING_DASHBOARD_PAGE_SIZE,
                prefetchDistance = PAGING_DASHBOARD_PREFETCH_DISTANCE,
                initialLoadSize = PAGING_DASHBOARD_INITIAL_LOAD_SIZE,
                maxSize = PAGING_DASHBOARD_MAX_SIZE,
            )
        }

        override fun createNewPagerSource(): PagingSource<Long, DashboardDbo> {
            return createNewDashboardItemPagingSource(queries)
        }

        // TODO: Rework mapping from DBO -> Presenter after DashboardItem
        //  gets re-worked.
        override suspend fun mapOriginal(original: DashboardDbo): DashboardItem {
            return when (val id = original.id) {
                is ChatId -> {
                    original.contact_id?.let { contactId ->
                        DashboardItem.Active.Conversation(
                            id,
                            contactId,
                            original.latest_message_id
                        )
                    } ?: DashboardItem.Active.GroupOrTribe(id, original.latest_message_id)
                }
                is ContactId -> {
                    DashboardItem.Inactive.Conversation(id)
                }
                is InviteId -> {
                    DashboardItem.Inactive.PendingInvite(id)
                }
            }
        }
    }

    override suspend fun getDashboardItemPagingSource(): PageSourceWrapper<Long, DashboardItem, DashboardDbo> {
        val queries = coreDB.getSphinxDatabaseQueries()
        return DashboardPageSourceWrapper(
            queries,
            createNewDashboardItemPagingSource(queries),
        )
    }

    private fun createNewDashboardItemPagingSource(
        queries: SphinxDatabaseQueries
    ): PagingSource<Long, DashboardDbo> =
        QueryPagingSource(
            countQuery = queries.dashboardCount(),
            transacter = queries,
            dispatcher = io,
            queryProvider = queries::dashboardPagination
        )

}
