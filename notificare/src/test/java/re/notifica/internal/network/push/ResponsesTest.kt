package re.notifica.internal.network.push

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareNotification
import re.notifica.models.NotificareNotification.Action.Companion.TYPE_APP
import re.notifica.models.NotificareNotification.Companion.TYPE_NONE
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class ResponsesTest {
    @Test
    public fun testApplicationToModel() {
        val expectedApplication = NotificareApplication(
            id = "testId",
            name = "testName",
            category = "testCategory",
            services = mapOf(),
            inboxConfig = NotificareApplication.InboxConfig(
                useInbox = true,
                useUserInbox = true,
                autoBadge = true
            ),
            regionConfig = NotificareApplication.RegionConfig(
                proximityUUID = "testProximityUUID"
            ),
            userDataFields = listOf(
                NotificareApplication.UserDataField(
                    type = "testType",
                    key = "testKey",
                    label = "testLabel"
                )
            ),
            actionCategories = listOf(
                NotificareApplication.ActionCategory(
                    type = "testType",
                    name = "testName",
                    description = "testDescription",
                    actions = listOf(
                        NotificareNotification.Action(
                            type = TYPE_APP,
                            label = "testLabel",
                            target = "",
                            camera = true,
                            keyboard = true,
                            destructive = true,
                            icon = NotificareNotification.Action.Icon(
                                android = "testAndroid",
                                ios = "testIos",
                                web = "testWeb"
                            )
                        )
                    )
                )
            )
        )

        val application = ApplicationResponse.Application(
            id = "testId",
            name = "testName",
            category = "testCategory",
            services = mapOf(),
            inboxConfig = NotificareApplication.InboxConfig(
                useInbox = true,
                useUserInbox = true,
                autoBadge = true
            ),
            regionConfig = NotificareApplication.RegionConfig(
                proximityUUID = "testProximityUUID"
            ),
            userDataFields = listOf(
                NotificareApplication.UserDataField(
                    type = "testType",
                    key = "testKey",
                    label = "testLabel"
                )
            ),
            actionCategories = listOf(
                ApplicationResponse.Application.ActionCategory(
                    type = "testType",
                    name = "testName",
                    description = "testDescription",
                    actions = listOf(
                        NotificationResponse.Notification.Action(
                            type = TYPE_APP,
                            label = "testLabel",
                            target = "",
                            camera = true,
                            keyboard = true,
                            destructive = true,
                            icon = NotificareNotification.Action.Icon(
                                android = "testAndroid",
                                ios = "testIos",
                                web = "testWeb"
                            )
                        )
                    )
                )
            )
        ).toModel()

        assertEquals(expectedApplication, application)
    }

    @Test
    public fun testNotificationToModel() {
        val expectedNotification = NotificareNotification(
            id = "testId",
            partial = true,
            type = TYPE_NONE,
            time = Date(1),
            title = "testTitle",
            subtitle = "testSubtitle",
            message = "testMessage"
        )

        val notification = NotificationResponse.Notification(
            id = "testId",
            partial = true,
            type = TYPE_NONE,
            time = Date(1),
            title = "testTitle",
            subtitle = "testSubtitle",
            message = "testMessage"
        ).toModel()

        assertEquals(expectedNotification, notification)
    }

    @Test
    public fun testActionToModel() {
        val expectedAction = NotificareNotification.Action(
            type = TYPE_APP,
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

        val action = NotificationResponse.Notification.Action(
            type = TYPE_APP,
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
        ).toModel()

        assertEquals(expectedAction, action)
    }
}
