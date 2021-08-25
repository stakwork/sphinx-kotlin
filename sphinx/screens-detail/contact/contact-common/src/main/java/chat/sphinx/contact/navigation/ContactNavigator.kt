package chat.sphinx.contact.navigation

import androidx.navigation.NavController
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class ContactNavigator(
    detailNavigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(detailNavigationDriver) {
    suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }
    abstract suspend fun closeDetailScreen()

    abstract suspend fun toQRCodeDetail(qrText: String, viewTitle: String, description: String)
}