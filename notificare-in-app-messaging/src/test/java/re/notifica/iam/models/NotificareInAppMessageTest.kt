package re.notifica.iam.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificareInAppMessageTest {
    @Test
    public fun testNotificareInAppMessageSerialization() {
        val inAppMessage = NotificareInAppMessage(
            id = "testId",
            name = "testName",
            type = NotificareInAppMessage.TYPE_BANNER,
            context = listOf("testContext"),
            title = "testTitle",
            message = "testMessage",
            image = "testImage",
            landscapeImage = "testLandscapeImage",
            delaySeconds = 10,
            primaryAction = NotificareInAppMessage.Action(
                label = "testAction",
                destructive = true,
                url = "testUrl"
            ),
            secondaryAction = NotificareInAppMessage.Action(
                label = "testAction",
                destructive = true,
                url = "testUrl"
            )
        )

        val convertedInAppMessage = NotificareInAppMessage.fromJson(inAppMessage.toJson())

        assertEquals(inAppMessage, convertedInAppMessage)
    }

    @Test
    public fun testNotificareInAppMessageSerializationWithNullProps() {
        val inAppMessage = NotificareInAppMessage(
            id = "testId",
            name = "testName",
            type = NotificareInAppMessage.TYPE_BANNER,
            context = listOf("testContext"),
            title = null,
            message = null,
            image = null,
            landscapeImage = null,
            delaySeconds = 10,
            primaryAction = null,
            secondaryAction = null
        )

        val convertedInAppMessage = NotificareInAppMessage.fromJson(inAppMessage.toJson())

        assertEquals(inAppMessage, convertedInAppMessage)
    }

    @Test
    public fun testActionSerialization() {
        val action = NotificareInAppMessage.Action(
            label = "testLabel",
            destructive = false,
            url = "testUrl"
        )

        val convertedAction = NotificareInAppMessage.Action.fromJson(action.toJson())

        assertEquals(action, convertedAction)
    }

    @Test
    public fun testActionSerializationWithNullProps() {
        val action = NotificareInAppMessage.Action(
            label = null,
            destructive = false,
            url = null
        )

        val convertedAction = NotificareInAppMessage.Action.fromJson(action.toJson())

        assertEquals(action, convertedAction)
    }
}
