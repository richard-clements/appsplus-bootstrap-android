package uk.co.appsplus.bootstrap.network.authenticator

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import uk.co.appsplus.bootstrap.network.models.AuthToken
import uk.co.appsplus.bootstrap.network.models.SimpleAuthToken
import uk.co.appsplus.bootstrap.network.models.TokenRefresh

interface TokenRefreshApi {
    suspend fun createNewToken(
        tokenRefresh: TokenRefresh,
        authorization: String
    ): AuthToken
}

interface SimpleAuthTokenRefreshApi : TokenRefreshApi {
    @POST("refresh")
    suspend fun refreshToken(
        @Body tokenRefresh: TokenRefresh,
        @Header("Authorization") authorization: String,
    ): SimpleAuthToken

    override suspend fun createNewToken(
        tokenRefresh: TokenRefresh,
        authorization: String
    ): AuthToken = refreshToken(tokenRefresh, authorization)
}
