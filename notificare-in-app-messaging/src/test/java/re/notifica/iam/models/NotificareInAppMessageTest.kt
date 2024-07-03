package re.notifica.iam.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import re.notifica.iam.models.NotificareInAppMessage.Companion.TYPE_BANNER

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificareInAppMessageTest {
    @Test
    public fun testNotificareInAppMessageSerialization() {
        val inAppMessage = NotificareInAppMessage(
            id = "testId",
            name = "testName",
            type = TYPE_BANNER,
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
    public fun testActionSerialization() {
        val action = NotificareInAppMessage.Action(
            label = "testLabel",
            destructive = false,
            url = "testUrl"
        )

        val convertedAction = NotificareInAppMessage.Action.fromJson(action.toJson())

        assertEquals(action, convertedAction)
    }
}
