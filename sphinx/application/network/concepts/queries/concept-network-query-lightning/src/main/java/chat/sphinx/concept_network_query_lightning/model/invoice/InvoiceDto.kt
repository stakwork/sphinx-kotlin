package chat.sphinx.concept_network_query_lightning.model.invoice

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("SpellCheckingInspection")
data class InvoiceDto(
    val memo: String,
    val r_preimage: HashDto,
    val r_hash: HashDto,
    val value: Long,
    val value_msat: Long,
    val settled: Boolean,
    val creation_date: Long,
    val settle_date: Long,
    val payment_request: String,
    val description_hash: HashDto,
    val expiry: String,
    val fallback_addr: String,
    val cltv_expiry: String,
    val route_hints: List<String>,
    val private: Boolean,
    val add_index: Long,
    val settle_index: Long,
    val amt_paid: Long,
    val amt_paid_sat: Long,
    val amt_paid_msat: Long,
    val state: String,
    val htlcs: List<HtlcDto>,
    val features: Map<Long, FeatureDto>,
    val is_keysend: Boolean,
)
