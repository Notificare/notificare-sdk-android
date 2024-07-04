package re.notifica.push.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import re.notifica.models.NotificareNotification
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificareRemoteMessageTest {
    @Test
    public fun testUnknownRemoteMessageToNotification() {
        val expectedNotification = NotificareUnknownNotification(
            messageId = "testMessageId",
            messageType = "testMessageType",
            senderId = "testSenderId",
            collapseKey = "testCollapseKey",
            from = "testFrom",
            to = "testTo",
            sentTime = 1,
            ttl = 1,
            priority = 1,
            originalPriority = 1,
            notification = null,
            data = mapOf()
        )

        val notification = NotificareUnknownRemoteMessage(
            messageId = "testMessageId",
            sentTime = 1,
            collapseKey = "testCollapseKey",
            ttl = 1,
            messageType = "testMessageType",
            senderId = "testSenderId",
            from = "testFrom",
            to = "testTo",
            priority = 1,
            originalPriority = 1,
            notification = null,
            data = mapOf()
        ).toNotification()

        assertEquals(expectedNotification, notification)
    }

    @Test
    public fun testRemoteMessageToNotification() {
        val expectedNotification = NotificareNotification(
            id = "testNotificationId",
            partial = true,
            type = "testNotificationType",
            time = Date(1),
            title = "testAlertTitle",
            subtitle = "testAlertSubtitle",
            message = "testAlert",
            content = emptyList(),
            actions = emptyList(),
            attachments = emptyList(),
            extra = mapOf()
        )

        val notification = NotificareNotificationRemoteMessage(
            messageId = "testMessageId",
            sentTime = 1,
            collapseKey = "testCollapseKey",
            ttl = 1,
            id = "testId",
            notificationId = "testNotificationId",
            notificationType = "testNotificationType",
            notificationChannel = "testNotificationChannel",
            notificationGroup = "testNotificationGroup",
            alert = "testAlert",
            alertTitle = "testAlertTitle",
            alertSubtitle = "testAlertSubtitle",
            attachment = null,
            actionCategory = "testActionCategory",
            extra = mapOf(),
            inboxItemId = "testInboxItemId",
            inboxItemVisible = true,
            inboxItemExpires = 1,
            presentation = true,
            notify = true,
            sound = null,
            lightsColor = null,
            lightsOn = null,
            lightsOff = null
        ).toNotification()

        assertEquals(expectedNotification, notification)
    }
}
