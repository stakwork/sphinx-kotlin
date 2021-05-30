package chat.sphinx.wrapper_common.dashboard

@JvmInline
value class ContactId(override val value: Long): DashboardItemId {
    override val dashboardItemType: DashboardItemType
        get() = DashboardItemType.Contact
}
