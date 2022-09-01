package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.add_tribe_member.navigation.ToAddTribeMember
import chat.sphinx.create_tribe.navigation.ToCreateTribeDetail
import chat.sphinx.qr_code.navigation.ToQRCodeDetail
import chat.sphinx.tribe_detail.navigation.TribeDetailNavigator
import chat.sphinx.tribe_members_list.navigation.ToTribeMembersListDetail
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import kotlinx.coroutines.delay
import javax.inject.Inject

internal class TribeDetailNavigatorImpl @Inject constructor(
    val detailDriver: DetailNavigationDriver,
    val primaryNavigationDriver: PrimaryNavigationDriver
): TribeDetailNavigator(detailDriver) {

    override suspend fun closeDetailScreen() {
        detailDriver.closeDetailScreen()
    }

    override suspend fun goBackToDashboard() {
        primaryNavigationDriver.submitNavigationRequest(
            PopBackStack()
        )
        delay(100L)
        detailDriver.closeDetailScreen()
    }

    override suspend fun toShareTribeScreen(
        qrText: String,
        viewTitle: String,
        description: String?,
    ) {
        detailDriver.submitNavigationRequest(ToQRCodeDetail(qrText, viewTitle, description))
    }

    override suspend fun toCreateTribeScreen(chatId: ChatId) {
        detailDriver.submitNavigationRequest(ToCreateTribeDetail(chatId))
    }

    override suspend fun toTribeMemberList(chatId: ChatId) {
        detailDriver.submitNavigationRequest(ToTribeMembersListDetail(chatId))
    }

    override suspend fun toAddMember(chatId: ChatId) {
        detailDriver.submitNavigationRequest(ToAddTribeMember(chatId))
    }
}
