package re.notifica.inbox.user.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import re.notifica.models.NotificareNotification
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificareUserInboxItemTest {
    @Test
    public fun testNotificareUserInboxItemSerialization() {
        val item = NotificareUserInboxItem(
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
            opened = true,
            expires = Date()
        )

        val convertedItem = NotificareUserInboxItem.fromJson(item.toJson())

        assertEquals(item, convertedItem)
    }

    @Test
    public fun testNotificareUserInboxItemSerializationWithNullProps() {
        val item = NotificareUserInboxItem(
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
            opened = true,
            expires = null
        )

        val convertedItem = NotificareUserInboxItem.fromJson(item.toJson())

        assertEquals(item, convertedItem)
    }
}
