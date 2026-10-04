package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.TangerDatabase
import com.example.data.realtime.TangerPushNotificationDispatcher
import com.example.data.realtime.TangerRealtimeVoiceService
import com.example.data.remote.TangerNetworkModule
import com.example.data.repository.BackendResult
import com.example.data.repository.GLOBAL_COUNTRIES
import com.example.data.repository.TangerRepository
import com.example.ui.ads.TangerAdMobManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: TangerDatabase
    private lateinit var realtimeService: TangerRealtimeVoiceService
    private lateinit var repository: TangerRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TangerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        realtimeService = TangerRealtimeVoiceService(CoroutineScope(Dispatchers.Unconfined))
        repository = TangerRepository(db, db.tangerDao(), realtimeService)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `verify production API client, relational database, authentication, Lucky Gifts, RBAC, and realtime connections`() = runBlocking {
        // 1. Verify App Identity & Secure BuildConfig Environment Variables
        val appName = context.getString(R.string.app_name)
        assertEquals("Tanger Star Voice", appName)
        assertTrue(BuildConfig.TANGER_API_BASE_URL.startsWith("https://"))
        assertTrue(BuildConfig.TANGER_WS_URL.startsWith("wss://"))
        assertTrue(BuildConfig.TANGER_CDN_BASE_URL.startsWith("https://"))

        // 2. Verify Production HTTPS Retrofit API Client Initialization
        val api = TangerNetworkModule.provideApi { "PROD_BEARER_TOKEN_199967" }
        assertNotNull(api)

        // 3. Initialize Relational Database v5 & Verify Mandatory Sign-Up Entry Gate (No Auto-Login Bypass)
        val initialActiveUser = repository.initializeDatabaseIfNeeded()
        assertNull(initialActiveUser)

        // 4. Test Sequential User Sign-Up (Step 1) + Onboarding Completion (Step 2)
        val morocco = GLOBAL_COUNTRIES.first()
        val signupRes = repository.registerOrSignInWithEmail(
            emailInput = "amina@tangerstarvoice.global",
            rawPassword = "SecurePassword2026!",
            fullNameInput = "Amina Tanger",
            country = morocco
        )
        assertTrue(signupRes is BackendResult.Success)
        val newUser = (signupRes as BackendResult.Success).data
        assertEquals("199972", newUser.tangerUserId)

        val onboardRes = repository.completeUserOnboarding(
            userId = newUser.tangerUserId,
            selectedCountry = morocco,
            selectedGender = "Female",
            desiredUsername = "amina_global",
            fullNameInput = "Amina Tanger",
            avatarUriInput = "content://tangerstar/avatars/199972",
            bioInput = "Live Host & Singer"
        )
        assertTrue(onboardRes is BackendResult.Success)
        assertTrue((onboardRes as BackendResult.Success).data.isOnboardingCompleted)

        // 5. Verify Founder Wallet Ledger (#199967) & Atomic Idempotent Diamond-to-Coin Exchange
        val initialVerify = repository.verifyAndRetrieveUserWalletHistory("199967")
        assertTrue(initialVerify is BackendResult.Success)
        val initialReport = (initialVerify as BackendResult.Success).data
        assertEquals(85_000L, initialReport.latestCoinsBalance)
        assertEquals(42_500L, initialReport.latestDiamondsBalance)
        assertTrue(initialReport.ledgerConsistencyVerified)

        val idemKey = "TEST_EXCHANGE_KEY_001"
        val exchangeRes = repository.exchangeDiamondsToCoinsAtomic(
            idempotencyKey = idemKey,
            userId = "199967",
            diamondsToExchange = 1_000L
        )
        assertTrue(exchangeRes is BackendResult.Success)

        // Duplicate replay with same idempotencyKey must be rejected by server/DB validation
        val replayRes = repository.exchangeDiamondsToCoinsAtomic(
            idempotencyKey = idemKey,
            userId = "199967",
            diamondsToExchange = 1_000L
        )
        assertTrue(replayRes is BackendResult.Error)

        // 6. Test Atomic Global Lucky Gift Delivery, Cashback, EXP & Realtime WebSocket/Push Dispatch
        realtimeService.connectProductionWebSocket("SESSION_199967")
        TangerPushNotificationDispatcher.ensureNotificationChannels(context)

        val giftRes = repository.sendGiftAtomicServerValidated(
            idempotencyKey = "TEST_LUCKY_GIFT_001",
            senderUserId = "199967",
            receiverUserId = "199968",
            roomId = "GLOBAL",
            giftId = "GFT-105",
            quantity = 5
        )
        assertTrue(giftRes is BackendResult.Success)
        val giftTx = (giftRes as BackendResult.Success).data
        assertEquals("199967", giftTx.senderUserId)
        assertEquals("199968", giftTx.receiverUserId)
        assertTrue(giftTx.totalCoinsSpent > 0L)
        assertTrue(giftTx.luckyRewardCoins > 0L)

        // 7. Test Social Posts, 24h Stories, Reels, Live PK Streams & Cloud Media CDN Registration
        val postRes = repository.createSocialPost(
            authorUserId = "199967",
            caption = "Welcome to TANGERAM Production APK!",
            mediaUri = "https://cdn.tangerstarvoice.global/media/post_test.jpg",
            mediaType = "IMAGE",
            locationTag = "Tangier, Morocco"
        )
        assertTrue(postRes is BackendResult.Success)

        val storyRes = repository.createStory24h(
            authorUserId = "199967",
            captionText = "24h Global Story Test",
            mediaUri = "",
            backgroundTheme = "EMERALD_GLOW"
        )
        assertTrue(storyRes is BackendResult.Success)

        val reelRes = repository.createReel(
            creatorUserId = "199967",
            title = "Live Vocal Reel",
            audioTrackName = "Tanger Star Original Audio",
            mediaUri = "https://cdn.tangerstarvoice.global/media/reel_test.mp4",
            durationSec = 15
        )
        assertTrue(reelRes is BackendResult.Success)

        val liveRes = repository.startOrJoinLiveStream(
            hostUserId = "199967",
            streamTitle = "Global Championship PK",
            category = "PK_BATTLE",
            isPkBattle = true,
            pkOpponentUserId = "199968"
        )
        assertTrue(liveRes is BackendResult.Success)

        // 8. Test Server-Side RBAC Protection & Atomic Production Database Backup Snapshot
        // Non-owner account (#199972) must be denied Owner Admin actions
        val unauthorizedBackup = repository.ownerCreateDatabaseBackupSnapshot("199972")
        assertTrue(unauthorizedBackup is BackendResult.Error)

        // Authorized Global Owner (#199967) succeeds and creates verified backup snapshot
        val authorizedBackup = repository.ownerCreateDatabaseBackupSnapshot("199967")
        assertTrue(authorizedBackup is BackendResult.Success)
        val backups = repository.databaseBackupsFlow.first()
        assertTrue(backups.isNotEmpty())
        assertEquals(5, backups.first().schemaVersion)

        // 9. Verify Official Google AdMob Configuration, Build-Type Ad Unit Switching & Placement Guardrails
        assertEquals("ca-app-pub-9420979892759161~4631920628", context.getString(R.string.admob_app_id))
        assertEquals("ca-app-pub-9420979892759161~4631920628", TangerAdMobManager.configuredAppId)
        assertEquals("ca-app-pub-9420979892759161/2005757280", TangerAdMobManager.resolveActiveAdUnitId(isDebugBuild = false))
        assertEquals("ca-app-pub-3940256099942544/6300978111", TangerAdMobManager.resolveActiveAdUnitId(isDebugBuild = true))

        // Allowed only in Home/Discover/Social Posts feeds; blocked on Login, OTP, Payments, Wallet, and Voice/Call controls
        assertTrue(TangerAdMobManager.isPlacementAllowed("HOME_FEED"))
        assertTrue(TangerAdMobManager.isPlacementAllowed("DISCOVER_FEED"))
        assertTrue(TangerAdMobManager.isPlacementAllowed("SOCIAL_POSTS_FEED"))
        assertFalse(TangerAdMobManager.isPlacementAllowed("LOGIN"))
        assertFalse(TangerAdMobManager.isPlacementAllowed("OTP"))
        assertFalse(TangerAdMobManager.isPlacementAllowed("PAYMENTS"))
        assertFalse(TangerAdMobManager.isPlacementAllowed("WALLET"))
        assertFalse(TangerAdMobManager.isPlacementAllowed("VOICE_ROOM"))
        assertFalse(TangerAdMobManager.isPlacementAllowed("CALL_CONTROLS"))
    }
}
