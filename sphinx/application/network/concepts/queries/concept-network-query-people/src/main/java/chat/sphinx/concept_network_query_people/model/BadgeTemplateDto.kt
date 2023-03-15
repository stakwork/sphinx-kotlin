package chat.sphinx.concept_network_query_people.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BadgeTemplateDto(
    val icon: String?,
    val name: String?,
    val rewardType: Int?,
    val rewardRequirement: Int?
)
