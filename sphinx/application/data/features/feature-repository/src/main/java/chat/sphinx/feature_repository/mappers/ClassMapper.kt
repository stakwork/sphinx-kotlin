package chat.sphinx.feature_repository.mappers

import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.withContext

@Suppress("NOTHING_TO_INLINE")
internal suspend inline fun<From, To> ClassMapper<From, To>.mapListFrom(value: List<From>): List<To> =
    withContext(default) {
        value.map { mapFrom(it) }
    }

@Suppress("NOTHING_TO_INLINE")
internal suspend inline fun<From, To> ClassMapper<From, To>.mapListTo(value: List<To>): List<From> =
    withContext(default) {
        value.map { mapTo(it) }
    }

internal abstract class ClassMapper<From, To>(
    val dispatchers: CoroutineDispatchers
): CoroutineDispatchers by dispatchers {
    abstract suspend fun mapFrom(value: From): To
    abstract suspend fun mapTo(value: To): From
}
