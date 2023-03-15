package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.notification_level.navigation.ToNotificationLevel
import chat.sphinx.add_tribe_member.navigation.ToAddTribeMember
import chat.sphinx.create_tribe.navigation.ToEditTribeDetail
import chat.sphinx.qr_code.navigation.ToQRCodeDetail
import chat.sphinx.tribe_badge.navigation.ToTribeBadges
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

    override suspend fun toTribeBadgesScreen(chatId: ChatId) {
        detailDriver.submitNavigationRequest(ToTribeBadges(chatId))
    }

    override suspend fun toEditTribeScreen(chatId: ChatId) {
        detailDriver.submitNavigationRequest(ToEditTribeDetail(chatId))
    }

    override suspend fun toTribeMemberList(chatId: ChatId) {
        detailDriver.submitNavigationRequest(ToTribeMembersListDetail(chatId))
    }

    override suspend fun toNotificationLevel(chatId: ChatId) {
        detailDriver.submitNavigationRequest(ToNotificationLevel(chatId))
    }

    override suspend fun toAddMember(chatId: ChatId) {
        detailDriver.submitNavigationRequest(ToAddTribeMember(chatId))
    }
}
