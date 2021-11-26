package re.notifica.loyalty.models

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.ktx.cast
import re.notifica.internal.moshi
import re.notifica.internal.parcelize.NotificareExtraParceler
import re.notifica.loyalty.internal.ktx.*
import java.text.ParseException
import java.util.*

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificarePass(
    val id: String,
    val type: PassType?,
    val version: Int,
    val passbook: String?,
    val template: String?,
    val serial: String,
    val barcode: String,
    val redeem: Redeem,
    val redeemHistory: List<Redemption>,
    val limit: Int,
    val token: String,
    val data: @WriteWith<NotificareExtraParceler> Map<String, Any> = mapOf(),
    val date: Date,
    val googlePaySaveLink: String?,
) : Parcelable {

    public val description: String?
        get() {
            return data["description"] as? String
        }

    public val logo: String?
        get() {
            return data["logo"] as? String
        }

    public val icon: String?
        get() {
            return data["icon"] as? String
        }

    public val isVoided: Boolean
        get() {
            return data["voided"] as? Boolean
                ?: false
        }

    public val expirationDate: Date?
        get() {
            val dateStr = data["expirationDate"] as? String
                ?: return null

            return try {
                NotificarePass.parseDate(dateStr)
            } catch (e: Exception) {
                // Unable to parse the expiration date.
                null
            }
        }

    public val isExpired: Boolean
        get() {
            if (isVoided) return true
            val expirationDate = expirationDate ?: return false
            return expirationDate.before(Date())
        }

    public val relevantDate: Date?
        get() {
            val dateStr = data["relevantDate"] as? String
                ?: return null

            return try {
                NotificarePass.parseDate(dateStr)
            } catch (e: Exception) {
                // Unable to parse the expiration date.
                null
            }
        }

    public val locations: List<PassbookLocation>
        get() {
            val list = data["locations"] as? List<*> ?: return emptyList()
            return list.filterIsInstance(Map::class.java)
                .mapNotNull { PassbookLocation.from(it.cast()) }
        }

    public val beacons: List<PassbookBeacon>
        get() {
            val list = data["beacons"] as? List<*> ?: return emptyList()
            return list.filterIsInstance(Map::class.java)
                .mapNotNull { PassbookBeacon.from(it.cast()) }
        }

    public val maxDistance: Double?
        get() {
            return data["maxDistance"] as? Double
        }

    public val headerFields: List<PassbookField>
        get() {
            val list = data["headerFields"] as? List<*> ?: return emptyList()
            return list.filterIsInstance(Map::class.java)
                .mapNotNull { PassbookField.from(it.cast()) }
        }

    public val primaryFields: List<PassbookField>
        get() {
            val list = data["primaryFields"] as? List<*> ?: return emptyList()
            return list.filterIsInstance(Map::class.java)
                .mapNotNull { PassbookField.from(it.cast()) }
        }

    public val secondaryFields: List<PassbookField>
        get() {
            val list = data["secondaryFields"] as? List<*> ?: return emptyList()
            return list.filterIsInstance(Map::class.java)
                .mapNotNull { PassbookField.from(it.cast()) }
        }

    public val auxiliaryFields: List<PassbookField>
        get() {
            val list = data["auxiliaryFields"] as? List<*> ?: return emptyList()
            return list.filterIsInstance(Map::class.java)
                .mapNotNull { PassbookField.from(it.cast()) }
        }

    public val backFields: List<PassbookField>
        get() {
            val list = data["backFields"] as? List<*> ?: return emptyList()
            return list.filterIsInstance(Map::class.java)
                .mapNotNull { PassbookField.from(it.cast()) }
        }


    public fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }

    public companion object {
        private val adapter = Notificare.moshi.adapter(NotificarePass::class.java)

        public fun fromJson(json: JSONObject): NotificarePass {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }

    public enum class Redeem {
        @Json(name = "once")
        ONCE,

        @Json(name = "limit")
        LIMIT,

        @Json(name = "always")
        ALWAYS;
    }

    public enum class PassType {
        @Json(name = "boarding")
        BOARDING,

        @Json(name = "coupon")
        COUPON,

        @Json(name = "ticket")
        TICKET,

        @Json(name = "generic")
        GENERIC,

        @Json(name = "card")
        CARD;
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class Redemption(
        val comments: String?,
        val date: Date,
    ) : Parcelable

    public data class PassbookBeacon(
        val major: Int?,
        val minor: Int?,
        val proximityUUID: String,
        val relevantText: String?,
    ) {

        internal companion object {
            internal fun from(data: Map<String, Any>): PassbookBeacon? {
                val proximityUUID = data["proximityUUID"] as? String ?: return null

                return PassbookBeacon(
                    major = data["major"] as? Int,
                    minor = data["minor"] as? Int,
                    proximityUUID = proximityUUID,
                    relevantText = data["relevantText"] as? String,
                )
            }
        }
    }

    public data class PassbookLocation(
        val latitude: Double,
        val longitude: Double,
        val altitude: Double?,
        val relevantText: String?,
    ) {

        internal companion object {
            internal fun from(data: Map<String, Any>): PassbookLocation? {
                val latitude = data["latitude"] as? Double ?: return null
                val longitude = data["longitude"] as? Double ?: return null

                return PassbookLocation(
                    latitude = latitude,
                    longitude = longitude,
                    altitude = data["altitude"] as? Double,
                    relevantText = data["relevantText"] as? String,
                )
            }
        }
    }

    public data class PassbookField(
        //
        // Standard field keys
        //
        val key: String,
        val label: String?,
        val value: String?,
        val attributedValue: String?,
        val changeMessage: String?,
        val dataDetectorTypes: List<DataDetectorTypes>,
        val textAlignment: TextAlignment,
        //
        // Date style keys
        //
        val dateStyle: DateStyle?,
        val ignoresTimeZone: Boolean,
        val isRelative: Boolean,
        val timeStyle: DateStyle?,
        //
        // Number style keys
        //
        val currencyCode: String?,
        val numberStyle: NumberStyle?,
    ) {

        val formattedValue: String?
            get() {
                return try {
                    when {
                        isDateField -> formatDate()
                        isCurrencyField -> formatCurrency()
                        isNumberField -> formatNumber()
                        else -> value
                    }
                } catch (e: ParseException) {
                    // Ignore, just use the value
                    value
                }
            }

        val parsedChangeMessage: String?
            get() {
                val changeMessage = this.changeMessage ?: return null
                if (changeMessage.isBlank()) return null

                val template = changeMessage.replace("%@", "%s")
                return String.format(template, formattedValue)
            }


        internal companion object {
            internal fun from(data: Map<String, Any>): PassbookField? {
                return PassbookField(
                    //
                    // Standard field keys
                    //
                    key = data["key"] as? String ?: return null,
                    label = data["label"] as? String,
                    value = data["value"]?.toString(),
                    attributedValue = data["attributedValue"]?.toString(),
                    changeMessage = data["changeMessage"] as? String,
                    dataDetectorTypes = mutableListOf<DataDetectorTypes>().apply {
                        val types = (data["dataDetectorTypes"] as? List<*>)
                            ?.mapNotNull { it as? String }

                        if (types != null) {
                            types.forEach { type ->
                                add(DataDetectorTypes.valueOf(type))
                            }
                        } else {
                            addAll(DataDetectorTypes.values())
                        }
                    },
                    textAlignment = data["textAlignment"]?.toString()?.let { TextAlignment.valueOf(it) }
                        ?: TextAlignment.PKTextAlignmentNatural,
                    //
                    // Date style keys
                    //
                    dateStyle = data["dateStyle"]?.toString()?.let { DateStyle.valueOf(it) },
                    ignoresTimeZone = data["ignoresTimeZone"] as? Boolean ?: false,
                    isRelative = data["isRelative"] as? Boolean ?: false,
                    timeStyle = data["timeStyle"]?.toString()?.let { DateStyle.valueOf(it) },
                    //
                    // Number style keys
                    //
                    currencyCode = data["currencyCode"] as? String,
                    numberStyle = data["numberStyle"]?.toString()?.let { NumberStyle.valueOf(it) },
                )
            }
        }

        /**
         * Possible values for the dataDetectorTypes attribute
         */
        public enum class DataDetectorTypes {
            PKDataDetectorTypePhoneNumber,
            PKDataDetectorTypeLink,
            PKDataDetectorTypeAddress,
            PKDataDetectorTypeCalendarEvent
        }

        /**
         * Possible values for the textAlignment attribute
         */
        public enum class TextAlignment {
            PKTextAlignmentLeft,
            PKTextAlignmentCenter,
            PKTextAlignmentRight,
            PKTextAlignmentNatural
        }

        /**
         * Possible values for the dateStyle attribute
         */
        public enum class DateStyle {
            PKDateStyleNone,
            PKDateStyleShort,
            PKDateStyleMedium,
            PKDateStyleLong,
            PKDateStyleFull
        }

        /**
         * Possible values for the numberStyle attribute
         */
        public enum class NumberStyle {
            PKNumberStyleDecimal,
            PKNumberStylePercent,
            PKNumberStyleScientific,
            PKNumberStyleSpellOut
        }
    }
}
