package chat.sphinx.wrapper_io_utils

import java.io.InputStream

@Suppress("NOTHING_TO_INLINE")
inline fun InputStream.toInputStreamProvider(): InputStreamProvider {
    val inputStream = this

    return object : InputStreamProvider() {
        override fun newInputStream(): InputStream {
            return inputStream
        }
    }
}

abstract class InputStreamProvider {
    abstract fun newInputStream(): InputStream
}
