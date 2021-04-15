package chat.sphinx.feature_network_query_chat

import app.cash.exhaustive.Exhaustive
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.exception
import chat.sphinx.kotlin_response.message
import chat.sphinx.test_network_query.NetworkQueryTestHelper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test

class NetworkQueryChatImplUnitTest: NetworkQueryTestHelper() {

    @Test
    fun `getChats returns success`() =
        testDispatcher.runBlockingTest {
            getCredentials()?.let {
                nqChat.getChats().collect { loadResponse ->

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
}
