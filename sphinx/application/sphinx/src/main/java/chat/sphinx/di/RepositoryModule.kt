package chat.sphinx.di

import chat.sphinx.concept_crypto_rsa.RSA
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.database.SphinxCoreDBImpl
import chat.sphinx.feature_coredb.CoreDBImpl
import chat.sphinx.feature_repository.SphinxRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
        coreDBImpl: CoreDBImpl,
        dispatchers: CoroutineDispatchers,
        networkQueryChat: NetworkQueryChat,
        networkQueryMessage: NetworkQueryMessage,
        rsa: RSA,
    ): SphinxRepository =
        SphinxRepository(
            authenticationCoreManager,
            coreDBImpl,
            dispatchers,
            networkQueryChat,
            networkQueryMessage,
            rsa
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
