package chat.sphinx.onboard_common.internal.json

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class Step3Json(val invite_data_json: Step1Json.InviteDataJson)
