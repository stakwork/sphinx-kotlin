package chat.sphinx.wrapper_common.dashboard

@JvmInline
value class ContactId(override val value: Long): DashboardItemId {
    companion object {
        const val NULL_CONTACT_ID = Long.MAX_VALUE
    }

    override val dashboardItemType: DashboardItemType
        get() = DashboardItemType.Contact
}
