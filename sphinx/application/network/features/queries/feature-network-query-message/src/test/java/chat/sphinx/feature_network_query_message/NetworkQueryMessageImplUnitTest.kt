package chat.sphinx.feature_network_query_message

import app.cash.exhaustive.Exhaustive
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.exception
import chat.sphinx.kotlin_response.message
import chat.sphinx.test_network_query.NetworkQueryTestHelper
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.message.MessagePagination
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test

class NetworkQueryMessageImplUnitTest: NetworkQueryTestHelper() {

    @Test
    fun `getMessages returns success`() =
        testDispatcher.runBlockingTest {
            getCredentials()?.let {
                // get all available messages
                nqMessage.getMessages(null).collect { loadResponse ->

                    @Exhaustive
                    when (loadResponse) {
                        is Response.Error -> {
                            loadResponse.exception?.printStackTrace()
                            Assert.fail(loadResponse.message)
                        }
                        is Response.Success -> {}
                        is LoadResponse.Loading -> {}
                    }

                }

            }
        }

    @Test
    fun `pagination returns correct number when limited`() =
        testDispatcher.runBlockingTest {
            getCredentials()?.let {

                val expectedListSize = 10
                val paginationParams = MessagePagination.instantiate(
                    limit = expectedListSize,
                    offset = 0,
                    date = null
                )

                nqMessage.getMessages(paginationParams).collect { loadResponse ->

                    @Exhaustive
                    when (loadResponse) {
                        is Response.Error -> {
                            loadResponse.exception?.printStackTrace()
                            Assert.fail(loadResponse.message)
                        }
                        is Response.Success -> {
                            Assert.assertEquals(
                                expectedListSize,
                                loadResponse.value.new_messages.size
                            )
                        }
                        is LoadResponse.Loading -> {}
                    }


                }
            }
        }

    @Test
    fun `pagination returns correct offset`() =
        testDispatcher.runBlockingTest {
            getCredentials()?.let {
                val limit = 2
                val offset = 8

                val paginationParams = MessagePagination.instantiate(
                    limit = limit,
                    offset = offset,
                    date = null
                )

                nqMessage.getMessages(paginationParams).collect { loadResponse ->

                    @Exhaustive
                    when (loadResponse) {
                        is Response.Error -> {
                            loadResponse.exception?.printStackTrace()
                            Assert.fail(loadResponse.message)
                        }
                        is Response.Success -> {
                            Assert.assertEquals(
                                offset + 1L,
                                loadResponse.value.new_messages.firstOrNull()?.id
                            )
                        }
                        is LoadResponse.Loading -> {}
                    }

                }
            }
        }

    @Test
    fun `getPayments returns success`() =
        testDispatcher.runBlockingTest {
            getCredentials()?.let {
                nqMessage.getPayments().collect { loadResponse ->

                    @Exhaustive
                    when (loadResponse) {
                        is Response.Error -> {
                            loadResponse.exception?.printStackTrace()
                            Assert.fail(loadResponse.message)
                        }
                        is Response.Success -> {}
                        is LoadResponse.Loading -> {}
                    }

                }
            }
        }

    @Test
    fun `readMessages returns success`() =
        testDispatcher.runBlockingTest {
            getCredentials()?.let {
                nqMessage.readMessages(ChatId(1)).collect { loadResponse ->

                    @Exhaustive
                    when (loadResponse) {
                        is LoadResponse.Loading -> {}
                        is Response.Error -> {
                            loadResponse.exception?.printStackTrace()
                            Assert.fail(loadResponse.message)
                        }
                        is Response.Success -> {}
                    }

                }
            }
        }
}