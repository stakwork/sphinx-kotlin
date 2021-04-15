package chat.sphinx.wrapper_common

import org.junit.Assert
import org.junit.Test

internal class DateTimeUnitTest {

    companion object {

        // pulled from /chats endpoint
        private val testData: List<String> by lazy {
            listOf(
                "2021-02-17T14:47:40.000Z",
                "2021-03-25T14:32:41.997Z",
                "2021-02-17T14:48:58.000Z",
                "2021-03-23T17:32:34.674Z",
                "2021-02-19T20:42:57.000Z",
                "2021-03-23T16:22:38.309Z",
                "2021-03-01T14:43:22.000Z",
                "2021-03-24T20:33:39.476Z",
                "2021-03-05T14:33:30.000Z",
                "2021-03-11T19:52:46.057Z",
            )
        }
    }

    @Test
    fun `string converts to DateTime and back correctly formatted`() {
        testData.forEach { timeString ->
            Assert.assertEquals(timeString, timeString.toDateTime().toString())
        }
    }

    @Test
    fun `DateTime before and after comparators function properly`() {
        val date1 = testData[0].toDateTime()
        val date2 = testData[1].toDateTime()

        Assert.assertTrue(date1.before(date2))
        Assert.assertTrue(date2.after(date1))
    }

    @Test
    fun `DateTime to and from Long compatible with string format`() {
        val date: Long = testData[4].toDateTime().time

        Assert.assertEquals(testData[4], date.toDateTime().toString())
    }
}