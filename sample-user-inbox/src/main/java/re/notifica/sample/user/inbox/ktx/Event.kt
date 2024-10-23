package re.notifica.sample.user.inbox.ktx

sealed class Event {
    data class ShowSnackBar(val text: String) : Event()
}
