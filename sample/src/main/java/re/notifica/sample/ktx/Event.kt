package re.notifica.sample.ktx

sealed class Event {
    data class ShowSnackBar(val text: String) : Event()
}
