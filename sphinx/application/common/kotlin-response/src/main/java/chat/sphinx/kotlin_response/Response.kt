package chat.sphinx.kotlin_response

sealed class Response<out Success: Any?, out Error: Any>: LoadResponse<Success, Error>() {
    data class Success<out Success: Any?>(val value: Success): Response<Success, Nothing>()
    data class Error<out Error: Any>(val cause: Error): Response<Nothing, Error>()
}

sealed class LoadResponse<out Success: Any?, out Error: Any> {
    object Loading: LoadResponse<Nothing, Nothing>()
}

inline val Response.Error<ResponseError>.message: String
    get() = cause.message

inline val Response.Error<ResponseError>.exception: Exception?
    get() = cause.exception

data class ResponseError(
    val message: String,
    val exception: Exception? = null,
    val statusCode: Long? = null
)
