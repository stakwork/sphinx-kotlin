package chat.sphinx.di

import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.database.SphinxCoreDBImplAndroid
import chat.sphinx.feature_coredb.SphinxCoreDBImpl
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
        sphinxCoreDBImplAndroid: SphinxCoreDBImplAndroid
    ): SphinxCoreDBImpl =
        sphinxCoreDBImplAndroid

    @Provides
    @Singleton
    fun provideSphinxRepository(
        dispatchers: CoroutineDispatchers,
        networkQueryChat: NetworkQueryChat,
        sphinxCoreDBImplAndroid: SphinxCoreDBImplAndroid,
    ): SphinxRepository =
        SphinxRepository(
            dispatchers,
            networkQueryChat,
            sphinxCoreDBImplAndroid,
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
