package re.notifica.scannables.internal.network.push

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import re.notifica.internal.network.push.NotificationResponse
import re.notifica.models.NotificareNotification
import re.notifica.models.NotificareNotification.Companion.TYPE_NONE
import re.notifica.scannables.models.NotificareScannable
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class ResponsesTest {
    @Test
    public fun testScannableToModel() {
        val expectedScannable = NotificareScannable(
            id = "testId",
            name = "testName",
            type = "testType",
            tag = "testTag",
            notification = NotificareNotification(
                id = "testId",
                partial = true,
                type = TYPE_NONE,
                time = Date(1),
                title = "testTitle",
                subtitle = "testSubtitle",
                message = "testMessage"
            )
        )

        val scannable = FetchScannableResponse.Scannable(
            _id = "testId",
            name = "testName",
            type = "testType",
            tag = "testTag",
            data = FetchScannableResponse.Scannable.ScannableData(
                notification = NotificationResponse.Notification(
                    id = "testId",
                    partial = true,
                    type = TYPE_NONE,
                    time = Date(1),
                    title = "testTitle",
                    subtitle = "testSubtitle",
                    message = "testMessage"
                )
            )
        ).toModel()

        assertEquals(expectedScannable, scannable)
    }
}
