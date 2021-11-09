package chat.sphinx.feature_repository.mappers.feed

import chat.sphinx.conceptcoredb.FeedItemDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_podcast.PodcastEpisode
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class FeedItemDboPodcastEpisodePresenterMapper(
    dispatchers: CoroutineDispatchers,
): ClassMapper<FeedItemDbo, PodcastEpisode>(dispatchers) {
    override suspend fun mapFrom(value: FeedItemDbo): PodcastEpisode {
        return PodcastEpisode(
            id = value.id,
            title = value.title,
            description = value.description,
            image = value.image_url,
            link = value.link,
            enclosureUrl = value.enclosure_url,
            podcastId = value.feed_id
        )
    }

    override suspend fun mapTo(value: PodcastEpisode): FeedItemDbo {
        return FeedItemDbo(
            value.id,
            value.title,
            value.description,
            null,
            null,
            null,
            null,
            value.enclosureUrl,
            null,
            value.image,
            null,
            value.link,
            value.podcastId
        )
    }
}