package re.notifica.push.models

import android.net.Uri
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
        ).toNotification()

        assertEquals(expectedNotification, notification)
    }

    @Test
    public fun testUnknownRemoteMessageWithNullPropsToNotification() {
        val expectedNotification = NotificareUnknownNotification(
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

        val notification = NotificareUnknownRemoteMessage(
            messageId = null,
            sentTime = 1,
            collapseKey = null,
            ttl = 1,
            messageType = null,
            senderId = null,
            from = null,
            to = null,
            priority = 1,
            originalPriority = 1,
            notification = null,
            data = mapOf("testKey" to "testValue")
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
            attachments = listOf(
                NotificareNotification.Attachment(
                    mimeType = "testMimeType",
                    uri = "testUri"
                )
            ),
            extra = mapOf("testKey" to "testValue")
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
            attachment = NotificareNotification.Attachment(
                mimeType = "testMimeType",
                uri = "testUri"
            ),
            actionCategory = "testActionCategory",
            extra = mapOf("testKey" to "testValue"),
            inboxItemId = "testInboxItemId",
            inboxItemVisible = true,
            inboxItemExpires = 1,
            presentation = true,
            notify = true,
            sound = "testSound",
            lightsColor = "testColor",
            lightsOn = 1,
            lightsOff = 1
        ).toNotification()

        assertEquals(expectedNotification, notification)
    }

    @Test
    public fun testRemoteMessageWithNullPropsToNotification() {
        val expectedNotification = NotificareNotification(
            id = "testNotificationId",
            partial = true,
            type = "testNotificationType",
            time = Date(1),
            title = null,
            subtitle = null,
            message = "testAlert",
            content = emptyList(),
            actions = emptyList(),
            attachments = emptyList(),
            extra = mapOf("testKey" to "testValue")
        )

        val notification = NotificareNotificationRemoteMessage(
            messageId = null,
            sentTime = 1,
            collapseKey = null,
            ttl = 1,
            id = "testId",
            notificationId = "testNotificationId",
            notificationType = "testNotificationType",
            notificationChannel = null,
            notificationGroup = null,
            alert = "testAlert",
            alertTitle = null,
            alertSubtitle = null,
            attachment = null,
            actionCategory = null,
            extra = mapOf("testKey" to "testValue"),
            inboxItemId = null,
            inboxItemVisible = true,
            inboxItemExpires = null,
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
