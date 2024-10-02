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
public class NotificareUserInboxResponseTest {
    @Test
    public fun testNotificareUserInboxResponseSerialization() {
        val response = NotificareUserInboxResponse(
            count = 1,
            unread = 1,
            items = listOf(
                NotificareUserInboxItem(
                    id = "testId",
                    notification = NotificareNotification(
                        id = "testId",
                        type = "testType",
                        time = Date(),
                        title = "testTitle",
                        subtitle = "testSubtitle",
                        message = "testMessage"
                    ),
                    time = Date(),
                    opened = true,
                    expires = Date()
                )
            )
        )

        val convertedResponse = NotificareUserInboxResponse.fromJson(response.toJson())

        assertEquals(response, convertedResponse)
    }
}
