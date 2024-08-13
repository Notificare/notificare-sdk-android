package re.notifica.push.models

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificarePushSubscriptionTest {

    @Test
    public fun testSubscriptionSerialization() {
        val subscription = NotificarePushSubscription(
            token = "foo",
        )

        val encoded = subscription.toJson()
        val decoded = NotificarePushSubscription.fromJson(encoded)

        Assert.assertEquals(subscription, decoded)
    }
}
