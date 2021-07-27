package chat.sphinx.onboard_common.internal.json

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class Step4Json(val invite_data_json: Step1Json.InviteDataJson)
