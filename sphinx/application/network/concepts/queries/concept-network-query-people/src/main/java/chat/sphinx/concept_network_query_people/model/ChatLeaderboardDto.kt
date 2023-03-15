package chat.sphinx.concept_network_query_people.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatLeaderboardDto(
    val alias: String?,
    val earned: Int?,
    val spent: Int?,
    val tribeUUID: String?,
    val reputation: Int?,
    val spentRank: Int?,
    val earnedRank: Int?
)