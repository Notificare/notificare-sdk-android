package re.notifica.inbox.user.internal.responses

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import re.notifica.inbox.user.models.NotificareUserInboxItem
import re.notifica.models.NotificareNotification
import re.notifica.models.NotificareNotification.Companion.TYPE_NONE
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
                type = TYPE_NONE,
                time = Date(1),
                title = "testTitle",
                subtitle = "testSubtitle",
                message = "testMessage",
                attachments = listOf(),
                extra = mapOf()
            ),
            time = Date(1),
            opened = false,
            expires = null
        )

        val item = RawUserInboxResponse.RawUserInboxItem(
            _id = "testId",
            notification = "testNotification",
            type = TYPE_NONE,
            time = Date(1),
            title = "testTitle",
            subtitle = "testSubtitle",
            message = "testMessage",
            attachment = null,
        ).toModel()

        assertEquals(expectedItem, item)
    }
}
