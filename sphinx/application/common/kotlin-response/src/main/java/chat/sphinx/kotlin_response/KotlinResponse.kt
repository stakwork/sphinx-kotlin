package chat.sphinx.kotlin_response

sealed class KotlinResponse<out T: Any> {
    data class Success<out T: Any>(val value: T): KotlinResponse<T>()
    data class Error(val message: String, val cause: Exception? = null): KotlinResponse<Nothing>()
    object Loading: KotlinResponse<Nothing>()
}
