package chat.sphinx.concept_network_query_lightning.model.balance

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BalanceDto(
    val reserve: Long,
    val full_balance: Long,
    val balance: Long,
    val pending_open_balance: Long
)
