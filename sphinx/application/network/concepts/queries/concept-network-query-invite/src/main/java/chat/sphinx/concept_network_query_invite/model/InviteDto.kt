package chat.sphinx.concept_network_query_invite.model

import chat.sphinx.wrapper_common.invite.InviteStatus
import chat.sphinx.wrapper_common.invite.isPaymentPending
import chat.sphinx.wrapper_common.invite.isProcessingPayment
import chat.sphinx.wrapper_common.invite.toInviteStatus
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InviteDto(
    val id: Long,
    val invite_string: String,
    val invoice: String?,
    val welcome_message: String,
    val contact_id: Long,
    val status: Int,
    val price: Long?,
    val created_at: String,
    val updated_at: String,
) {

    fun getInviteStatus(currentStatus: InviteStatus?): InviteStatus {
        return if (
            (currentStatus?.isProcessingPayment() == true) &&
            (status.toInviteStatus().isPaymentPending())
        ) {
            InviteStatus.PROCESSING_PAYMENT.toInviteStatus()
        } else {
            status.toInviteStatus()
        }
    }

}
