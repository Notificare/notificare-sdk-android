package com.google.api.client.auth.oauth2

import android.content.Context
import java.io.*

internal class GoogleStorageUtils(
    context: Context,
) {
    private val file = File(context.filesDir, StoredCredential::class.java.simpleName)

    internal fun loadStoredCredential(): StoredCredential? {
        if (!file.exists()) return null

        return try {
            val inputStream = FileInputStream(file)
            val keyValueMap: HashMap<String, ByteArray> = deserialize(inputStream)
            deserialize(ByteArrayInputStream(keyValueMap["Notificare"]))
        } catch (e: Exception) {
            null
        }
    }

    internal fun removeStoredCredential() {
        if (!file.exists()) return

        file.delete()
    }

    private fun <S : Serializable?> deserialize(inputStream: InputStream): S {
        return try {
            ObjectInputStream(inputStream).readObject() as S
        } catch (e: ClassNotFoundException) {
            throw IOException("Failed to deserialize object").apply {
                initCause(e)
            }
        } finally {
            inputStream.close()
        }
    }
}
