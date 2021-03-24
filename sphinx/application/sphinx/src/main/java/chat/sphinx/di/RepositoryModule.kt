package chat.sphinx.di

import android.content.Context
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.feature_repository.SphinxRepository
import chat.sphinx.feature_repository.SphinxDatabase
import chat.sphinx.feature_repository.adapters.chat.*
import chat.sphinx.feature_repository.adapters.common.*
import chat.sphinx.feature_repository.adapters.contact.ContactIdsAdapter
import chat.sphinx.featurerepository.ChatDbo
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideSqlDriver(
        @ApplicationContext appContext: Context
    ): SqlDriver =
        AndroidSqliteDriver(
            SphinxDatabase.Schema,
            appContext,
            SphinxRepository.DB_NAME
        )

    @Provides
    @Singleton
    fun provideSphinxDatabase(
        sqlDriver: SqlDriver
    ): SphinxDatabase =
        SphinxDatabase(
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
        )

    @Provides
    @Singleton
    fun provideSphinxRepository(
        dispatchers: CoroutineDispatchers,
        networkQueryChat: NetworkQueryChat,
        sphinxDatabase: SphinxDatabase,
    ): SphinxRepository =
        SphinxRepository(
            dispatchers,
            networkQueryChat,
            sphinxDatabase.sphinxDatabaseQueries
        )

    @Provides
    fun provideChatRepository(
        sphinxRepository: SphinxRepository
    ): ChatRepository =
        sphinxRepository

    @Provides
    fun provideMessageRepository(
        sphinxRepository: SphinxRepository
    ): MessageRepository =
        sphinxRepository
}