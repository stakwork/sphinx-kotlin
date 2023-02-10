package chat.sphinx.wrapper_common.dashboard

data class RestoreProgress(
    val restoring: Boolean,
    val progress : Int,
)

data class RestoreProgressViewState(
    val progress : Int,
    val progressLabel: Int,
    val continueButtonEnabled: Boolean
)