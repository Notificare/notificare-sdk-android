package re.notifica

interface NotificareCallback<T> {
    fun onSuccess(result: T)

    fun onFailure(e: Exception)
}
