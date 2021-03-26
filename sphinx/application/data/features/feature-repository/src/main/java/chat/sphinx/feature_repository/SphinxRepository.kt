package chat.sphinx.feature_repository

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_coredb.CoreDB
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.feature_repository.mappers.chat.ChatMapper
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
import java.text.ParseException

class SphinxRepository(
    private val coreDB: CoreDB,
    private val dispatchers: CoroutineDispatchers,
    private val networkQueryChat: NetworkQueryChat,
): ChatRepository, MessageRepository {

    /////////////
    /// Chats ///
    /////////////
    private val chatLock = Mutex()
    private val chatMapper: ChatMapper by lazy {
        ChatMapper()
    }

    override suspend fun getChats(): Flow<List<Chat>> {
        return coreDB.getSphinxDatabaseQueries().getAllChats()
            .asFlow()
            .flowOn(dispatchers.io)
            .mapToList(dispatchers.default)
            .map { chatMapper.fromDBOsToPresenters(it) }
            .flowOn(dispatchers.default)
    }

    override suspend fun getChatById(chatId: ChatId): Flow<Chat?> {
        return coreDB.getSphinxDatabaseQueries().getChatById(chatId)
            .asFlow()
            .flowOn(dispatchers.io)
            .mapToOneOrNull(dispatchers.default)
            .map { it?.let { chatMapper.fromDBOtoPresenter(it) } }
            .distinctUntilChanged()
    }

    override suspend fun getChatByUUID(chatUUID: ChatUUID): Flow<Chat?> {
        return coreDB.getSphinxDatabaseQueries().getChatByUUID(chatUUID)
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

                    try {

                        withContext(dispatchers.io) {

                            chatLock.withLock {

                                val queries = coreDB.getSphinxDatabaseQueries()
                                val chatIdsToRemove = queries.getAllChatIds()
                                    .executeAsList()
                                    .toMutableSet()

                                chatMapper.fromDTOsToDBOs(loadResponse.value).let { dbos ->

                                    queries.transaction {
                                        dbos.forEach { dbo ->
                                            queries.upsertChat(
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
                                            queries.deleteChatById(chatId)
                                        }
                                    }
                                }
                            }
                        }

                        emit(KotlinResponse.Success(true))

                    } catch (e: IllegalArgumentException) {
                        emit(
                            KotlinResponse.Error(
                                ResponseError("Failed to convert Json from Relay", e)
                            )
                        )
                    } catch (e: ParseException) {
                        emit(
                            KotlinResponse.Error(
                                ResponseError("Failed to convert date/time from SphinxRelay", e)
                            )
                        )
                    }
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
