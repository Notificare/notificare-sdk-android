package re.notifica.assets.internal.network.push

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import re.notifica.assets.models.NotificareAsset

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class ResponsesTest {
    @Test
    public fun testAssetToModel() {
        val expectedAsset = NotificareAsset(
            title = "testTitle",
            description = "testDescription",
            key = "testKey",
            url = null,
            button = NotificareAsset.Button(
                label = "testLabel",
                action = "testAction"
            ),
            metaData = NotificareAsset.MetaData(
                originalFileName = "testOriginalFileName",
                contentType = "testContentType",
                contentLength = 1
            ),
        )

        val asset = FetchAssetsResponse.Asset(
            title = "testTitle",
            description = "testDescription",
            key = "testKey",
            url = null,
            button = FetchAssetsResponse.Asset.Button(
                label = "testLabel",
                action = "testAction"
            ),
            metaData = FetchAssetsResponse.Asset.MetaData(
                originalFileName = "testOriginalFileName",
                contentType = "testContentType",
                contentLength = 1
            )
        ).toModel()

        assertEquals(expectedAsset, asset)
    }
}
