package chat.sphinx.wrapper_common.message

import chat.sphinx.wrapper_common.DateTime

inline class MessagePagination private constructor(val value: String) {

    companion object {

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
                "&date=${it.value}"
            } ?: ""

            return MessagePagination("?limit=$limit&offset=${offset}$dateString")
        }

    }
}
