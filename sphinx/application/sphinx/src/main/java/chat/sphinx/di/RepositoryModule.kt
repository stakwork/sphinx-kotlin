package chat.sphinx.di

import android.content.Context
import chat.sphinx.concept_coredb.CoreDB
import chat.sphinx.concept_crypto_rsa.RSA
import chat.sphinx.concept_network_query_attachment.NetworkQueryAttachment
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_socket_io.SocketIOManager
import chat.sphinx.database.SphinxCoreDBImpl
import chat.sphinx.feature_coredb.CoreDBImpl
import chat.sphinx.feature_repository.mappers.contact.toContact
import chat.sphinx.feature_repository_android.SphinxRepositoryAndroid
import chat.sphinx.logger.SphinxLogger
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
    fun provideSphinxRepositoryAndroid(
        accountOwner: StateFlow<Contact?>,
        applicationScope: CoroutineScope,
        authenticationCoreManager: AuthenticationCoreManager,
        authenticationStorage: AuthenticationStorage,
        coreDBImpl: CoreDBImpl,
        dispatchers: CoroutineDispatchers,
        moshi: Moshi,
        networkQueryChat: NetworkQueryChat,
        networkQueryContact: NetworkQueryContact,
        networkQueryLightning: NetworkQueryLightning,
        networkQueryMessage: NetworkQueryMessage,
        networkQueryAttachment: NetworkQueryAttachment,
        socketIOManager: SocketIOManager,
        rsa: RSA,
        sphinxLogger: SphinxLogger,
    ): SphinxRepositoryAndroid =
        SphinxRepositoryAndroid(
            accountOwner,
            applicationScope,
            authenticationCoreManager,
            authenticationStorage,
            coreDBImpl,
            dispatchers,
            moshi,
            networkQueryChat,
            networkQueryContact,
            networkQueryLightning,
            networkQueryMessage,
            networkQueryAttachment,
            rsa,
            socketIOManager,
            sphinxLogger,
        )

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
}
