package chat.sphinx.feature_repository.mappers

@Suppress("NOTHING_TO_INLINE")
internal inline fun<From, To> ClassMapper<From, To>.mapListFrom(value: List<From>): List<To> =
    value.map { mapFrom(it) }

@Suppress("NOTHING_TO_INLINE")
internal inline fun<From, To> ClassMapper<From, To>.mapListTo(value: List<To>): List<From> =
    value.map { mapTo(it) }

internal abstract class ClassMapper<From, To> {
    abstract fun mapFrom(value: From): To
    abstract fun mapTo(value: To): From
}
