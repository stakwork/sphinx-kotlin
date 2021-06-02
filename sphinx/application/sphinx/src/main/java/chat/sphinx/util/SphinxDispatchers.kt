package chat.sphinx.util

import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class SphinxDispatchers(
    override val default: CoroutineDispatcher,
    override val io: CoroutineDispatcher,
    override val main: CoroutineDispatcher,
    override val mainImmediate: CoroutineDispatcher,
    override val unconfined: CoroutineDispatcher
): CoroutineDispatchers {
    constructor(): this(
        Dispatchers.Default,
        Dispatchers.IO,
        Dispatchers.Main,
        Dispatchers.Main.immediate,
        Dispatchers.Unconfined
    )
}
