package chat.sphinx.concept_network_query_people.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BadgeCreateDto(
    val chat_id: Int,
    val name: String,
    val reward_requirement: Int,
    val memo: String,
    val icon: String,
    val reward_type: Int,
    val active: Boolean = false,
    val amount: Int
)