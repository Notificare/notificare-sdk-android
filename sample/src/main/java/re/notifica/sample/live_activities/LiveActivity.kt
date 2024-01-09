package re.notifica.sample.live_activities

enum class LiveActivity {
    COFFEE_BREWER;

    val identifier: String
        get() = when (this) {
            COFFEE_BREWER -> "coffee-brewer"
        }

    companion object {
        fun from(identifier: String): LiveActivity? {
            return values().firstOrNull { it.identifier == identifier }
        }
    }
}
