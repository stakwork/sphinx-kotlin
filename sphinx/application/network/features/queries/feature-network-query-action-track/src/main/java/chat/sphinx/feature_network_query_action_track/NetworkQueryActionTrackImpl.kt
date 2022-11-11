package chat.sphinx.feature_network_query_action_track

import chat.sphinx.concept_network_query_action_track.NetworkQueryActionTrack
import chat.sphinx.concept_network_query_action_track.model.SyncActionsDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_action_track.model.ActionsTrackedRelayResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow

class NetworkQueryActionTrackImpl(
    private val networkRelayCall: NetworkRelayCall
): NetworkQueryActionTrack() {

    companion object {
        private const val ENDPOINT_ACTION_HISTORY_BULK = "/action_history_bulk"
    }

    override fun sendActionsTracked(
        syncActionsDto: SyncActionsDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<Any?, ResponseError>> {
        return networkRelayCall.relayPost(
            responseJsonClass = ActionsTrackedRelayResponse::class.java,
            relayEndpoint = ENDPOINT_ACTION_HISTORY_BULK,
            requestBodyJsonClass = SyncActionsDto::class.java,
            requestBody = syncActionsDto,
            relayData = relayData
        )
    }
}