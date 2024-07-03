package re.notifica.loyalty.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificarePassTest {
    @Test
    public fun testNotificarePassSerialization() {
        val pass = NotificarePass(
            id = "testId",
            type = NotificarePass.PassType.BOARDING,
            version = 1,
            passbook = "testPassbook",
            template = "testTemplate",
            serial = "testSerial",
            barcode = "testBarcode",
            redeem = NotificarePass.Redeem.ONCE,
            redeemHistory = listOf(
                NotificarePass.Redemption(
                    comments = "testComments",
                    date = Date()
                )
            ),
            limit = 1,
            token = "testToken",
            date = Date(),
            googlePaySaveLink = "testGooglePaySaveLink"
        )

        val convertedPass = NotificarePass.fromJson(pass.toJson())

        assertEquals(pass, convertedPass)
    }
}
