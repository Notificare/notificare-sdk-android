package re.notifica.loyalty.internal.storage.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import re.notifica.Notificare
import re.notifica.internal.moshi
import re.notifica.loyalty.models.NotificarePass

@Entity(
    tableName = "passes"
)
internal data class PassEntity(
    @PrimaryKey @ColumnInfo(name = "serial") val serial: String,
    @ColumnInfo(name = "pass_json") val json: String,
) {

    internal fun toModel(): NotificarePass {
        return checkNotNull(adapter.fromJson(json))
    }

    internal companion object {
        private val adapter = Notificare.moshi.adapter(NotificarePass::class.java)

        internal fun from(pass: NotificarePass): PassEntity {
            val json = adapter.toJson(pass)

            return PassEntity(
                serial = pass.serial,
                json = json,
            )
        }
    }
}
