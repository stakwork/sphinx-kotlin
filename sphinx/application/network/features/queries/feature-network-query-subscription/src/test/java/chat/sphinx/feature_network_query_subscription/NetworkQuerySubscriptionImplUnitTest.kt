package chat.sphinx.feature_network_query_subscription

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_network_query_subscription.model.SubscriptionDto
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.exception
import chat.sphinx.kotlin_response.message
import chat.sphinx.test_network_query.NetworkQueryTestHelper
import chat.sphinx.wrapper_common.subscription.SubscriptionId
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test

class NetworkQuerySubscriptionImplUnitTest: NetworkQueryTestHelper() {

    @Test
    fun `getSubscriptions returns success`() =
        testDispatcher.runBlockingTest {
            getCredentials()?.let {
                nqSubscription.getSubscriptions().collect { loadResponse ->

                    @Exhaustive
                    when (loadResponse) {
                        is Response.Error -> {
                            // will fail on error
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
    fun `getSubscriptionById returns success`() =
        testDispatcher.runBlockingTest {
            getCredentials()?.let {
                var subscription: SubscriptionDto? = null

                nqSubscription.getSubscriptions().collect { loadResponse ->

                    @Exhaustive
                    when (loadResponse) {
                        is Response.Error -> {
                            // will fail on error
                            loadResponse.exception?.printStackTrace()
                            Assert.fail(loadResponse.message)
                        }
                        is Response.Success -> {
                            subscription = loadResponse.value.lastOrNull()
                        }
                        is LoadResponse.Loading -> {}
                    }

                }

                subscription?.let { nnSub ->
                    nqSubscription.getSubscriptionById(SubscriptionId(nnSub.id)).collect { loadResponse ->

                        @Exhaustive
                        when (loadResponse) {
                            is Response.Error -> {
                                // will fail on error
                                loadResponse.exception?.printStackTrace()
                                Assert.fail(loadResponse.message)
                            }
                            is Response.Success -> {
                                // should return the same object
                                Assert.assertEquals(
                                    nnSub.toString(),
                                    loadResponse.value.toString()
                                )
                            }
                            LoadResponse.Loading -> {}
                        }

                    }
                } ?: println("\nTest Account does not have any subscriptions")
            }
        }

    @Test
    fun `getSubscriptionById returns error if Id doesn't exist`() =
        testDispatcher.runBlockingTest {
            getCredentials()?.let {
                var lastSubIdPlus1: Long? = null

                nqSubscription.getSubscriptions().collect { loadResponse ->

                    @Exhaustive
                    when (loadResponse) {
                        is Response.Error -> {
                            // will fail on error
                            loadResponse.exception?.printStackTrace()
                            Assert.fail(loadResponse.message)
                        }
                        is Response.Success -> {
                            lastSubIdPlus1 = (loadResponse.value.lastOrNull()?.id ?: 0L) + 1L
                        }
                        is LoadResponse.Loading -> {}
                    }

                }

                nqSubscription.getSubscriptionById(SubscriptionId(lastSubIdPlus1!!)).collect { loadResponse ->

                    @Exhaustive
                    when (loadResponse) {
                        is Response.Error -> {
                            // an error should be returned as that Id does not exist
                            Assert.assertTrue(
//                                Print statement:
//                                Error(cause=ResponseError(message=, exception=java.io.IOException: Response{protocol=http/1.1, code=400, message=Bad Request, url=https://2218d66f0a-sphinx.m.relay.voltageapp.io:3001/subscription/1}))
                                loadResponse.cause.exception?.message?.contains("code=400") == true
                            )
                        }
                        is Response.Success -> {
                            Assert.fail()
                        }
                        LoadResponse.Loading -> {}
                    }

                }
            }
        }
}