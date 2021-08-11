package chat.sphinx.wrapper_common.lightning

/**
 * A class that contains data which is descriptive of how
 * to reach one's lightning node.
 * */
sealed interface LightningNodeDescriptor {
    val value: String
}
