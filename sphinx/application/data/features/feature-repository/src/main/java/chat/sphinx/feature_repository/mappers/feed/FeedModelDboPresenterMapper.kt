package chat.sphinx.feature_repository.mappers.feed

import chat.sphinx.conceptcoredb.FeedModelDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_feed.FeedModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class FeedModelDboPresenterMapper(
    dispatchers: CoroutineDispatchers,
): ClassMapper<FeedModelDbo, FeedModel>(dispatchers) {
    override suspend fun mapFrom(value: FeedModelDbo): FeedModel {
        return FeedModel(
            value.id,
            value.type,
            value.suggested,
        )
    }

    override suspend fun mapTo(value: FeedModel): FeedModelDbo {
        return FeedModelDbo(
            value.id,
            value.type,
            value.suggested,
        )
    }
}