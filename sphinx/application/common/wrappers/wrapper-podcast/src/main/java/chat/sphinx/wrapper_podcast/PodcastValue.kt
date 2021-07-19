package chat.sphinx.wrapper_podcast

data class PodcastValue(
    val model: PodcastModel,
    val destinations: List<PodcastDestination>,
) {
}

//fun PodcastValueDto.toPodcastValue(): ParcelablePodcastValue {
//    val podcastDestinations = mutableListOf<ParcelablePodcastDestination>()
//
//    for (destination in this.destinations) {
//        podcastDestinations.add(destination.toPodcastDestination())
//    }
//
//    return ParcelablePodcastValue(this.model.toPodcastModel(), podcastDestinations)
//}