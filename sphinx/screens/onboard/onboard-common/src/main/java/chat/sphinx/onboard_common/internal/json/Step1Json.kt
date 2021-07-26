package chat.sphinx.onboard_common.internal.json

import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.onboard_common.model.OnBoardStep
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
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
internal inline fun OnBoardStep.Step1.toStep1Json(): Step1Json =
    Step1Json(
        relayUrl.value,
        authorizationToken.value,
        inviterData.toInviteDataJson(),
    )

@Suppress("NOTHING_TO_INLINE")
internal inline fun Step1Json.toOnboardStep1(): OnBoardStep.Step1 =
    OnBoardStep.Step1(
        RelayUrl(relay_url),
        AuthorizationToken(authorization_token),
        invite_data_json.toOnBoardInviteData(),
    )

@JsonClass(generateAdapter = true)
internal data class Step1Json(
    val relay_url: String,
    val authorization_token: String,
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
