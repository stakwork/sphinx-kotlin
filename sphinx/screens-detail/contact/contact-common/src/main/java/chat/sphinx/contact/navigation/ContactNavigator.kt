package chat.sphinx.contact.navigation

import androidx.navigation.NavController
import chat.sphinx.qr_code.navigation.ToQRCodeDetail
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

    suspend fun toQRCodeDetail(qrText: String, viewTitle: String, description: String) {
        navigationDriver.submitNavigationRequest(
            ToQRCodeDetail(
                qrText,
                viewTitle,
                description,
                true
            )
        )
    }
}