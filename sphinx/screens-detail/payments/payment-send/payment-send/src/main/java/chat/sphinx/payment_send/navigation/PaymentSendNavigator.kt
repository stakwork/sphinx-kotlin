package chat.sphinx.payment_send.navigation

import androidx.navigation.NavController
import chat.sphinx.payment_common.navigation.PaymentNavigator
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class PaymentSendNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): PaymentNavigator(navigationDriver) {
}
