package chat.sphinx.di

import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.database.SphinxCoreDBImpl
import chat.sphinx.feature_repository.SphinxRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    fun provideSphinxCoreDBImpl(
        sphinxCoreDBImpl: SphinxCoreDBImpl
    ): SphinxCoreDBImpl =
        sphinxCoreDBImpl

    @Provides
    @Singleton
    fun provideSphinxRepository(
        dispatchers: CoroutineDispatchers,
        networkQueryChat: NetworkQueryChat,
        sphinxCoreDBImpl: SphinxCoreDBImpl,
    ): SphinxRepository =
        SphinxRepository(
            sphinxCoreDBImpl,
            dispatchers,
            networkQueryChat,
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
