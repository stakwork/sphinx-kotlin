package chat.sphinx.add_tribe_member.ui

import android.content.Context
import chat.sphinx.add_tribe_member.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class AddTribeMemberSideEffect: SideEffect<Context>() {
    object FieldsRequired: AddTribeMemberSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.member_info_required)
        }
    }

    object InvalidPublicKey: AddTribeMemberSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.invalid_public_key)
        }
    }

    object InvalidRouteHint: AddTribeMemberSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.invalid_route_hint)
        }
    }

    object InvalidUrl: AddTribeMemberSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.invalid_url)
        }
    }

    object FailedToProcessImage: AddTribeMemberSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.failed_to_process_image)
        }
    }

    object FailedToAddMember: AddTribeMemberSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.failed_to_process_image)
        }
    }
}
