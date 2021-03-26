package chat.sphinx.feature_coredb

import chat.sphinx.concept_coredb.SphinxCoreDB
import chat.sphinx.concept_coredb.SphinxDatabase
import chat.sphinx.conceptcoredb.ChatDbo
import chat.sphinx.conceptcoredb.SphinxDatabaseQueries
import chat.sphinx.feature_coredb.adapters.chat.*
import chat.sphinx.feature_coredb.adapters.common.*
import chat.sphinx.feature_coredb.adapters.contact.ContactIdsAdapter
import com.squareup.sqldelight.db.SqlDriver
import io.matthewnelson.concept_encryption_key.EncryptionKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

abstract class SphinxCoreDBImpl: SphinxCoreDB() {

    companion object {
        const val DB_NAME = "sphinx.db"
    }

    @Volatile
    private var sphinxDatabaseQueries: SphinxDatabaseQueries? = null

    val isInitialized: Boolean
        get() = sphinxDatabaseQueries != null

    protected fun setDatabaseQueries(sqlDriver: SqlDriver) {
        if (!isInitialized) {
            sphinxDatabaseQueries = SphinxDatabase(
                driver = sqlDriver,
                chatDboAdapter = ChatDbo.Adapter(
                    idAdapter = ChatIdAdapter(),
                    uuidAdapter = ChatUUIDAdapter(),
                    nameAdapter = ChatNameAdapter(),
                    photo_urlAdapter = PhotoUrlAdapter.getInstance(),
                    typeAdapter = ChatTypeAdapter(),
                    statusAdapter = ChatStatusAdapter(),
                    contact_idsAdapter = ContactIdsAdapter(),
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
                    pending_contact_idsAdapter = ContactIdsAdapter(),
                )
            ).sphinxDatabaseQueries
        }
    }

    @Throws(CancellationException::class)
    override suspend fun getSphinxDatabaseQueries(): SphinxDatabaseQueries {
        var queries = sphinxDatabaseQueries
        while (queries == null) {
            // This _never_ fires b/c by the time the dashboard is navigated
            // to, initialization is already complete.
            currentCoroutineContext().ensureActive()
            delay(15L)
            queries = sphinxDatabaseQueries
        }
        return queries
    }

    abstract fun initializeDatabase(encryptionKey: EncryptionKey)
}
