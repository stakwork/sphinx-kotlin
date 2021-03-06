package chat.sphinx.address_book.navigation

import androidx.navigation.NavController
import chat.sphinx.address_book.R
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToAddressBookScreen: NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.address_book_nav_graph,
            null,
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}
