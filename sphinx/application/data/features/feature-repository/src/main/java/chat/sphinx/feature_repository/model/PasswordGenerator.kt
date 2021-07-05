package chat.sphinx.feature_repository.model

import io.matthewnelson.crypto_common.clazzes.Password
import java.io.CharArrayWriter
import java.security.SecureRandom

internal class PasswordGenerator(size: Int, chars: Set<Char> = DEFAULT_CHARS) {

    companion object {
        val DEFAULT_CHARS: Set<Char>
            get() = setOf(
                '0', '1', '2', '3', '4',
                '5', '6', '7', '8', '9',

                'a', 'b', 'c', 'd', 'e',
                'f', 'g', 'h', 'i', 'j',
                'k', 'l', 'm', 'n', 'o',
                'p', 'q', 'r', 's', 't',
                'u', 'v', 'w', 'x', 'y',
                'z',

                'A', 'B', 'C', 'D', 'E',
                'F', 'G', 'H', 'I', 'J',
                'K', 'L', 'M', 'N', 'O',
                'P', 'Q', 'R', 'S', 'T',
                'U', 'V', 'W', 'X', 'Y',
                'Z',
            )
    }

    init {
        require(size >= 20) {
            "size must be greater than or equal to 20"
        }
        require(chars.size >= 30) {
            "chars must contain greater than or equal to 30"
        }
    }

    val password: Password = SecureRandom().let { random ->
        CharArrayWriter(size).let { writer ->
            repeat(size) {
                writer.append(chars.elementAt(random.nextInt(chars.size)))
            }

            Password(writer.toCharArray())
        }
    }
}
