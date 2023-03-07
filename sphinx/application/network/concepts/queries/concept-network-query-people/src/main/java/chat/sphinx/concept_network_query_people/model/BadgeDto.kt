package chat.sphinx.concept_network_query_people.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BadgeDto(
    val badge_id: Int?,
    val icon: String?,
    val name: String?,
    val amount_created: Int?,
    val amount_issued: Int?,
    val chat_id: Int?,
    val claim_amount: Int?,
    val reward_type: Int?,
    val reward_requirement: Int?,
    val memo: String?,
    val active : Boolean = false
)