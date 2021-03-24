package chat.sphinx.feature_repository

//import app.cash.exhaustive.Exhaustive
//import chat.sphinx.feature_repository.adapters.chat.*
//import chat.sphinx.feature_repository.adapters.common.*
//import chat.sphinx.feature_repository.adapters.contact.ContactIdsAdapter
//import chat.sphinx.featurecoredb.ChatDbo
//import chat.sphinx.kotlin_response.KotlinResponse
//import chat.sphinx.kotlin_response.LoadResponse
//import chat.sphinx.test_network_query.NetworkQueryTestHelper
//import chat.sphinx.wrapper_chat.Chat
//import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.collect
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.test.runBlockingTest
//import org.junit.After
//import org.junit.Assert
//import org.junit.Test
//
//// TODO: java.lang.NoSuchFieldError: Companion
////  Error attributed to dependency mismatch for Okio using the JvmDriver
////  run: ./gradlew :sphinx:application:data:features:feature-coredb:dependencyInsight --configuration testRuntimeClasspath --dependency okio
////  only thing that can be done is to wait for Moshi to upgrade to kotlin.
//class SphinxRepositoryUnitTest: NetworkQueryTestHelper() {
//
//    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
//    private val repository: SphinxRepository by lazy {
//        SphinxRepository(
//            dispatchers,
//            nqChat,
//            SphinxDatabase(
//                driver = driver,
//                chatDboAdapter = ChatDbo.Adapter(
//                    idAdapter = ChatIdAdapter(),
//                    uuidAdapter = ChatUUIDAdapter(),
//                    nameAdapter = ChatNameAdapter(),
//                    photo_urlAdapter = PhotoUrlAdapter.getInstance(),
//                    typeAdapter = ChatTypeAdapter(),
//                    statusAdapter = ChatStatusAdapter(),
//                    contact_idsAdapter = ContactIdsAdapter(),
//                    is_mutedAdapter = ChatMutedAdapter(),
//                    created_atAdapter = DateTimeAdapter.getInstance(),
//                    group_keyAdapter = ChatGroupKeyAdapter(),
//                    hostAdapter = ChatHostAdapter(),
//                    price_per_messageAdapter = SatAdapter.getInstance(),
//                    escrow_amountAdapter = SatAdapter.getInstance(),
//                    unlistedAdapter = ChatUnlistedAdapter(),
//                    private_tribeAdapter = ChatPrivateAdapter(),
//                    owner_pub_keyAdapter = LightningNodePubKeyAdapter.getInstance(),
//                    seenAdapter = SeenAdapter.getInstance(),
//                    meta_dataAdapter = ChatMetaDataAdapter(),
//                    my_photo_urlAdapter = PhotoUrlAdapter.getInstance(),
//                    my_aliasAdapter = ChatAliasAdapter(),
//                    pending_contact_idsAdapter = ContactIdsAdapter(),
//                )
//            ).sphinxDatabaseQueries
//        )
//    }
//
//    @After
//    fun tearDownTest() {
//        driver.close()
//    }
//
//    @Test
//    fun `networkRefresh updates DB`() =
//        testDispatcher.runBlockingTest {
//            getCredentials()?.let {
//                var chats: List<Chat> = listOf()
//                val getChatsJob = launch {
//                    repository.getChats().collect {
//                        chats = it
//                    }
//                }
//                delay(10L)
//                Assert.assertTrue(chats.isEmpty())
//
//                repository.networkRefreshChats().collect { loadResponse ->
//
//                    @Exhaustive
//                    when (loadResponse) {
//                        is KotlinResponse.Error -> {
//                            getChatsJob.cancel()
//                            Assert.fail()
//                        }
//                        is KotlinResponse.Success -> {
//                            delay(10L)
//                            Assert.assertTrue(chats.isNotEmpty())
//                        }
//                        is LoadResponse.Loading -> {}
//                    }
//                }
//
//                getChatsJob.cancel()
//            }
//        }
//}
