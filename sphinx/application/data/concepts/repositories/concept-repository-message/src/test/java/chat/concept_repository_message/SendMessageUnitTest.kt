package chat.concept_repository_message

import chat.sphinx.concept_repository_message.model.AttachmentInfo
import chat.sphinx.concept_repository_message.model.SendMessage
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_message_media.MediaType
import org.junit.After
import org.junit.Assert
import org.junit.Test
import java.io.File

class SendMessageUnitTest {

    private val builder = SendMessage.Builder()
    private val assertFalse
        get() = Assert.assertFalse(builder.isValid)

    private val assertTrue
        get() = Assert.assertTrue(builder.isValid)

    private val path: String
        get() = System.getProperty("user.dir").let { dir ->
            val d: String = if (dir.contains(":\\")) {
                "\\"
            } else {
                "/"
            }

            // return this file
            "$dir${d}src${d}test${d}java${d}chat${d}concept_repository_message${d}" +
                    "${this.javaClass.simpleName}.kt"
        }

    @After
    fun tearDown() {
        builder.clear()
    }

    @Test
    fun `no contact or chat id fails`() {
        builder.setText("some text")
        assertFalse

        val info = AttachmentInfo(
            File(path),
            MediaType.Image("image"),
            isLocalFile = true
        )

        builder.setAttachmentInfo(info)
        assertFalse
    }

    @Test
    fun `contact and or chat id succeeds`() {
        builder.setText("some text")

        // only contact succeeds
        builder.setContactId(ContactId(1))
        assertTrue

        // both contact & chat succeeds
        builder.setChatId(ChatId(1))
        assertTrue

        // only chat succeeds
        builder.setContactId(null)
        assertTrue

        builder.setChatId(null)
        assertFalse
    }

    @Test
    fun `file DNE fails`() {
        val path = path

        println(path)
        // invalid file path
        builder.setAttachmentInfo(
            AttachmentInfo(
                File(path.dropLast(3)),
                MediaType.Text,
                isLocalFile = false,
            )
        )
        builder.setContactId(ContactId(1))
        assertFalse

        // fails even with text b/c file is invalid
        builder.setText("some text")
        assertFalse

        // file exists
        builder.setAttachmentInfo(
            AttachmentInfo(
                File(path),
                MediaType.Text,
                isLocalFile = true,
            )
        )
        assertTrue
    }

    @Test
    fun `build() returns null if invalid`() {
        builder.setText("some text")
        Assert.assertNull(builder.build())
        builder.setChatId(ChatId(1))
        Assert.assertNotNull(builder.build())
    }
}
