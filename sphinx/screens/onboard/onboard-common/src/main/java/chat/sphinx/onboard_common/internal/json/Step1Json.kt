package chat.sphinx.onboard_common.internal.json

import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.onboard_common.model.OnBoardStep
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayHMacKey
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_rsa.RsaPublicKey
import com.squareup.moshi.JsonClass

@Suppress("NOTHING_TO_INLINE")
internal inline fun OnBoardInviterData.toInviteDataJson(): Step1Json.InviteDataJson =
    Step1Json.InviteDataJson(
        nickname,
        pubkey?.value,
        routeHint,
        message,
        action,
        pin
    )

@Suppress("NOTHING_TO_INLINE")
internal inline fun Step1Json.InviteDataJson.toOnBoardInviteData(): OnBoardInviterData =
    OnBoardInviterData(
        nickname,
        pubkey?.toLightningNodePubKey(),
        route_hint,
        message,
        action,
        pin,
    )

@Suppress("NOTHING_TO_INLINE")
internal inline fun OnBoardStep.Step1_WelcomeMessage.toStep1Json(): Step1Json =
    Step1Json(
        relay_url = relayUrl.value,
        authorization_token = authorizationToken.value,
        transport_key = transportKey?.value?.joinToString("") ?: "",
        h_mac_key = hMacKey?.value ?: "",
        invite_data_json = inviterData.toInviteDataJson(),
    )

@Suppress("NOTHING_TO_INLINE")
internal inline fun Step1Json.toOnboardStep1(): OnBoardStep.Step1_WelcomeMessage =
    OnBoardStep.Step1_WelcomeMessage(
        RelayUrl(relay_url),
        AuthorizationToken(authorization_token),
        RsaPublicKey(transport_key.toCharArray()),
        RelayHMacKey(h_mac_key),
        invite_data_json.toOnBoardInviteData(),
    )

@JsonClass(generateAdapter = true)
internal data class Step1Json(
    val relay_url: String,
    val authorization_token: String,
    val transport_key: String,
    val h_mac_key: String,
    val invite_data_json: InviteDataJson,
) {

    @JsonClass(generateAdapter = true)
    internal class InviteDataJson(
        val nickname: String?,
        val pubkey: String?,
        val route_hint: String?,
        val message: String?,
        val action: String?,
        val pin: String?
    )

}
