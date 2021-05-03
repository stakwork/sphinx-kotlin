package chat.sphinx.di

import chat.sphinx.concept_crypto_rsa.RSA
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_socket_io.SocketIOManager
import chat.sphinx.database.SphinxCoreDBImpl
import chat.sphinx.feature_coredb.CoreDBImpl
import chat.sphinx.feature_repository.SphinxRepository
import chat.sphinx.logger.SphinxLogger
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    fun provideCoreDBImpl(
        sphinxCoreDBImpl: SphinxCoreDBImpl
    ): CoreDBImpl =
        sphinxCoreDBImpl

    @Provides
    @Singleton
    fun provideSphinxRepository(
        authenticationCoreManager: AuthenticationCoreManager,
        authenticationStorage: AuthenticationStorage,
        coreDBImpl: CoreDBImpl,
        dispatchers: CoroutineDispatchers,
        moshi: Moshi,
        networkQueryChat: NetworkQueryChat,
        networkQueryContact: NetworkQueryContact,
        networkQueryLightning: NetworkQueryLightning,
        networkQueryMessage: NetworkQueryMessage,
        socketIOManager: SocketIOManager,
        rsa: RSA,
        sphinxLogger: SphinxLogger,
    ): SphinxRepository =
        SphinxRepository(
            authenticationCoreManager,
            authenticationStorage,
            coreDBImpl,
            dispatchers,
            moshi,
            networkQueryChat,
            networkQueryContact,
            networkQueryLightning,
            networkQueryMessage,
            rsa,
            socketIOManager,
            sphinxLogger,
        )

    @Provides
    fun provideChatRepository(
        sphinxRepository: SphinxRepository
    ): ChatRepository =
        sphinxRepository

    @Provides
    fun provideContactRepository(
        sphinxRepository: SphinxRepository
    ): ContactRepository =
        sphinxRepository

    @Provides
    fun provideLightningRepository(
        sphinxRepository: SphinxRepository
    ): LightningRepository =
        sphinxRepository

    @Provides
    fun provideMessageRepository(
        sphinxRepository: SphinxRepository
    ): MessageRepository =
        sphinxRepository
}
