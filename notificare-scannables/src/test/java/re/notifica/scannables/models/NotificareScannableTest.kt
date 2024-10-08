package re.notifica.scannables.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import re.notifica.models.NotificareNotification
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificareScannableTest {
    @Test
    public fun testNotificareScannableSerialization() {
        val scannable = NotificareScannable(
            id = "testId",
            name = "testName",
            tag = "testTag",
            type = "testType",
            notification = NotificareNotification(
                id = "testNotification",
                partial = true,
                type = NotificareNotification.TYPE_NONE,
                time = Date(1),
                title = "testTitle",
                subtitle = "testSubtitle",
                message = "testMessage",
                attachments = listOf(
                    NotificareNotification.Attachment(
                        mimeType = "testMimeType",
                        uri = "testUri"
                    )
                ),
                extra = mapOf("testKey" to "testValue")
            )
        )

        val convertedScannable = NotificareScannable.fromJson(scannable.toJson())

        assertEquals(scannable, convertedScannable)
    }

    @Test
    public fun testNotificareScannableSerializationWithNullProps() {
        val scannable = NotificareScannable(
            id = "testId",
            name = "testName",
            tag = "testTag",
            type = "testType",
            notification = null
        )

        val convertedScannable = NotificareScannable.fromJson(scannable.toJson())

        assertEquals(scannable, convertedScannable)
    }
}
