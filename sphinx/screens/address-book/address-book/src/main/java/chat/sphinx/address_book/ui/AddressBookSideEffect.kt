package chat.sphinx.address_book.ui

import androidx.fragment.app.FragmentActivity
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class AddressBookSideEffect: SideEffect<FragmentActivity>() {
    class Notify(private val msg: String): AddressBookSideEffect() {
        override suspend fun execute(value: FragmentActivity) {
            SphinxToastUtils().show(value, msg)
        }
    }
}
