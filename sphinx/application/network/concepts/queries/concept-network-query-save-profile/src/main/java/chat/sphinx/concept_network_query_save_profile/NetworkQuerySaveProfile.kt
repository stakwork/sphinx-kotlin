package chat.sphinx.concept_network_query_save_profile

import chat.sphinx.concept_network_query_save_profile.model.PersonInfoDto
import chat.sphinx.concept_network_query_save_profile.model.SaveProfileDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkQuerySaveProfile {

    abstract fun getPeopleProfileByKey(
        host: String,
        key: String
    ): Flow<LoadResponse<SaveProfileDto, ResponseError>>

    abstract fun savePeopleProfile(
        data: PersonInfoDto,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<Any, ResponseError>>

//    abstract fun deleteProfile(): Flow<LoadResponse<Any, ResponseError>>
}