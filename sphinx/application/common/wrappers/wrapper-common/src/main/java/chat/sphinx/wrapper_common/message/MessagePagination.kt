package chat.sphinx.wrapper_common.message

import chat.sphinx.wrapper_common.DateTime
import java.text.SimpleDateFormat
import java.util.*

@JvmInline
value class MessagePagination private constructor(val value: String) {

    companion object {

        const val FORMAT_PAGINATION_PERCENT_ESCAPED = "yyyy-MM-dd'%20'HH:mm:ss"

        @Volatile
        private var formatPercentEscapedPagination: SimpleDateFormat? = null
        fun getFormatPaginationPercentEscaped(): SimpleDateFormat =
            formatPercentEscapedPagination ?: synchronized(this) {
                formatPercentEscapedPagination ?: SimpleDateFormat(
                    FORMAT_PAGINATION_PERCENT_ESCAPED,
                    Locale.ENGLISH
                )
                    .also {
                        it.timeZone = TimeZone.getTimeZone(DateTime.UTC)
                        formatPercentEscapedPagination = it
                    }
            }

        @Throws(IllegalArgumentException::class)
        fun instantiate(
            limit: Int,
            offset: Int,
            date: DateTime?
        ): MessagePagination {
            require(limit > 0) {
                "MessagePagination limit must be greater than 0"
            }
            require(offset >= 0) {
                "MessagePagination offset must be greater than or equal to 0"
            }

            val dateString = date?.let {
                "&date=${getFormatPaginationPercentEscaped().format(it.value)}"
            } ?: ""

            return MessagePagination("?limit=$limit&offset=${offset}$dateString")
        }

    }
}
