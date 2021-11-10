package re.notifica.loyalty.internal.ktx

import re.notifica.loyalty.models.NotificarePass
import java.text.DateFormat
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

internal val NotificarePass.PassbookField.isDateField: Boolean
    get() = dateStyle != null && timeStyle != null

internal val NotificarePass.PassbookField.isCurrencyField: Boolean
    get() = !currencyCode.isNullOrBlank() && numberStyle == null && isNumeric(value)

internal val NotificarePass.PassbookField.isNumberField: Boolean
    get() = numberStyle != null && currencyCode.isNullOrBlank() && isNumeric(value)


internal fun NotificarePass.PassbookField.formatDate(): String? {
    val value = value ?: return null
    val date = NotificarePass.parseDate(value) ?: return null
    val inputFormat = getDateFormat(value) ?: return null

    val dateStyle = when (dateStyle) {
        null -> DateFormat.DEFAULT
        NotificarePass.PassbookField.DateStyle.PKDateStyleNone -> null
        NotificarePass.PassbookField.DateStyle.PKDateStyleShort -> DateFormat.SHORT
        NotificarePass.PassbookField.DateStyle.PKDateStyleMedium -> DateFormat.MEDIUM
        NotificarePass.PassbookField.DateStyle.PKDateStyleLong -> DateFormat.LONG
        NotificarePass.PassbookField.DateStyle.PKDateStyleFull -> DateFormat.FULL
    }

    val timeStyle = when (timeStyle) {
        null -> DateFormat.DEFAULT
        NotificarePass.PassbookField.DateStyle.PKDateStyleNone -> null
        NotificarePass.PassbookField.DateStyle.PKDateStyleShort -> DateFormat.SHORT
        NotificarePass.PassbookField.DateStyle.PKDateStyleMedium -> DateFormat.MEDIUM
        NotificarePass.PassbookField.DateStyle.PKDateStyleLong -> DateFormat.LONG
        NotificarePass.PassbookField.DateStyle.PKDateStyleFull -> DateFormat.FULL
    }

    val outputFormat: DateFormat = when {
        dateStyle != null && timeStyle != null -> DateFormat.getDateTimeInstance(dateStyle, timeStyle)
        dateStyle != null -> DateFormat.getDateInstance(dateStyle)
        timeStyle != null -> DateFormat.getTimeInstance(timeStyle)
        else -> return null
    }

    // TODO retrieve timezone info from original date string
    if (this.ignoresTimeZone) {
        outputFormat.timeZone = inputFormat.timeZone
    } else {
        outputFormat.timeZone = TimeZone.getDefault()
    }

    // todo handle isRelative property

    return outputFormat.format(date)
}

internal fun NotificarePass.PassbookField.formatCurrency(): String? {
    val currencyCode = currencyCode ?: return null
    val value = this.value?.toDoubleOrNull() ?: return null

    return NumberFormat.getCurrencyInstance()
        .apply { this.currency = Currency.getInstance(currencyCode) }
        .format(value)
}

internal fun NotificarePass.PassbookField.formatNumber(): String? {
    val value = this.value?.toDoubleOrNull() ?: return null

    // TODO handle numberStyle property
    return NumberFormat.getNumberInstance()
        .format(value)
}


@Throws(ParseException::class)
internal fun NotificarePass.Companion.parseDate(dateStr: String, ignoreTimeZone: Boolean = false): Date? {
    var dateStr = dateStr

    if (dateStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}Z$".toRegex()) ||
        dateStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$".toRegex()) ||
        dateStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z$".toRegex())
    ) {
        // replace HH:mm:ss.SSSZ with HH:mm:ss.SSS+0000
        dateStr = dateStr.substring(0, dateStr.length - 1) + "+0000"
    } else if (dateStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}[+\\-]\\d{2}:\\d{2}$".toRegex()) ||
        dateStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+\\-]\\d{2}:\\d{2}$".toRegex()) ||
        dateStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}[+\\-]\\d{2}:\\d{2}$".toRegex())
    ) {
        // replace HH:mm:ss.SSS+ZZ:ZZ with HH:mm:ss.SSS+ZZZZ
        dateStr = dateStr.substring(0, dateStr.length - 3) + dateStr.substring(dateStr.length - 2)
    }

    return getDateFormat(dateStr)?.parse(dateStr)
}

internal fun NotificarePass.getUpdatedFields(oldPass: NotificarePass): List<NotificarePass.PassbookField> {
    val auxiliaryFields = auxiliaryFields
        .filter { newField ->
            val oldField = oldPass.auxiliaryFields.firstOrNull { it.key == newField.key }

            return@filter !newField.changeMessage.isNullOrBlank() &&
                oldField != null &&
                newField.value != oldField.value
        }

    val headerFields = headerFields
        .filter { newField ->
            val oldField = oldPass.headerFields.firstOrNull { it.key == newField.key }

            return@filter !newField.changeMessage.isNullOrBlank() &&
                oldField != null &&
                newField.value != oldField.value
        }

    val backFields = backFields
        .filter { newField ->
            val oldField = oldPass.backFields.firstOrNull { it.key == newField.key }

            return@filter !newField.changeMessage.isNullOrBlank() &&
                oldField != null &&
                newField.value != oldField.value
        }

    val primaryFields = primaryFields
        .filter { newField ->
            val oldField = oldPass.primaryFields.firstOrNull { it.key == newField.key }

            return@filter !newField.changeMessage.isNullOrBlank() &&
                oldField != null &&
                newField.value != oldField.value
        }

    val secondaryFields = secondaryFields
        .filter { newField ->
            val oldField = oldPass.secondaryFields.firstOrNull { it.key == newField.key }

            return@filter !newField.changeMessage.isNullOrBlank() &&
                oldField != null &&
                newField.value != oldField.value
        }

    return auxiliaryFields + headerFields + backFields + primaryFields + secondaryFields
}


private fun isNumeric(text: String?): Boolean {
    return text?.matches("\\d+(\\.\\d+)?".toRegex()) ?: false
}

private fun getDateFormat(dateStr: String): DateFormat? {
    val dateFormat: String = when {
        dateStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$".toRegex()) -> "yyyy-MM-dd'T'HH:mm"
        dateStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}Z$".toRegex()) ||
            dateStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}[+\\-]\\d{2}:\\d{2}$".toRegex()) ||
            dateStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}[+\\-]\\d{4}$".toRegex()) -> "yyyy-MM-dd'T'HH:mmZ"
        dateStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$".toRegex()) -> "yyyy-MM-dd'T'HH:mm:ss"
        dateStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$".toRegex()) ||
            dateStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+\\-]\\d{2}:\\d{2}$".toRegex()) ||
            dateStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+\\-]\\d{4}$".toRegex()) -> "yyyy-MM-dd'T'HH:mm:ssZ"
        dateStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}$".toRegex()) -> "yyyy-MM-dd'T'HH:mm:ss.SSS"
        dateStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z$".toRegex()) ||
            dateStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}[+\\-]\\d{2}:\\d{2}$".toRegex()) ||
            dateStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}[+\\-]\\d{4}$".toRegex()) -> "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        else -> return null
    }

    return SimpleDateFormat(dateFormat, Locale.US)
}
