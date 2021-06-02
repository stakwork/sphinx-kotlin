package chat.sphinx.concept_paging

import com.squareup.sqldelight.Query

abstract class QueryProvider<T: Any> {
    abstract fun provide(limit: Long, offset: Long): Query<T>
}
