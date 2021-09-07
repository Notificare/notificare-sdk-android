package re.notifica

public interface NotificareCallback<T> {
    public fun onSuccess(result: T)

    public fun onFailure(e: Exception)
}
