package chat.sphinx.feature_coredb

import chat.sphinx.concept_coredb.CoreDB
import chat.sphinx.concept_coredb.SphinxDatabase
import chat.sphinx.conceptcoredb.ChatDbo
import chat.sphinx.conceptcoredb.ContactDbo
import chat.sphinx.conceptcoredb.MessageDbo
import chat.sphinx.conceptcoredb.SphinxDatabaseQueries
import chat.sphinx.feature_coredb.adapters.chat.*
import chat.sphinx.feature_coredb.adapters.common.*
import chat.sphinx.feature_coredb.adapters.contact.*
import chat.sphinx.feature_coredb.adapters.contact.ContactAliasAdapter
import chat.sphinx.feature_coredb.adapters.contact.ContactOwnerAdapter
import chat.sphinx.feature_coredb.adapters.contact.LightningNodeAliasAdapter
import chat.sphinx.feature_coredb.adapters.contact.LightningRouteHintAdapter
import chat.sphinx.feature_coredb.adapters.contact.PrivatePhotoAdapter
import chat.sphinx.feature_coredb.adapters.message.*
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

    override val isInitialized: Boolean
        get() = sphinxDatabaseQueriesStateFlow.value != null

    protected abstract fun getSqlDriver(encryptionKey: EncryptionKey): SqlDriver

    private val initializationLock = Object()

    fun initializeDatabase(encryptionKey: EncryptionKey) {
        if (isInitialized) {
            return
        }

        synchronized(initializationLock) {

            if (isInitialized) {
                return
            }

            sphinxDatabaseQueriesStateFlow.value = SphinxDatabase(
                driver = getSqlDriver(encryptionKey),
                chatDboAdapter = ChatDbo.Adapter(
                    idAdapter = ChatIdAdapter.getInstance(),
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
                    latest_message_idAdapter = MessageIdAdapter.getInstance(),
                ),
                messageDboAdapter = MessageDbo.Adapter(
                    idAdapter = MessageIdAdapter.getInstance(),
                    uuidAdapter = MessageUUIDAdapter(),
                    chat_idAdapter = ChatIdAdapter.getInstance(),
                    typeAdapter = MessageTypeAdapter(),
                    senderAdapter = ContactIdAdapter.getInstance(),
                    receiverAdapter = ContactIdAdapter.getInstance(),
                    amountAdapter = SatAdapter.getInstance(),
                    payment_hashAdapter = LightningPaymentHashAdapter.getInstance(),
                    payment_requestAdapter = LightningPaymentRequestAdapter.getInstance(),
                    dateAdapter = DateTimeAdapter.getInstance(),
                    expiration_dateAdapter = DateTimeAdapter.getInstance(),
                    message_contentAdapter = MessageContentAdapter(),
                    message_content_decryptedAdapter = MessageContentDecryptedAdapter(),
                    statusAdapter = MessageStatusAdapter(),
                    media_keyAdapter = MediaKeyAdapter(),
                    media_typeAdapter = MediaTypeAdapter(),
                    media_tokenAdapter = MediaTokenAdapter(),
                    seenAdapter = SeenAdapter.getInstance(),
                    sender_aliasAdapter = SenderAliasAdapter(),
                    sender_picAdapter = PhotoUrlAdapter.getInstance(),
                    original_muidAdapter = MessageMUIDAdapter(),
                    reply_uuidAdapter = ReplyUUIDAdapter(),
                ),
                contactDboAdapter = ContactDbo.Adapter(
                    idAdapter = ContactIdAdapter.getInstance(),
                    route_hintAdapter = LightningRouteHintAdapter(),
                    node_pub_keyAdapter = LightningNodePubKeyAdapter.getInstance(),
                    node_aliasAdapter = LightningNodeAliasAdapter(),
                    aliasAdapter = ContactAliasAdapter(),
                    photo_urlAdapter = PhotoUrlAdapter.getInstance(),
                    private_photoAdapter = PrivatePhotoAdapter(),
                    ownerAdapter = ContactOwnerAdapter(),
                    statusAdapter = ContactStatusAdapter(),
                    public_keyAdapter = ContactPublicKeyAdapter(),
                    device_idAdapter = DeviceIdAdapter(),
                    created_atAdapter = DateTimeAdapter.getInstance(),
                    updated_atAdapter = DateTimeAdapter.getInstance(),
                    notification_soundAdapter = NotificationSoundAdapter(),
                    tip_amountAdapter = SatAdapter.getInstance(),
                    invite_idAdapter = InviteIdAdapter.getInstance(),
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
