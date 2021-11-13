package chat.sphinx.concept_network_query_save_profile

import chat.sphinx.concept_network_query_save_profile.model.PersonInfoDto
import chat.sphinx.concept_network_query_save_profile.model.SignBase64Dto
import chat.sphinx.concept_network_query_save_profile.model.SaveProfileDto
import chat.sphinx.concept_network_query_save_profile.model.SaveProfileInfoDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkQuerySaveProfile {

    abstract fun getProfileByKey(
        host: String,
        key: String
    ): Flow<LoadResponse<PersonInfoDto, ResponseError>>

    abstract fun saveProfile(
        data: String
    ): Flow<LoadResponse<PersonInfoDto, ResponseError>>
}