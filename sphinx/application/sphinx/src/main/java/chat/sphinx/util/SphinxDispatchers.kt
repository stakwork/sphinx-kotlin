package chat.sphinx.util

import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class SphinxDispatchers(
    default: CoroutineDispatcher,
    io: CoroutineDispatcher,
    main: CoroutineDispatcher,
    mainImmediate: CoroutineDispatcher,
    unconfined: CoroutineDispatcher
): CoroutineDispatchers(default, io, main, mainImmediate, unconfined) {
    constructor(): this(
        Dispatchers.Default,
        Dispatchers.IO,
        Dispatchers.Main,
        Dispatchers.Main.immediate,
        Dispatchers.Unconfined
    )
}
