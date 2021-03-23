package chat.sphinx.concept_network_query_lightning.model.channel

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChannelConstraintDto(
    val csv_delay: Int,
    val chan_reserve_sat: Long,
    val dust_limit_sat: Long,
    val max_pending_amt_msat: Long,
    val min_htlc_msat: Long,
    val max_accepted_htlcs: Int,
)
