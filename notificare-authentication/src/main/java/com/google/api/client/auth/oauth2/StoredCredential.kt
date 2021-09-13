package com.google.api.client.auth.oauth2

import java.io.Serializable

internal data class StoredCredential(
    val accessToken: String,
    val refreshToken: String,
    val expirationTimeMilliseconds: Long,
) : Serializable {

    internal companion object {

        @JvmStatic
        private val serialVersionUID = 1L
    }
}
