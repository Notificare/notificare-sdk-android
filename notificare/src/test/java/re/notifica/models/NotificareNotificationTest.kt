package re.notifica.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificareNotificationTest {
    @Test
    public fun testNotificareNotificationSerialization() {
        val notification = NotificareNotification(
            id = "testId",
            partial = true,
            type = NotificareNotification.TYPE_ALERT,
            time = Date(),
            title = "testTitle",
            subtitle = "testSubtitle",
            message = "testMessage",
            content = listOf(
                NotificareNotification.Content(
                    type = NotificareNotification.Content.TYPE_HTML,
                    data = "testData"
                )
            ),
            actions = listOf(
                NotificareNotification.Action(
                    type = NotificareNotification.Action.TYPE_APP,
                    label = "testLabel",
                    target = "testTarget",
                    camera = true,
                    keyboard = true,
                    destructive = true,
                    icon = NotificareNotification.Action.Icon(
                        android = "testAndroid",
                        ios = "testIos",
                        web = "testWeb"
                    )
                )
            ),
            attachments = listOf(
                NotificareNotification.Attachment(
                    mimeType = "testMimeType",
                    uri = "testUri"
                )
            ),
            extra = mapOf("testKey" to "testValue")
        )

        val convertedNotification = NotificareNotification.fromJson(notification.toJson())

        assertEquals(notification, convertedNotification)
    }

    @Test
    public fun testNotificareNotificationSerializationWithNullProps() {
        val notification = NotificareNotification(
            id = "testId",
            partial = true,
            type = NotificareNotification.TYPE_ALERT,
            time = Date(),
            title = null,
            subtitle = null,
            message = "testMessage",
        )

        val convertedNotification = NotificareNotification.fromJson(notification.toJson())

        assertEquals(notification, convertedNotification)
    }

    @Test
    public fun testContentSerialization() {
        val content = NotificareNotification.Content(
            type = NotificareNotification.Content.TYPE_HTML,
            data = "testData"
        )

        val convertedContent = NotificareNotification.Content.fromJson(content.toJson())

        assertEquals(content, convertedContent)
    }

    @Test
    public fun testActionSerialization() {
        val action = NotificareNotification.Action(
            type = NotificareNotification.Action.TYPE_APP,
            label = "testLabel",
            target = "testTarget",
            camera = true,
            keyboard = true,
            destructive = true,
            icon = NotificareNotification.Action.Icon(
                android = "testAndroid",
                ios = "testIos",
                web = "testWeb"
            )
        )

        val convertedAction = NotificareNotification.Action.fromJson(action.toJson())

        assertEquals(action, convertedAction)
    }

    @Test
    public fun testActionSerializationWithNullProps() {
        val action = NotificareNotification.Action(
            type = NotificareNotification.Action.TYPE_APP,
            label = "testLabel",
            target = null,
            camera = true,
            keyboard = true,
            destructive = null,
            icon = null
        )

        val convertedAction = NotificareNotification.Action.fromJson(action.toJson())

        assertEquals(action, convertedAction)
    }

    @Test
    public fun testIconSerialization() {
        val icon = NotificareNotification.Action.Icon(
            android = "testAndroid",
            ios = "testIos",
            web = "testWeb"
        )

        val convertedIcon = NotificareNotification.Action.Icon.fromJson(icon.toJson())

        assertEquals(icon, convertedIcon)
    }

    @Test
    public fun testIconSerializationWithNullProps() {
        val icon = NotificareNotification.Action.Icon(
            android = null,
            ios = null,
            web = null
        )

        val convertedIcon = NotificareNotification.Action.Icon.fromJson(icon.toJson())

        assertEquals(icon, convertedIcon)
    }

    @Test
    public fun testAttachmentSerialization() {
        val attachment = NotificareNotification.Attachment(
            mimeType = "testMimeType",
            uri = "testUri"
        )

        val convertedAttachment = NotificareNotification.Attachment.fromJson(attachment.toJson())

        assertEquals(attachment, convertedAttachment)
    }
}
