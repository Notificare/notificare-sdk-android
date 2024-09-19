package re.notifica.utilities.ktx

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class PackageManagerTests {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager

    @Before
    public fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        packageManager = context.packageManager
    }

    @Test
    public fun testActivityInfo() {
        val componentName = ComponentName(context.packageName, "re.notifica.utilities.ktx.MainActivity")

        Robolectric.buildActivity(MainActivity::class.java).get()

        val result: ActivityInfo = packageManager.activityInfo(componentName, 0)

        assertEquals("re.notifica.utilities.ktx.MainActivity", result.name)
    }

    @Test
    public fun testApplicationInfo() {
        val packageName = context.packageName

        val result: ApplicationInfo = packageManager.applicationInfo(packageName, 0)

        assertEquals(packageName, result.packageName)
    }

    @Test
    public fun testPackageInfo() {
        val packageName = context.packageName

        val result: PackageInfo = packageManager.packageInfo(packageName, 0)

        assertEquals(packageName, result.packageName)
    }
}

private class MainActivity : Activity()
