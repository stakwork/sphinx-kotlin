package chat.sphinx.concept_network_query_lightning.model.invoice

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("SpellCheckingInspection")
data class HtlcDto(
    val chan_id: String,
    val htlc_index: Long,
    val amt_msat: Long,
    val accept_height: Long,
    val accept_time: Long,
    val resolve_time: Long,
    val expiry_height: Long,
    val state: String,
    val custom_records: Map<Long, HashDto>,
    val mpp_total_amt_msat: Long,
)
