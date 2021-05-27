package chat.sphinx.concept_repository_dashboard

sealed class DashboardItem {
    class Chat(): DashboardItem()
    class Contact(): DashboardItem()
    class Invite(): DashboardItem()
}
