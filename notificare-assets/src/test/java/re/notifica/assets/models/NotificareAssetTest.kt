package re.notifica.assets.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificareAssetTest {
    @Test
    public fun testAssetSerialization() {
        val asset = NotificareAsset(
            title = "testTitle",
            description = "testDescription",
            key = "testKey",
            url = "testUrl",
            button = NotificareAsset.Button(
                label = "testLabel",
                action = "testAction"
            ),
            metaData = NotificareAsset.MetaData(
                originalFileName = "testOriginalFileName",
                contentType = "testContentType",
                contentLength = 1
            ),
            extra = mapOf("testKey" to "testValue")
        )

        val convertedAsset = NotificareAsset.fromJson(asset.toJson())

        assertEquals(asset, convertedAsset)
    }

    @Test
    public fun testAssetSerializationWithNullProps() {
        val asset = NotificareAsset(
            title = "testTitle",
            description = null,
            key = null,
            url = null,
            button = null,
            metaData = null
        )

        val convertedAsset = NotificareAsset.fromJson(asset.toJson())

        assertEquals(asset, convertedAsset)
    }

    @Test
    public fun testButtonSerialization() {
        val button = NotificareAsset.Button(
            label = "testLabel",
            action = "testAction"
        )

        val convertedButton = NotificareAsset.Button.fromJson(button.toJson())

        assertEquals(button, convertedButton)
    }

    @Test
    public fun testButtonSerializationWithNullProps() {
        val button = NotificareAsset.Button(
            label = null,
            action = null
        )

        val convertedButton = NotificareAsset.Button.fromJson(button.toJson())

        assertEquals(button, convertedButton)
    }

    @Test
    public fun testMetaDataSerialization() {
        val metaData = NotificareAsset.MetaData(
            originalFileName = "testOriginalFileName",
            contentType = "testContentType",
            contentLength = 0
        )

        val convertedMetaData = NotificareAsset.MetaData.fromJson(metaData.toJson())

        assertEquals(metaData, convertedMetaData)
    }
}
