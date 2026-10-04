package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainNavTab
import com.example.ui.TangerViewModel
import com.example.ui.ads.TangerAdMobManager
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TangerAdMobManager.initializeMobileAdsSdk(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TangerStarVoiceApp()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TangerStarVoiceApp(
    viewModel: TangerViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allRooms by viewModel.allRooms.collectAsStateWithLifecycle()
    val activeRoom by viewModel.activeRoom.collectAsStateWithLifecycle()
    val activeRoomSeats by viewModel.activeRoomSeats.collectAsStateWithLifecycle()
    val activeRoomRoles by viewModel.activeRoomRoles.collectAsStateWithLifecycle()
    val activeRoomTopBanners by viewModel.activeRoomTopNotifications.collectAsStateWithLifecycle()
    val activeRoomMessages by viewModel.activeRoomMessages.collectAsStateWithLifecycle()
    val globalMessages by viewModel.globalMessages.collectAsStateWithLifecycle()
    val callSessions by viewModel.callSessions.collectAsStateWithLifecycle()
    val allGifts by viewModel.allGifts.collectAsStateWithLifecycle()
    val luckyState by viewModel.luckyGiftState.collectAsStateWithLifecycle()
    val luckyAudits by viewModel.luckyGiftAudits.collectAsStateWithLifecycle()
    val vipConfigs by viewModel.vipConfigs.collectAsStateWithLifecycle()
    val vipHistory by viewModel.currentUserVipHistory.collectAsStateWithLifecycle()
    val userTransactions by viewModel.currentUserTransactions.collectAsStateWithLifecycle()
    val diamondCycles by viewModel.currentUserDiamondCycles.collectAsStateWithLifecycle()
    val allAgencies by viewModel.allAgencies.collectAsStateWithLifecycle()
    val allAgencyMembers by viewModel.allAgencyMembers.collectAsStateWithLifecycle()
    val userInvitations by viewModel.currentUserInvitations.collectAsStateWithLifecycle()
    val followingList by viewModel.currentUserFollowing.collectAsStateWithLifecycle()
    val gameMatches by viewModel.gameMatches.collectAsStateWithLifecycle()
    val globalEvents by viewModel.globalEvents.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val userReports by viewModel.userReports.collectAsStateWithLifecycle()
    val socialPosts by viewModel.socialPosts.collectAsStateWithLifecycle()
    val postComments by viewModel.allPostComments.collectAsStateWithLifecycle()
    val activeStories by viewModel.activeStories.collectAsStateWithLifecycle()
    val reels by viewModel.reels.collectAsStateWithLifecycle()
    val liveStreams by viewModel.liveStreams.collectAsStateWithLifecycle()
    val cloudMediaAssets by viewModel.cloudMediaAssets.collectAsStateWithLifecycle()
    val databaseBackups by viewModel.databaseBackups.collectAsStateWithLifecycle()

    val nowMillis by viewModel.nowMillis.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val lastIssuedOtp by viewModel.lastIssuedOtpCode.collectAsStateWithLifecycle()
    val otpCooldownSec by viewModel.otpCooldownRemainingSec.collectAsStateWithLifecycle()

    val isMicMuted by viewModel.isLocalMicMuted.collectAsStateWithLifecycle()
    val hasAudioPermission by viewModel.hasAudioPermission.collectAsStateWithLifecycle()
    val micAmplitudeDb by viewModel.realtimeService.localMicAmplitudeDb.collectAsStateWithLifecycle()
    val connectionState by viewModel.realtimeService.connectionState.collectAsStateWithLifecycle()
    val pingMs by viewModel.realtimeService.pingMs.collectAsStateWithLifecycle()
    val recentGiftOverlay by viewModel.recentGiftOverlay.collectAsStateWithLifecycle()

    val showAdminAuthDialog by viewModel.showAdminAuthDialog.collectAsStateWithLifecycle()
    val showOwnerAdminScreen by viewModel.showOwnerAdminScreen.collectAsStateWithLifecycle()
    val isSignedUpAndAuthenticated by viewModel.isSignedUpAndAuthenticated.collectAsStateWithLifecycle()
    val showSignupScreen by viewModel.showSignupScreen.collectAsStateWithLifecycle()
    val showOnboardingScreen by viewModel.showOnboardingScreen.collectAsStateWithLifecycle()
    val userSequenceCounter by viewModel.userSequenceCounter.collectAsStateWithLifecycle()

    // Strict Entry Gate: User must complete Step 1 (Sign Up) and Step 2 (Onboarding) before entering the app
    val isCompletedAndInsideApp = isSignedUpAndAuthenticated &&
        currentUser != null &&
        currentUser?.isOnboardingCompleted == true &&
        !showSignupScreen &&
        !showOnboardingScreen

    var showGlobalGiftCatalogFromTopBar by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = TangerEmeraldDark,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Brand Logo (Double-tap is ONLY entry trigger for Admin RBAC modal, never authentication)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = { viewModel.selectTab(MainNavTab.HOME) },
                                    onDoubleClick = { viewModel.onLogoDoubleTapEntryTrigger() }
                                )
                                .testTag("tanger_star_brand_logo")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(TangerGoldGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "Tanger Star Logo (Double-tap for Owner RBAC Gate)",
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "TANGER STAR VOICE",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (!isSignedUpAndAuthenticated || currentUser == null) {
                                        "Step 1 of 2: Complete Sign Up to Enter App"
                                    } else if (currentUser?.isOnboardingCompleted == false || showOnboardingScreen) {
                                        "Step 2 of 2: Complete Onboarding (ID #${currentUser?.tangerUserId})"
                                    } else {
                                        "TANGERAM ID: #${currentUser?.tangerUserId} • VIP ${currentUser?.vipLevel ?: 1}"
                                    },
                                    color = TangerMint,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isCompletedAndInsideApp) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.clickable { viewModel.selectTab(MainNavTab.WALLET) }
                                ) {
                                    Text(
                                        text = "🪙 ${currentUser?.coinsBalance ?: 0}",
                                        color = TangerGoldLight,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.clickable { viewModel.selectTab(MainNavTab.WALLET) }
                                ) {
                                    Text(
                                        text = "💎 ${currentUser?.diamondsBalance ?: 0}",
                                        color = TangerDiamondBlue,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    color = TangerGold,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("top_bar_signup_gate_badge")
                                ) {
                                    Text(
                                        text = if (!isSignedUpAndAuthenticated || currentUser == null) "🔒 Step 1: Sign Up Required" else "🔒 Step 2: Onboarding",
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            if (isCompletedAndInsideApp) {
                                Surface(
                                    color = TangerTurquoiseBright,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .clickable { showGlobalGiftCatalogFromTopBar = true }
                                        .testTag("top_bar_global_gift_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = "🎁 Gift",
                                            color = Color.Black,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Surface(
                                    color = TangerGold,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .clickable { viewModel.openSignupFlow() }
                                        .testTag("top_bar_signup_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PersonAdd,
                                            contentDescription = "Sign Up Page",
                                            tint = Color.Black,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Sign Up",
                                            color = Color.Black,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { viewModel.onLogoDoubleTapEntryTrigger() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("admin_rbac_trigger_button")
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = "Owner Admin RBAC Gate",
                                    tint = TangerGoldLight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Status feedback toast banner
                    AnimatedVisibility(visible = statusBanner != null) {
                        statusBanner?.let { msg ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .clickable { viewModel.postStatus(null) },
                                color = TangerTurquoiseBright,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("✕", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Only show the main app navigation bar AFTER the user completes Step 1 Sign Up & Step 2 Onboarding
            if (!showOwnerAdminScreen && isCompletedAndInsideApp) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = TangerEmeraldDark,
                    shadowElevation = 12.dp
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(MainNavTab.entries) { tab ->
                            val isSelected = selectedTab == tab
                            val icon: ImageVector = when (tab) {
                                MainNavTab.SIGNUP -> Icons.Default.PersonAdd
                                MainNavTab.HOME -> Icons.Default.Home
                                MainNavTab.SOCIAL_LIVE -> Icons.Default.VideoLibrary
                                MainNavTab.DISCOVER -> Icons.Default.Explore
                                MainNavTab.ROOMS -> Icons.Default.Mic
                                MainNavTab.MESSAGES -> Icons.Default.Chat
                                MainNavTab.GAMES -> Icons.Default.Casino
                                MainNavTab.WALLET -> Icons.Default.AccountBalanceWallet
                                MainNavTab.EVENTS -> Icons.Default.EmojiEvents
                                MainNavTab.PROFILE -> Icons.Default.Person
                            }

                            Surface(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { viewModel.selectTab(tab) }
                                    .testTag("nav_tab_${tab.name.lowercase()}"),
                                color = if (isSelected) TangerTurquoiseBright else Color.White.copy(alpha = 0.08f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) Color.Black else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = tab.title,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                showOwnerAdminScreen -> {
                    OwnerAdminPanelScreen(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        allUsers = allUsers,
                        allGifts = allGifts,
                        luckyState = luckyState,
                        allRooms = allRooms,
                        allAgencies = allAgencies,
                        allEvents = globalEvents,
                        allReports = userReports,
                        auditLogs = auditLogs,
                        databaseBackups = databaseBackups,
                        cloudMediaAssets = cloudMediaAssets
                    )
                }
                !isSignedUpAndAuthenticated || currentUser == null || showSignupScreen -> {
                    TangeramSignupScreen(
                        viewModel = viewModel,
                        nextSequentialId = userSequenceCounter?.nextAvailableId ?: 199972L,
                        lastIssuedOtp = lastIssuedOtp,
                        otpCooldownSec = otpCooldownSec,
                        allUsers = allUsers,
                        isAlreadySignedIn = isSignedUpAndAuthenticated && currentUser != null
                    )
                }
                showOnboardingScreen || currentUser?.isOnboardingCompleted == false -> {
                    TangeramOnboardingScreen(
                        viewModel = viewModel,
                        currentUser = currentUser
                    )
                }
                else -> {
                    when (selectedTab) {
                    MainNavTab.SIGNUP -> TangeramSignupScreen(
                        viewModel = viewModel,
                        nextSequentialId = userSequenceCounter?.nextAvailableId ?: 199972L,
                        lastIssuedOtp = lastIssuedOtp,
                        otpCooldownSec = otpCooldownSec,
                        allUsers = allUsers,
                        isAlreadySignedIn = true
                    )
                    MainNavTab.HOME -> HomeAndDiscoverScreen(
                        isDiscoverMode = false,
                        viewModel = viewModel,
                        currentUser = currentUser,
                        allUsers = allUsers,
                        allRooms = allRooms,
                        allAgencies = allAgencies,
                        globalEvents = globalEvents,
                        luckyState = luckyState,
                        followingList = followingList,
                        allGifts = allGifts
                    )
                    MainNavTab.SOCIAL_LIVE -> SocialReelsAndLiveScreen(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        allUsers = allUsers,
                        posts = socialPosts,
                        comments = postComments,
                        stories = activeStories,
                        reels = reels,
                        liveStreams = liveStreams,
                        cloudMediaAssets = cloudMediaAssets,
                        allGifts = allGifts,
                        luckyState = luckyState
                    )
                    MainNavTab.DISCOVER -> HomeAndDiscoverScreen(
                        isDiscoverMode = true,
                        viewModel = viewModel,
                        currentUser = currentUser,
                        allUsers = allUsers,
                        allRooms = allRooms,
                        allAgencies = allAgencies,
                        globalEvents = globalEvents,
                        luckyState = luckyState,
                        followingList = followingList,
                        allGifts = allGifts
                    )
                    MainNavTab.ROOMS -> VoiceRoomScreen(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        allRooms = allRooms,
                        activeRoom = activeRoom,
                        seats = activeRoomSeats,
                        roles = activeRoomRoles,
                        activeTopBanners = activeRoomTopBanners,
                        roomMessages = activeRoomMessages,
                        allGifts = allGifts,
                        luckyState = luckyState,
                        allUsers = allUsers,
                        nowMillis = nowMillis,
                        isMicMuted = isMicMuted,
                        hasAudioPermission = hasAudioPermission,
                        micAmplitudeDb = micAmplitudeDb,
                        connectionState = connectionState,
                        pingMs = pingMs,
                        recentGiftOverlay = recentGiftOverlay
                    )
                    MainNavTab.MESSAGES -> MessagesAndCallsScreen(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        allUsers = allUsers,
                        globalMessages = globalMessages,
                        callSessions = callSessions,
                        allGifts = allGifts,
                        luckyState = luckyState
                    )
                    MainNavTab.GAMES -> MultiplayerGamesScreen(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        allUsers = allUsers,
                        matches = gameMatches
                    )
                    MainNavTab.WALLET -> WalletAndDiamondsScreen(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        transactions = userTransactions,
                        diamondCycles = diamondCycles,
                        vipConfigs = vipConfigs,
                        vipHistory = vipHistory,
                        luckyState = luckyState,
                        luckyAudits = luckyAudits
                    )
                    MainNavTab.EVENTS -> EventsAndAgenciesScreen(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        events = globalEvents,
                        agencies = allAgencies,
                        agencyMembers = allAgencyMembers,
                        invitations = userInvitations
                    )
                    MainNavTab.PROFILE -> ProfileAndAuthScreen(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        allUsers = allUsers,
                        lastIssuedOtp = lastIssuedOtp,
                        otpCooldownSec = otpCooldownSec
                    )
                    }
                }
            }

            // Real-Time Global Lucky Gift Animation & Notification Overlay across all screens
            GlobalLuckyGiftRealTimeBannerOverlay(
                giftOverlay = recentGiftOverlay,
                onDismiss = { viewModel.dismissRecentGiftOverlay() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    if (showGlobalGiftCatalogFromTopBar) {
        GlobalGiftCatalogDialog(
            currentUser = currentUser,
            allUsers = allUsers,
            allGifts = allGifts,
            luckyState = luckyState,
            initialReceiverUserId = allUsers.firstOrNull { it.tangerUserId != currentUser?.tangerUserId }?.tangerUserId,
            contextRoomId = "GLOBAL",
            onSendGift = { receiverId, giftId, qty ->
                viewModel.sendGlobalGiftToUser(receiverId, giftId, qty, "GLOBAL")
            },
            onDismiss = { showGlobalGiftCatalogFromTopBar = false }
        )
    }

    // RBAC Authentication Challenge Dialog (Double-tap logo is ONLY entry trigger, never authentication)
    if (showAdminAuthDialog) {
        var rbacSecretInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissAdminAuthDialog() },
            title = { Text("🛡️ Global Owner RBAC Verification", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Security Rule: The double-tap logo is ONLY an entry trigger, never authentication. Backend RBAC verifies your GLOBAL_OWNER role (#199967) or your OWNER_MASTER_RBAC_SECRET configured via BuildConfig Secrets before unlocking the Global Owner Admin Panel.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = rbacSecretInput,
                        onValueChange = { rbacSecretInput = it },
                        label = { Text("Owner Master RBAC Key (from BuildConfig Secrets)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_rbac_secret_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.verifyOwnerAdminRbac(rbacSecretInput) },
                    modifier = Modifier.testTag("verify_admin_rbac_button")
                ) {
                    Text("Verify Server RBAC")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAdminAuthDialog() }) { Text("Cancel") }
            }
        )
    }
}
