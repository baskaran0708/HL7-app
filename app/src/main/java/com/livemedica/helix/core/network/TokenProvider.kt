package com.livemedica.helix.core.network

/**
 * Supplies the bearer token for the future AWS backend.
 *
 * No token is ever hard-coded or committed: Phase 1 holds it in memory only, and Phase 2 will back
 * this with EncryptedSharedPreferences / the Android Keystore plus refresh-token rotation.
 */
interface TokenProvider {
    suspend fun accessToken(): String?
    suspend fun refreshToken(): String?
    suspend fun updateTokens(access: String, refresh: String)
    suspend fun clear()
}

/** Phase-1 stub. Deliberately volatile — nothing is persisted to disk. */
class InMemoryTokenProvider : TokenProvider {
    @Volatile private var access: String? = null
    @Volatile private var refresh: String? = null

    override suspend fun accessToken(): String? = access
    override suspend fun refreshToken(): String? = refresh

    override suspend fun updateTokens(access: String, refresh: String) {
        this.access = access
        this.refresh = refresh
    }

    override suspend fun clear() {
        access = null
        refresh = null
    }
}
