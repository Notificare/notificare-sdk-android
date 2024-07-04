package re.notifica.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import re.notifica.models.NotificareNotification.Action.Companion.TYPE_APP

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificareApplicationTest {
    @Test
    public fun testApplicationSerialization() {
        val application = NotificareApplication(
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

        val convertedApplication = NotificareApplication.fromJson(application.toJson())

        assertEquals(application, convertedApplication)
    }

    @Test
    public fun testInboxConfigSerialization() {
        val inboxConfig = NotificareApplication.InboxConfig(
            useInbox = true,
            useUserInbox = true,
            autoBadge = true
        )

        val convertedConfig = NotificareApplication.InboxConfig.fromJson(inboxConfig.toJson())

        assertEquals(inboxConfig, convertedConfig)
    }

    @Test
    public fun testRegionConfigSerialization() {
        val regionConfig = NotificareApplication.RegionConfig(
            proximityUUID = "testProximityUUID"
        )

        val convertedRegionConfig = NotificareApplication.RegionConfig.fromJson(regionConfig.toJson())

        assertEquals(regionConfig, convertedRegionConfig)
    }

    @Test
    public fun testUserDataFieldSerialization() {
        val userDataField = NotificareApplication.UserDataField(
            type = "testType",
            key = "testKey",
            label = "testLabel"
        )

        val convertedUserDataField = NotificareApplication.UserDataField.fromJson(userDataField.toJson())

        assertEquals(userDataField, convertedUserDataField)
    }

    @Test
    public fun testActionCategorySerialization() {
        val actionCategory = NotificareApplication.ActionCategory(
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

        val convertedActionCategory = NotificareApplication.ActionCategory.fromJson(actionCategory.toJson())

        assertEquals(actionCategory, convertedActionCategory)
    }
}
