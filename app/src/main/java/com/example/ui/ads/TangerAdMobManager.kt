package com.example.ui.ads

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.BuildConfig
import com.example.ui.theme.TangerEmerald
import com.example.ui.theme.TangerEmeraldDark
import com.example.ui.theme.TangerGold
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Official Google AdMob Manager for TANGER STAR VOICE (TANGERAM).
 *
 * Strict Policy & Placement Rules:
 * 1. Official AdMob App ID: ca-app-pub-9420979892759161~4631920628
 * 2. Official Production Ad Unit ID: ca-app-pub-9420979892759161/2005757280
 * 3. Uses Google's official test Ad Unit ID (ca-app-pub-3940256099942544/6300978111) during Debug/Development
 *    and switches automatically to the Production Ad Unit ID (ca-app-pub-9420979892759161/2005757280) in Release builds.
 * 4. Displayed ONLY in safe feed/discovery areas (Home, Discover, Social Posts Feed).
 *    NEVER displayed on Login/Signup, OTP, Payments, Wallet Transactions, or Critical Voice/Call controls.
 * 5. Zero Coin/Diamond rewards are granted from Banner/Feed ads.
 */
object TangerAdMobManager {
    const val OFFICIAL_ADMOB_APP_ID = "ca-app-pub-9420979892759161~4631920628"
    const val OFFICIAL_PRODUCTION_AD_UNIT_ID = "ca-app-pub-9420979892759161/2005757280"
    const val GOOGLE_OFFICIAL_TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    private val isInitialized = AtomicBoolean(false)

    val configuredAppId: String
        get() = BuildConfig.ADMOB_APP_ID.ifBlank { OFFICIAL_ADMOB_APP_ID }

    val configuredProductionAdUnitId: String
        get() = BuildConfig.ADMOB_PRODUCTION_AD_UNIT_ID.ifBlank { OFFICIAL_PRODUCTION_AD_UNIT_ID }

    val configuredDebugTestAdUnitId: String
        get() = BuildConfig.ADMOB_DEBUG_TEST_AD_UNIT_ID.ifBlank { GOOGLE_OFFICIAL_TEST_BANNER_AD_UNIT_ID }

    /**
     * Resolves the active AdMob Ad Unit ID based on build type:
     * - Debug / Development (`isDebugBuild = true`): Google Official Test Ad Unit ID
     * - Release / Production (`isDebugBuild = false`): Official Production Ad Unit ID (`ca-app-pub-9420979892759161/2005757280`)
     */
    fun resolveActiveAdUnitId(isDebugBuild: Boolean = BuildConfig.DEBUG): String {
        return if (isDebugBuild) {
            configuredDebugTestAdUnitId
        } else {
            configuredProductionAdUnitId
        }
    }

    /**
     * Enforces placement safety so ads NEVER render on Login, OTP, Payments, Wallet, or Voice/Call controls.
     */
    fun isPlacementAllowed(placementZone: String): Boolean {
        val normalized = placementZone.trim().uppercase()
        val forbiddenZones = setOf(
            "LOGIN", "SIGNUP", "ONBOARDING", "OTP", "PAYMENTS",
            "WALLET", "TRANSACTIONS", "VOICE_ROOM", "CALL_CONTROLS", "ADMIN_RBAC"
        )
        if (normalized in forbiddenZones) return false
        return normalized in setOf("HOME_FEED", "DISCOVER_FEED", "SOCIAL_POSTS_FEED")
    }

    fun initializeMobileAdsSdk(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                MobileAds.initialize(context.applicationContext) {}
            } catch (_: Throwable) {
                // Safe guard for JVM unit test environments without Play Services binary
            }
        }
    }
}

@Composable
fun TangerAdMobFeedBannerCard(
    placementZone: String,
    modifier: Modifier = Modifier
) {
    // Strictly block rendering if placement is not an approved feed zone
    if (!TangerAdMobManager.isPlacementAllowed(placementZone)) {
        return
    }

    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val activeAdUnitId = remember { TangerAdMobManager.resolveActiveAdUnitId(BuildConfig.DEBUG) }
    val buildModeLabel = if (BuildConfig.DEBUG) "DEV TEST AD MODE" else "RELEASE PRODUCTION AD"
    var adLoadStatus by remember { mutableStateOf("LOADING") }

    DisposableEffect(context) {
        TangerAdMobManager.initializeMobileAdsSdk(context)
        onDispose { }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tanger_admob_banner_${placementZone.lowercase()}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, TangerGold.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Sponsored Feed Placement",
                        tint = TangerEmerald,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Sponsored • Google AdMob ($buildModeLabel)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TangerEmeraldDark
                    )
                }
                Surface(
                    color = TangerEmerald.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = TangerEmerald,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Non-Rewarded Feed Ad",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TangerEmeraldDark
                        )
                    }
                }
            }

            if (!isPreview) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        factory = { ctx ->
                            AdView(ctx).apply {
                                setAdSize(AdSize.BANNER)
                                adUnitId = activeAdUnitId
                                adListener = object : AdListener() {
                                    override fun onAdLoaded() {
                                        adLoadStatus = "LOADED"
                                    }

                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                        adLoadStatus = "READY (${error.code})"
                                    }
                                }
                                try {
                                    loadAd(AdRequest.Builder().build())
                                } catch (_: Throwable) {
                                    adLoadStatus = "READY"
                                }
                            }
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App ID: ${TangerAdMobManager.OFFICIAL_ADMOB_APP_ID.take(22)}… • Unit: ${activeAdUnitId.takeLast(14)}",
                    fontSize = 9.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Status: $adLoadStatus",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TangerEmerald
                )
            }
        }
    }
}
