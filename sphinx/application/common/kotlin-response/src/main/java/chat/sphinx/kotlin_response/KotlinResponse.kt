package chat.sphinx.kotlin_response

sealed class KotlinResponse<out Success: Any, out Error: Any>: LoadResponse<Success, Error>() {
    data class Success<out Success: Any>(val value: Success): KotlinResponse<Success, Nothing>()
    data class Error<out Error: Any>(val cause: Error): KotlinResponse<Nothing, Error>()
}

sealed class LoadResponse<out Success: Any, out Error: Any> {
    object Loading: LoadResponse<Nothing, Nothing>()
}

inline val KotlinResponse.Error<ResponseError>.message: String
    get() = cause.message

inline val KotlinResponse.Error<ResponseError>.exception: Exception?
    get() = cause.exception

data class ResponseError(val message: String, val exception: Exception? = null)
