package chat.sphinx.concept_network_query_chat.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TribeDto(
    val host: String?,
    val uuid: String?,
    val name: String?,
    val description: String?,
    val img: String?,
    val group_key: String?,
    val owner_pubkey: String?,
    val owner_route_hint: String?,
    val owner_alias: String?,
    val price_to_join: Long?,
    val price_per_message: Long?,
    val escrow_amount: Long?,
    val escrow_millis: Long?,
    val unlisted: Any?,
    val private: Any?,
    val deleted: Any?,
    val app_url: String?,
    val feed_url: String?,
) {

    val hourToStake: Long
        get() = (escrow_millis ?: 0) / 60 / 60 / 1000
}