package re.notifica.sample.live_activities.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CoffeeBrewerContentState(
    val state: CoffeeBrewingState,
    val remaining: Int,
)
