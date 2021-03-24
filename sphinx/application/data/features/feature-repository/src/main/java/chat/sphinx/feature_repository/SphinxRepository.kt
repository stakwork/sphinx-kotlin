package chat.sphinx.feature_repository

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.feature_repository.mappers.chat.ChatMapper
import chat.sphinx.featurerepository.SphinxDatabaseQueries
import chat.sphinx.kotlin_response.KotlinResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatUUID
import chat.sphinx.wrapper_common.chat.ChatId
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SphinxRepository(
    private val dispatchers: CoroutineDispatchers,
    private val networkQueryChat: NetworkQueryChat,
    private val sphinxDatabaseQueries: SphinxDatabaseQueries,
): ChatRepository, MessageRepository {

    companion object {
        const val DB_NAME = "sphinx.db"
    }

    /////////////
    /// Chats ///
    /////////////
    private val chatLock = Mutex()
    private val chatMapper: ChatMapper by lazy {
        ChatMapper()
    }

    override fun getChats(): Flow<List<Chat>> {
        return sphinxDatabaseQueries.getAllChats()
            .asFlow()
            .flowOn(dispatchers.io)
            .mapToList(dispatchers.default)
            .map { chatMapper.fromDBOsToPresenters(it) }
            .flowOn(dispatchers.default)
    }

    override fun getChatById(chatId: ChatId): Flow<Chat?> {
        return sphinxDatabaseQueries.getChatById(chatId)
            .asFlow()
            .flowOn(dispatchers.io)
            .mapToOneOrNull(dispatchers.default)
            .map { it?.let { chatMapper.fromDBOtoPresenter(it) } }
            .distinctUntilChanged()
    }

    override fun getChatByUUID(chatUUID: ChatUUID): Flow<Chat?> {
        return sphinxDatabaseQueries.getChatByUUID(chatUUID)
            .asFlow()
            .flowOn(dispatchers.io)
            .mapToOneOrNull(dispatchers.default)
            .map { it?.let { chatMapper.fromDBOtoPresenter(it) } }
            .distinctUntilChanged()
    }

    override fun networkRefreshChats(): Flow<LoadResponse<Boolean, ResponseError>> = flow {
        networkQueryChat.getChats().collect { loadResponse ->

            @Exhaustive
            when (loadResponse) {
                is KotlinResponse.Error -> {
                    emit(loadResponse)
                }
                is KotlinResponse.Success -> {
                    withContext(dispatchers.io) {

                        chatLock.withLock {

                            val chatIdsToRemove = sphinxDatabaseQueries.getAllChatIds()
                                .executeAsList()
                                .toMutableSet()

                            chatMapper.fromDTOsToDBOs(loadResponse.value).let { dbos ->

                                sphinxDatabaseQueries.transaction {
                                    dbos.forEach { dbo ->
                                        sphinxDatabaseQueries.upsertChat(
                                            dbo.uuid,
                                            dbo.name,
                                            dbo.photo_url,
                                            dbo.type,
                                            dbo.status,
                                            dbo.contact_ids,
                                            dbo.is_muted,
                                            dbo.created_at,
                                            dbo.group_key,
                                            dbo.host,
                                            dbo.price_per_message,
                                            dbo.escrow_amount,
                                            dbo.unlisted,
                                            dbo.private_tribe,
                                            dbo.owner_pub_key,
                                            dbo.seen,
                                            dbo.meta_data,
                                            dbo.my_photo_url,
                                            dbo.my_alias,
                                            dbo.pending_contact_ids,
                                            dbo.id
                                        )

                                        chatIdsToRemove.remove(dbo.id)
                                    }

                                    // remove remaining chat's from DB
                                    chatIdsToRemove.forEach { chatId ->
                                        sphinxDatabaseQueries.deleteChatById(chatId)
                                    }
                                }
                            }
                        }
                    }

                    emit(KotlinResponse.Success(true))
                }
                is LoadResponse.Loading -> {
                    emit(loadResponse)
                }
            }

        }
    }

    ////////////////
    /// Messages ///
    ////////////////
}
