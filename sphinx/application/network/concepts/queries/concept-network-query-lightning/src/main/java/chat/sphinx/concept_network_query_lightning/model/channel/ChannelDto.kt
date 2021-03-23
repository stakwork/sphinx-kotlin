package chat.sphinx.concept_network_query_lightning.model.channel

import chat.sphinx.concept_network_query_lightning.model.invoice.HtlcDto
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("SpellCheckingInspection")
data class ChannelDto(
    val active: Boolean,
    val remote_pubkey: String,
    val channel_point: String,
    val chan_id: Long,
    val capacity: Long,
    val local_balance: Long,
    val remote_balance: Long,
    val commit_fee: Long,
    val commit_weight: Long,
    val fee_per_kw: Long,
    val unsettled_balance: Long,
    val total_satoshis_sent: Long,
    val total_satoshis_received: Long,
    val num_updates: Long,
    val pending_htlcs: List<HtlcDto>,
    val csv_delay: Int,
    val private: Boolean,
    val initiator: Boolean,
    val chan_status_flags: String,
    val local_chan_reserve_sat: Long,
    val remote_chan_reserve_sat: Long,
    val commitment_type: String,
    val lifetime: Long,
    val uptime: Long,
    val close_address: String,
    val push_amount_sat: Long,
    val thaw_height: Int,
    val local_constraints: ChannelConstraintDto,
    val remote_constraints: ChannelConstraintDto,
)
