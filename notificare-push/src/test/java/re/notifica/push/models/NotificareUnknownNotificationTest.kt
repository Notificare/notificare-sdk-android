package re.notifica.push.models

import android.net.Uri
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
            notification = NotificareUnknownNotification.Notification(
                title = "testTitle",
                titleLocalizationKey = "testLocalizationKey",
                titleLocalizationArgs = listOf("testLocalizationArgs"),
                body = "testBody",
                bodyLocalizationKey = "testBodyKey",
                bodyLocalizationArgs = listOf("testLocalizationArgs"),
                icon = "testIcon",
                imageUrl = Uri.parse("www.testuri.com"),
                sound = "testSound",
                tag = "testTag",
                color = "testColor",
                clickAction = "testClickAction",
                channelId = "testChannelId",
                link = Uri.parse("www.testuri.com"),
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
                lightSettings = listOf(1),
                vibrateSettings = listOf(1)
            ),
            data = mapOf("testKey" to "testValue")
        )

        val convertedNotification = NotificareUnknownNotification.fromJson(notification.toJson())

        assertEquals(notification, convertedNotification)
    }

    @Test
    public fun testNotificareUnknownNotificationSerializationWithNullProps() {
        val notification = NotificareUnknownNotification(
            messageId = null,
            messageType = null,
            senderId = null,
            collapseKey = null,
            from = null,
            to = null,
            sentTime = 1,
            ttl = 1,
            priority = 1,
            originalPriority = 1,
            notification = null,
            data = mapOf("testKey" to "testValue")
        )

        val convertedNotification = NotificareUnknownNotification.fromJson(notification.toJson())

        assertEquals(notification, convertedNotification)
    }

    @Test
    public fun testNotificationSerialization() {
        val notification = NotificareUnknownNotification.Notification(
            title = "testTitle",
            titleLocalizationKey = "testLocalizationKey",
            titleLocalizationArgs = listOf("testLocalizationArgs"),
            body = "testBody",
            bodyLocalizationKey = "testBodyKey",
            bodyLocalizationArgs = listOf("testLocalizationArgs"),
            icon = "testIcon",
            imageUrl = Uri.parse("www.testuri.com"),
            sound = "testSound",
            tag = "testTag",
            color = "testColor",
            clickAction = "testClickAction",
            channelId = "testChannelId",
            link = Uri.parse("www.testuri.com"),
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
            lightSettings = listOf(1),
            vibrateSettings = listOf(1)
        )

        val convertedNotification = NotificareUnknownNotification.Notification.fromJson(notification.toJson())

        assertEquals(notification, convertedNotification)
    }

    @Test
    public fun testNotificationSerializationWithNullProps() {
        val notification = NotificareUnknownNotification.Notification(
            title = null,
            titleLocalizationKey = null,
            titleLocalizationArgs = null,
            body = null,
            bodyLocalizationKey = null,
            bodyLocalizationArgs = null,
            icon = null,
            imageUrl = null,
            sound = null,
            tag = null,
            color = null,
            clickAction = null,
            channelId = null,
            link = null,
            ticker = null,
            sticky = true,
            localOnly = true,
            defaultSound = true,
            defaultVibrateSettings = true,
            defaultLightSettings = true,
            notificationPriority = null,
            visibility = null,
            notificationCount = null,
            eventTime = null,
            lightSettings = null,
            vibrateSettings = null
        )

        val convertedNotification = NotificareUnknownNotification.Notification.fromJson(notification.toJson())

        assertEquals(notification, convertedNotification)
    }
}
