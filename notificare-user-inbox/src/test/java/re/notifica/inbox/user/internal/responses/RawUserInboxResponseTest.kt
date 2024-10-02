package re.notifica.inbox.user.internal.responses

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import re.notifica.inbox.user.models.NotificareUserInboxItem
import re.notifica.models.NotificareNotification
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class RawUserInboxResponseTest {
    @Test
    public fun testRawUserInboxItemToModel() {
        val expectedItem = NotificareUserInboxItem(
            id = "testId",
            notification = NotificareNotification(
                id = "testNotification",
                partial = true,
                type = NotificareNotification.TYPE_NONE,
                time = Date(1),
                title = "testTitle",
                subtitle = "testSubtitle",
                message = "testMessage",
                attachments = listOf(
                    NotificareNotification.Attachment(
                        mimeType = "testMimeType",
                        uri = "testUri"
                    )
                ),
                extra = mapOf("testKey" to "testValue")
            ),
            time = Date(1),
            opened = true,
            expires = Date(1)
        )

        val item = RawUserInboxResponse.RawUserInboxItem(
            _id = "testId",
            notification = "testNotification",
            type = NotificareNotification.TYPE_NONE,
            time = Date(1),
            title = "testTitle",
            subtitle = "testSubtitle",
            message = "testMessage",
            attachment = NotificareNotification.Attachment(
                mimeType = "testMimeType",
                uri = "testUri"
            ),
            extra = mapOf("testKey" to "testValue"),
            opened = true,
            expires = Date(1)
        ).toModel()

        assertEquals(expectedItem, item)
    }

    @Test
    public fun testRawUserInboxItemWithNullPropsToModel() {
        val expectedItem = NotificareUserInboxItem(
            id = "testId",
            notification = NotificareNotification(
                id = "testNotification",
                partial = true,
                type = NotificareNotification.TYPE_NONE,
                time = Date(1),
                title = null,
                subtitle = null,
                message = "testMessage",
                attachments = listOf(),
                extra = mapOf("testKey" to "testValue")
            ),
            time = Date(1),
            opened = false,
            expires = null
        )

        val item = RawUserInboxResponse.RawUserInboxItem(
            _id = "testId",
            notification = "testNotification",
            type = NotificareNotification.TYPE_NONE,
            time = Date(1),
            title = null,
            subtitle = null,
            message = "testMessage",
            attachment = null,
            extra = mapOf("testKey" to "testValue"),
            opened = false,
            expires = null
        ).toModel()

        assertEquals(expectedItem, item)
    }
}
