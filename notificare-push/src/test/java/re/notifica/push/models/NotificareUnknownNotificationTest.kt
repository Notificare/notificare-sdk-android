package re.notifica.push.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificareUnknownNotificationTest {
    @Test
    public fun testNotificareUnknownNotificationSerialization() {
        val notification = NotificareUnknownNotification(
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

        val convertedNotification = NotificareUnknownNotification.fromJson(notification.toJson())

        assertEquals(notification, convertedNotification)
    }

    @Test
    public fun testNotificationSerialization() {
        val notification = NotificareUnknownNotification.Notification(
            title = "testTitle",
            titleLocalizationKey = "testLocalizationKey",
            titleLocalizationArgs = null,
            body = "testBody",
            bodyLocalizationKey = "testBodyKey",
            bodyLocalizationArgs = null,
            icon = "testIcon",
            imageUrl = null,
            sound = "testSound",
            tag = "testTag",
            color = "testColor",
            clickAction = "testClickAction",
            channelId = "testChannelId",
            link = null,
            ticker = "testTicker",
            sticky = true,
            localOnly = true,
            defaultSound = true,
            defaultVibrateSettings = true,
            defaultLightSettings = true,
            notificationPriority = 1,
            visibility = 1,
            notificationCount = 1,
            eventTime = 1,
            lightSettings = null,
            vibrateSettings = null
        )

        val convertedNotification = NotificareUnknownNotification.Notification.fromJson(notification.toJson())

        assertEquals(notification, convertedNotification)
    }
}
