package chat.sphinx.feature_repository.mappers.feed

import chat.sphinx.conceptcoredb.FeedModelDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_podcast.PodcastModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class FeedModelDboPodcastModelPresenterMapper(
    dispatchers: CoroutineDispatchers,
): ClassMapper<FeedModelDbo, PodcastModel>(dispatchers) {
    override suspend fun mapFrom(value: FeedModelDbo): PodcastModel {
        return PodcastModel(
            type = value.type,
            suggested = value.suggested,
            podcastId = value.id
        )
    }

    override suspend fun mapTo(value: PodcastModel): FeedModelDbo {
        return FeedModelDbo(
            id = value.podcastId,
            type = value.type,
            suggested = value.suggested
        )
    }
}