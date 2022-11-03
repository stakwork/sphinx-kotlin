package chat.sphinx.concept_network_query_action_track.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncActionsDto(
    val data: List<ActionTrackDto>
)
