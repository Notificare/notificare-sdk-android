package re.notifica.inbox.user.internal.moshi

import com.squareup.moshi.*
import okhttp3.internal.closeQuietly
import re.notifica.inbox.user.internal.responses.ConsumerUserInboxResponse
import re.notifica.inbox.user.internal.responses.RawUserInboxResponse
import re.notifica.inbox.user.models.NotificareUserInboxResponse
import re.notifica.internal.NotificareLogger

internal class UserInboxResponseAdapter {

    @FromJson
    fun fromJson(
        reader: JsonReader,
        rawAdapter: JsonAdapter<RawUserInboxResponse>,
        consumerAdapter: JsonAdapter<ConsumerUserInboxResponse>,
    ): NotificareUserInboxResponse {
        val clonedReader = reader.peekJson()

        try {
            val raw = requireNotNull(rawAdapter.nonNull().fromJson(reader))

            try {
                return NotificareUserInboxResponse(
                    count = raw.count,
                    unread = raw.unread,
                    items = raw.inboxItems.map { it.toModel() },
                )
            } finally {
                // The cloned reader is not going to be used.
                // Close it explicitly to prevent leaks.
                clonedReader.closeQuietly()
            }
        } catch (e: Exception) {
            NotificareLogger.debug("Unable to parse user inbox response from the raw format.", e)
        }

        try {
            val consumer = requireNotNull(consumerAdapter.nonNull().fromJson(clonedReader))
            return NotificareUserInboxResponse(
                count = consumer.count,
                unread = consumer.unread,
                items = consumer.items,
            )
        } catch (e: Exception) {
            NotificareLogger.debug("Unable to parse user inbox response from the consumer format.", e)
            throw e
        }
    }

    @ToJson
    fun toJson(
        writer: JsonWriter,
        response: NotificareUserInboxResponse,
        delegate: JsonAdapter<ConsumerUserInboxResponse>,
    ) {
        val consumer = ConsumerUserInboxResponse(
            count = response.count,
            unread = response.unread,
            items = response.items,
        )

        delegate.toJson(writer, consumer)
    }
}
