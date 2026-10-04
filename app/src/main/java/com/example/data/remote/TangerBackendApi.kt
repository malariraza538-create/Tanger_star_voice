package com.example.data.remote

import com.example.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

data class OtpRequestPayload(
    val countryCode: String,
    val phoneNumber: String
)

data class GiftTransactionPayload(
    val idempotencyKey: String,
    val senderUserId: String,
    val receiverUserId: String,
    val roomId: String,
    val giftId: String,
    val quantity: Int
)

data class RoomBannerPayload(
    val roomId: String,
    val actorUserId: String,
    val title: String,
    val messageText: String,
    val iconName: String,
    val bannerStyle: String,
    val durationSeconds: Int
)

data class DiamondConversionPayload(
    val idempotencyKey: String,
    val userId: String,
    val diamondsToExchange: Long
)

data class VipDailyBonusClaimPayload(
    val idempotencyKey: String,
    val userId: String
)

data class SocialPostPayload(
    val authorUserId: String,
    val caption: String,
    val mediaCdnUrl: String,
    val mediaType: String,
    val locationTag: String
)

data class StoryPayload(
    val authorUserId: String,
    val captionText: String,
    val mediaCdnUrl: String,
    val backgroundTheme: String
)

data class ReelPayload(
    val creatorUserId: String,
    val title: String,
    val audioTrackName: String,
    val mediaCdnUrl: String,
    val durationSec: Int
)

data class LiveStreamPayload(
    val hostUserId: String,
    val streamTitle: String,
    val category: String,
    val isPkBattle: Boolean,
    val pkOpponentUserId: String?
)

data class CloudMediaUploadPayload(
    val ownerUserId: String,
    val assetType: String,
    val mimeType: String,
    val sha256Checksum: String,
    val sizeBytes: Long
)

interface TangerBackendApi {
    @POST("auth/otp/request")
    suspend fun requestOtp(
        @Body payload: OtpRequestPayload
    ): Response<Map<String, String>>

    @GET("wallet/{userId}/summary")
    suspend fun getWalletSummary(
        @Path("userId") userId: String
    ): Response<Map<String, Any>>

    @GET("wallet/{userId}/transactions")
    suspend fun getWalletTransactionHistory(
        @Path("userId") userId: String
    ): Response<List<Map<String, Any>>>

    @POST("wallet/diamonds/exchange")
    suspend fun exchangeDiamondsToCoinsAtomic(
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Body payload: DiamondConversionPayload
    ): Response<Map<String, Any>>

    @POST("wallet/vip/daily-bonus")
    suspend fun claimVipDailyBonusAtomic(
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Body payload: VipDailyBonusClaimPayload
    ): Response<Map<String, Any>>

    @POST("wallet/gifts/send")
    suspend fun sendGiftAtomic(
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Body payload: GiftTransactionPayload
    ): Response<Map<String, String>>

    @POST("rooms/{roomId}/top-banner")
    suspend fun publishRoomTopBanner(
        @Path("roomId") roomId: String,
        @Body payload: RoomBannerPayload
    ): Response<Map<String, String>>

    @GET("rankings/{category}")
    suspend fun getServerRankings(
        @Path("category") category: String
    ): Response<List<Map<String, String>>>

    @POST("social/posts")
    suspend fun publishSocialPost(
        @Body payload: SocialPostPayload
    ): Response<Map<String, String>>

    @POST("social/stories")
    suspend fun publishStory(
        @Body payload: StoryPayload
    ): Response<Map<String, String>>

    @POST("social/reels")
    suspend fun publishReel(
        @Body payload: ReelPayload
    ): Response<Map<String, String>>

    @POST("live/streams/start")
    suspend fun startLiveStream(
        @Body payload: LiveStreamPayload
    ): Response<Map<String, String>>

    @POST("storage/media/register")
    suspend fun registerCloudMediaAsset(
        @Body payload: CloudMediaUploadPayload
    ): Response<Map<String, String>>

    @POST("admin/backups/create")
    suspend fun triggerProductionBackup(
        @Header("X-Owner-RBAC-Key") ownerRbacKey: String
    ): Response<Map<String, String>>
}

object TangerNetworkModule {
    fun provideApi(sessionTokenProvider: () -> String?): TangerBackendApi {
        val authInterceptor = Interceptor { chain ->
            val builder = chain.request().newBuilder()
                .addHeader("X-Tanger-Client", "Android-Mobile-1.0")
            sessionTokenProvider()?.let { token ->
                builder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(builder.build())
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val rawUrl = BuildConfig.TANGER_API_BASE_URL.ifBlank { "https://api.tangerstarvoice.global/v1/" }
        val baseUrl = if (rawUrl.endsWith("/")) rawUrl else "$rawUrl/"

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(TangerBackendApi::class.java)
    }
}
