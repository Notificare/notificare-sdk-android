package re.notifica.geo.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificareRegionTest {
    @Test
    public fun testNotificareRegionSerialization() {
        val region = NotificareRegion(
            id = "testId",
            name = "testName",
            description = "testDescription",
            referenceKey = "testReferenceKey",
            geometry = NotificareRegion.Geometry(
                type = "testType",
                coordinate = NotificareRegion.Coordinate(
                    latitude = 1.5,
                    longitude = 1.5,
                )
            ),
            advancedGeometry = NotificareRegion.AdvancedGeometry(
                type = "testType",
                coordinates = listOf(
                    NotificareRegion.Coordinate(
                        latitude = 1.5,
                        longitude = 1.5,
                    )
                )
            ),
            major = 1,
            distance = 1.5,
            timeZone = "testTimeZone",
            timeZoneOffset = 1.5
        )

        val convertedRegion = NotificareRegion.fromJson(region.toJson())

        assertEquals(region, convertedRegion)
    }

    @Test
    public fun testGeometrySerialization() {
        val geometry = NotificareRegion.Geometry(
            type = "testType",
            coordinate = NotificareRegion.Coordinate(
                latitude = 1.5,
                longitude = 1.5,
            )
        )

        val convertedGeometry = NotificareRegion.Geometry.fromJson(geometry.toJson())

        assertEquals(geometry, convertedGeometry)
    }

    @Test
    public fun testAdvancedGeometrySerialization() {
        val advancedGeometry = NotificareRegion.AdvancedGeometry(
            type = "testType",
            coordinates = listOf(
                NotificareRegion.Coordinate(
                    latitude = 1.5,
                    longitude = 1.5,
                )
            )
        )

        val convertedAdvancedGeometry = NotificareRegion.AdvancedGeometry.fromJson(advancedGeometry.toJson())

        assertEquals(advancedGeometry, convertedAdvancedGeometry)
    }

    @Test
    public fun testCoordinateSerialization() {
        val coordinate = NotificareRegion.Coordinate(
            latitude = 1.5,
            longitude = 1.5,
        )

        val convertedCoordinate = NotificareRegion.Coordinate.fromJson(coordinate.toJson())

        assertEquals(coordinate, convertedCoordinate)
    }
}
