package chat.sphinx.podcast_player.navigation

internal sealed class BackType {
    object PopBackStack: BackType()
    object CloseDetailScreen: BackType()
}
