package chat.sphinx.send_attachment.navigation

internal sealed class BackType {
    object PopBackStack: BackType()
    object CloseDetailScreen: BackType()
}