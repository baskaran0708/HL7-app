package com.livemedica.helix.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Attaches `Authorization: Bearer <token>` to outgoing requests.
 *
 * Installed nowhere yet — Phase 1 makes no calls. It exists so the auth seam is fixed before the
 * backend arrives, and so it is obvious that the token comes from [TokenProvider] rather than from
 * source, a build config, or a constant.
 */
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Auth endpoints must not carry a stale token, or a refresh can fail on the token it is
        // trying to replace.
        if (request.url.encodedPath.contains("/auth/")) return chain.proceed(request)

        // OkHttp interceptors are blocking; the token read is a fast in-memory/keystore lookup.
        val token = runBlocking { tokenProvider.accessToken() } ?: return chain.proceed(request)

        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build(),
        )
    }
}
