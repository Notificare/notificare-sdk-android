package re.notifica.inbox.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import re.notifica.models.NotificareNotification
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificareInboxItemTest {
    @Test
    public fun testNotificareInboxItemSerialization() {
        val inboxItem = NotificareInboxItem(
            id = "testId",
            notification = NotificareNotification(
                id = "testId",
                type = NotificareNotification.TYPE_NONE,
                time = Date(),
                title = "testTitle",
                subtitle = "testSubtitle",
                message = "testMessage"
            ),
            time = Date(),
            opened = false,
            expires = Date()
        )

        val convertedInboxItem = NotificareInboxItem.fromJson(inboxItem.toJson())

        assertEquals(inboxItem, convertedInboxItem)
    }

    @Test
    public fun testNotificareInboxItemSerializationWithNullProps() {
        val inboxItem = NotificareInboxItem(
            id = "testId",
            notification = NotificareNotification(
                id = "testId",
                type = NotificareNotification.TYPE_NONE,
                time = Date(),
                title = "testTitle",
                subtitle = "testSubtitle",
                message = "testMessage"
            ),
            time = Date(),
            opened = false,
            expires = null
        )

        val convertedInboxItem = NotificareInboxItem.fromJson(inboxItem.toJson())

        assertEquals(inboxItem, convertedInboxItem)
    }
}
