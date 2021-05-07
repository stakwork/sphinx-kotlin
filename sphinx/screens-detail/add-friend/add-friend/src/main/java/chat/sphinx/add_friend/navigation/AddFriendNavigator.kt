package chat.sphinx.add_friend.navigation

import androidx.navigation.NavController
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class AddFriendNavigator(
    detailNavigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(detailNavigationDriver) {
    abstract suspend fun toCreateInvitationDetail()
    abstract suspend fun toAddContactDetail()
    suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }
}