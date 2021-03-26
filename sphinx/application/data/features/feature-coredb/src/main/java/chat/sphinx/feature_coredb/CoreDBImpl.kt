package chat.sphinx.feature_coredb

import chat.sphinx.concept_coredb.CoreDB
import chat.sphinx.concept_coredb.SphinxDatabase
import chat.sphinx.conceptcoredb.ChatDbo
import chat.sphinx.conceptcoredb.SphinxDatabaseQueries
import chat.sphinx.feature_coredb.adapters.chat.*
import chat.sphinx.feature_coredb.adapters.common.*
import chat.sphinx.feature_coredb.adapters.contact.ContactIdsAdapter
import com.squareup.sqldelight.db.SqlDriver
import io.matthewnelson.concept_encryption_key.EncryptionKey
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

abstract class CoreDBImpl: CoreDB() {

    companion object {
        const val DB_NAME = "sphinx.db"
    }

    private val sphinxDatabaseQueriesStateFlow: MutableStateFlow<SphinxDatabaseQueries?> =
        MutableStateFlow(null)

    protected abstract fun getSqlDriver(encryptionKey: EncryptionKey): SqlDriver

    private val initializationLock = Object()

    fun initializeDatabase(encryptionKey: EncryptionKey) {
        if (sphinxDatabaseQueriesStateFlow.value != null) {
            return
        }

        synchronized(initializationLock) {

            if (sphinxDatabaseQueriesStateFlow.value != null) {
                return
            }

            sphinxDatabaseQueriesStateFlow.value = SphinxDatabase(
                driver = getSqlDriver(encryptionKey),
                chatDboAdapter = ChatDbo.Adapter(
                    idAdapter = ChatIdAdapter(),
                    uuidAdapter = ChatUUIDAdapter(),
                    nameAdapter = ChatNameAdapter(),
                    photo_urlAdapter = PhotoUrlAdapter.getInstance(),
                    typeAdapter = ChatTypeAdapter(),
                    statusAdapter = ChatStatusAdapter(),
                    contact_idsAdapter = ContactIdsAdapter.getInstance(),
                    is_mutedAdapter = ChatMutedAdapter(),
                    created_atAdapter = DateTimeAdapter.getInstance(),
                    group_keyAdapter = ChatGroupKeyAdapter(),
                    hostAdapter = ChatHostAdapter(),
                    price_per_messageAdapter = SatAdapter.getInstance(),
                    escrow_amountAdapter = SatAdapter.getInstance(),
                    unlistedAdapter = ChatUnlistedAdapter(),
                    private_tribeAdapter = ChatPrivateAdapter(),
                    owner_pub_keyAdapter = LightningNodePubKeyAdapter.getInstance(),
                    seenAdapter = SeenAdapter.getInstance(),
                    meta_dataAdapter = ChatMetaDataAdapter(),
                    my_photo_urlAdapter = PhotoUrlAdapter.getInstance(),
                    my_aliasAdapter = ChatAliasAdapter(),
                    pending_contact_idsAdapter = ContactIdsAdapter.getInstance(),
                )
            ).sphinxDatabaseQueries
        }
    }

    private class Hackery(val hack: SphinxDatabaseQueries): Exception()

    override suspend fun getSphinxDatabaseQueries(): SphinxDatabaseQueries {
        sphinxDatabaseQueriesStateFlow.value?.let { queries ->
            return queries
        }

        var queries: SphinxDatabaseQueries? = null

        try {
            sphinxDatabaseQueriesStateFlow.collect { queriesState ->
                if (queriesState != null) {
                    queries = queriesState
                    throw Hackery(queriesState)
                }
            }
        } catch (e: Hackery) {
            return e.hack
        }

        // Will never make it here, but to please the IDE just in case...
        delay(25L)
        return queries!!
    }
}
