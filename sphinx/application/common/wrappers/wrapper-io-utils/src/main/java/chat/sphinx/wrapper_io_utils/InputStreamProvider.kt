package chat.sphinx.wrapper_io_utils

import java.io.InputStream

abstract class InputStreamProvider {
    abstract fun newInputStream(): InputStream
}
