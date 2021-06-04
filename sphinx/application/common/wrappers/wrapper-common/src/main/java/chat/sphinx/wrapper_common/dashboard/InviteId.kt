package chat.sphinx.wrapper_common.dashboard

@JvmInline
value class InviteId(override val value: Long): DashboardItemId {

    init {
        require(this.value >= 0L) {
            "InviteId must be greater than or equal 0"
        }
    }

    override val dashboardItemType: DashboardItemType
        get() = DashboardItemType.Invite
}
