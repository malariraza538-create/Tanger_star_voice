package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.realtime.RealtimeEvent
import com.example.data.realtime.TangerPushNotificationDispatcher
import com.example.data.realtime.TangerRealtimeVoiceService
import com.example.data.repository.BackendResult
import com.example.data.repository.CountryOption
import com.example.data.repository.GLOBAL_COUNTRIES
import com.example.data.repository.TangerRepository
import com.example.data.repository.WalletVerificationReport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

enum class MainNavTab(val title: String) {
    SIGNUP("Sign Up"),
    HOME("Home"),
    SOCIAL_LIVE("Posts/Reels/Live"),
    DISCOVER("Discover"),
    ROOMS("Rooms"),
    MESSAGES("Messages"),
    GAMES("Games"),
    WALLET("Wallet"),
    EVENTS("Events"),
    PROFILE("Profile")
}

@OptIn(ExperimentalCoroutinesApi::class)
class TangerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = TangerDatabase.getInstance(application)
    val realtimeService = TangerRealtimeVoiceService(viewModelScope)
    val repository = TangerRepository(db, db.tangerDao(), realtimeService)

    private val _selectedTab = MutableStateFlow(MainNavTab.HOME)
    val selectedTab: StateFlow<MainNavTab> = _selectedTab.asStateFlow()

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _isSignedUpAndAuthenticated = MutableStateFlow(false)
    val isSignedUpAndAuthenticated: StateFlow<Boolean> = _isSignedUpAndAuthenticated.asStateFlow()

    private val _showSignupScreen = MutableStateFlow(true)
    val showSignupScreen: StateFlow<Boolean> = _showSignupScreen.asStateFlow()

    private val _showOnboardingScreen = MutableStateFlow(false)
    val showOnboardingScreen: StateFlow<Boolean> = _showOnboardingScreen.asStateFlow()

    private val _activeRoomId = MutableStateFlow("RM-101")
    val activeRoomId: StateFlow<String> = _activeRoomId.asStateFlow()

    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    val nowMillis: StateFlow<Long> = _nowMillis.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _lastIssuedOtpCode = MutableStateFlow<String?>(null)
    val lastIssuedOtpCode: StateFlow<String?> = _lastIssuedOtpCode.asStateFlow()

    private val _otpCooldownRemainingSec = MutableStateFlow(0)
    val otpCooldownRemainingSec: StateFlow<Int> = _otpCooldownRemainingSec.asStateFlow()

    private val _isLocalMicMuted = MutableStateFlow(false)
    val isLocalMicMuted: StateFlow<Boolean> = _isLocalMicMuted.asStateFlow()

    private val _hasAudioPermission = MutableStateFlow(false)
    val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()

    // Admin Panel RBAC states (Double-tap logo is ONLY entry trigger, never authentication)
    private val _showAdminAuthDialog = MutableStateFlow(false)
    val showAdminAuthDialog: StateFlow<Boolean> = _showAdminAuthDialog.asStateFlow()

    private val _isOwnerAdminAuthenticated = MutableStateFlow(false)
    val isOwnerAdminAuthenticated: StateFlow<Boolean> = _isOwnerAdminAuthenticated.asStateFlow()

    private val _showOwnerAdminScreen = MutableStateFlow(false)
    val showOwnerAdminScreen: StateFlow<Boolean> = _showOwnerAdminScreen.asStateFlow()

    private val _recentGiftOverlay = MutableStateFlow<RealtimeEvent.GiftBroadcast?>(null)
    val recentGiftOverlay: StateFlow<RealtimeEvent.GiftBroadcast?> = _recentGiftOverlay.asStateFlow()

    // Reactive Database Streams
    val currentUser: StateFlow<UserEntity?> = _currentUserId
        .flatMapLatest { id -> repository.observeUser(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userSequenceCounter: StateFlow<UserSequenceCounterEntity?> = repository.userSequenceCounterFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRooms: StateFlow<List<VoiceRoomEntity>> = repository.allRoomsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRoom: StateFlow<VoiceRoomEntity?> = _activeRoomId
        .flatMapLatest { id -> repository.observeRoom(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeRoomSeats: StateFlow<List<RoomSeatEntity>> = _activeRoomId
        .flatMapLatest { id -> repository.observeRoomSeats(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRoomRoles: StateFlow<List<RoomRoleEntity>> = _activeRoomId
        .flatMapLatest { id -> repository.observeRoomRoles(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Room Top Notifications filtered by real-time expiry clock
    val activeRoomTopNotifications: StateFlow<List<RoomTopNotificationEntity>> =
        combine(_activeRoomId, _nowMillis) { roomId, now -> roomId to now }
            .flatMapLatest { (roomId, now) -> repository.observeActiveRoomBanners(roomId, now) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRoomNotifications: StateFlow<List<RoomTopNotificationEntity>> =
        repository.allRoomNotificationsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGifts: StateFlow<List<GiftEntity>> = repository.allGiftsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val giftTransactions: StateFlow<List<GiftTransactionEntity>> = repository.giftTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val luckyGiftState: StateFlow<LuckyGiftStateEntity?> = repository.luckyGiftStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val luckyGiftAudits: StateFlow<List<LuckyGiftAuditEntity>> = repository.luckyGiftAuditsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vipConfigs: StateFlow<List<VipConfigEntity>> = repository.vipConfigsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserVipHistory: StateFlow<List<VipHistoryEntity>> = _currentUserId
        .flatMapLatest { id -> repository.observeVipHistory(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserTransactions: StateFlow<List<WalletTransactionEntity>> = _currentUserId
        .flatMapLatest { id -> repository.observeUserTransactions(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _walletTxTypeFilter = MutableStateFlow("ALL")
    val walletTxTypeFilter: StateFlow<String> = _walletTxTypeFilter.asStateFlow()

    private val _walletSearchQuery = MutableStateFlow("")
    val walletSearchQuery: StateFlow<String> = _walletSearchQuery.asStateFlow()

    private val _walletVerificationReport = MutableStateFlow<WalletVerificationReport?>(null)
    val walletVerificationReport: StateFlow<WalletVerificationReport?> = _walletVerificationReport.asStateFlow()

    val filteredUserTransactions: StateFlow<List<WalletTransactionEntity>> =
        combine(_currentUserId, _walletTxTypeFilter, _walletSearchQuery) { uid, typeFilter, query ->
            Triple(uid, typeFilter, query)
        }.flatMapLatest { (uid, typeFilter, query) ->
            repository.observeFilteredUserTransactions(uid, typeFilter, query)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<WalletTransactionEntity>> = repository.allTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserDiamondCycles: StateFlow<List<DiamondMonthlyCycleEntity>> = _currentUserId
        .flatMapLatest { id -> repository.observeDiamondCycles(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAgencies: StateFlow<List<AgencyEntity>> = repository.allAgenciesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAgencyMembers: StateFlow<List<AgencyMemberEntity>> = repository.allAgencyMembersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserInvitations: StateFlow<List<AgencyInvitationEntity>> = _currentUserId
        .flatMapLatest { id -> repository.observeInvitationsForUser(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserFollowing: StateFlow<List<FollowEntity>> = _currentUserId
        .flatMapLatest { id -> repository.observeFollowing(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRoomMessages: StateFlow<List<ChatMessageEntity>> = _activeRoomId
        .flatMapLatest { roomId -> repository.observeMessages("ROOM", roomId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val globalMessages: StateFlow<List<ChatMessageEntity>> =
        repository.observeMessages("GLOBAL", "GLOBAL")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callSessions: StateFlow<List<CallSessionEntity>> = repository.callSessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gameMatches: StateFlow<List<GameMatchEntity>> = repository.gameMatchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val globalEvents: StateFlow<List<GlobalEventEntity>> = repository.activeGlobalEventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rankingRewardClaims: StateFlow<List<RankingRewardClaimEntity>> = repository.rankingRewardClaimsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userReports: StateFlow<List<UserReportEntity>> = repository.allReportsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val socialPosts: StateFlow<List<SocialPostEntity>> = repository.socialPostsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPostComments: StateFlow<List<PostCommentEntity>> = repository.allPostCommentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeStories: StateFlow<List<StoryEntity>> = _nowMillis
        .flatMapLatest { now -> repository.observeActiveStories(now) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reels: StateFlow<List<ReelEntity>> = repository.reelsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveStreams: StateFlow<List<LiveStreamEntity>> = repository.liveStreamsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudMediaAssets: StateFlow<List<CloudMediaAssetEntity>> = repository.cloudMediaAssetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val databaseBackups: StateFlow<List<DatabaseBackupSnapshotEntity>> = repository.databaseBackupsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val activeUserId = repository.initializeDatabaseIfNeeded()
            realtimeService.connectProductionWebSocket(activeUserId)
            TangerPushNotificationDispatcher.ensureNotificationChannels(application.applicationContext)
            if (!activeUserId.isNullOrBlank()) {
                _currentUserId.value = activeUserId
                _isSignedUpAndAuthenticated.value = true
                _showSignupScreen.value = false
                when (val verifyRes = repository.verifyAndRetrieveUserWalletHistory(activeUserId)) {
                    is BackendResult.Success -> _walletVerificationReport.value = verifyRes.data
                    is BackendResult.Error -> {}
                }
            } else {
                // Mandatory gate: user must complete Step 1 Sign Up & Step 2 Onboarding before entering the app
                _currentUserId.value = ""
                _isSignedUpAndAuthenticated.value = false
                _showSignupScreen.value = true
                _showOnboardingScreen.value = false
            }
        }

        // Real-time ticker that sweeps expired Room Top Notifications every 2 seconds
        viewModelScope.launch {
            while (isActive) {
                delay(2000L)
                _nowMillis.value = System.currentTimeMillis()
                repository.sweepExpiredRoomNotifications()
                if (_otpCooldownRemainingSec.value > 0) {
                    _otpCooldownRemainingSec.value = (_otpCooldownRemainingSec.value - 2).coerceAtLeast(0)
                }
            }
        }

        // Listen for real-time gift animations and dispatch Android system push notifications
        viewModelScope.launch {
            realtimeService.events.collect { event ->
                if (event is RealtimeEvent.GiftBroadcast) {
                    _recentGiftOverlay.value = event
                    TangerPushNotificationDispatcher.dispatchGiftOrLuckyNotification(
                        application.applicationContext,
                        event
                    )
                    delay(3500L)
                    if (_recentGiftOverlay.value == event) {
                        _recentGiftOverlay.value = null
                    }
                }
            }
        }
    }

    fun selectTab(tab: MainNavTab) {
        val user = currentUser.value
        if (!_isSignedUpAndAuthenticated.value || _currentUserId.value.isBlank()) {
            _showSignupScreen.value = true
            postStatus("Step 1 Required: Please complete Sign Up first to enter TANGER STAR VOICE.")
            return
        }
        if (user != null && !user.isOnboardingCompleted) {
            _showOnboardingScreen.value = true
            postStatus("Step 2 Required: Please complete your Profile Onboarding to enter the app.")
            return
        }
        _selectedTab.value = tab
        _showSignupScreen.value = (tab == MainNavTab.SIGNUP)
    }

    fun selectAndEnterRoom(roomId: String) {
        if (!_isSignedUpAndAuthenticated.value || _currentUserId.value.isBlank()) {
            _showSignupScreen.value = true
            postStatus("Please complete Sign Up first before entering a Voice Room.")
            return
        }
        _activeRoomId.value = roomId
        _showSignupScreen.value = false
        _selectedTab.value = MainNavTab.ROOMS
        postStatus("Joined Room $roomId • Synced active top banner & seats")
    }

    fun postStatus(msg: String?) {
        _statusBannerMessage.value = msg
    }

    // ================= TANGERAM SIGNUP, ONBOARDING, AUTH & PROFILE ACTIONS =================
    fun openSignupFlow() {
        _showSignupScreen.value = true
    }

    fun closeSignupFlow() {
        if (!_isSignedUpAndAuthenticated.value || _currentUserId.value.isBlank()) {
            postStatus("Sign Up Required: Complete Step 1 (Sign Up) & Step 2 (Onboarding) to enter the app.")
            return
        }
        val user = currentUser.value
        if (user != null && !user.isOnboardingCompleted) {
            _showSignupScreen.value = false
            _showOnboardingScreen.value = true
            return
        }
        _showSignupScreen.value = false
        if (_selectedTab.value == MainNavTab.SIGNUP) {
            _selectedTab.value = MainNavTab.HOME
        }
    }

    fun signOutAndReturnToSignup() {
        viewModelScope.launch {
            repository.signOutCurrentSession()
            _currentUserId.value = ""
            _isSignedUpAndAuthenticated.value = false
            _showOnboardingScreen.value = false
            _showSignupScreen.value = true
            _selectedTab.value = MainNavTab.SIGNUP
            postStatus("Signed out. Complete Sign Up or select an account to enter the app.")
        }
    }

    fun openOnboardingFlow() {
        _showOnboardingScreen.value = true
    }

    fun requestOtp(country: CountryOption, phone: String) {
        viewModelScope.launch {
            when (val res = repository.requestPhoneOtp(country, phone)) {
                is BackendResult.Success -> {
                    _lastIssuedOtpCode.value = res.data.otpCode
                    _otpCooldownRemainingSec.value = 30
                    postStatus("${res.message} • SMS Verification Code: ${res.data.otpCode}")
                }
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun verifyOtpAndSignIn(
        country: CountryOption,
        phone: String,
        otp: String,
        username: String = "",
        gender: String = "",
        dob: String = "",
        bio: String = ""
    ) {
        viewModelScope.launch {
            when (val res = repository.verifyOtpAndEstablishSession(country, phone, otp, username, gender, dob, bio)) {
                is BackendResult.Success -> {
                    _currentUserId.value = res.data.tangerUserId
                    _isSignedUpAndAuthenticated.value = true
                    _lastIssuedOtpCode.value = null
                    _showSignupScreen.value = false
                    if (!res.data.isOnboardingCompleted) {
                        _showOnboardingScreen.value = true
                    } else {
                        _selectedTab.value = MainNavTab.HOME
                    }
                    verifyWalletLedgerHistory()
                    postStatus(res.message)
                }
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun registerOrSignInWithEmail(
        email: String,
        password: String,
        fullName: String,
        country: CountryOption
    ) {
        viewModelScope.launch {
            when (val res = repository.registerOrSignInWithEmail(email, password, fullName, country)) {
                is BackendResult.Success -> {
                    _currentUserId.value = res.data.tangerUserId
                    _isSignedUpAndAuthenticated.value = true
                    _showSignupScreen.value = false
                    if (!res.data.isOnboardingCompleted) {
                        _showOnboardingScreen.value = true
                    } else {
                        _selectedTab.value = MainNavTab.HOME
                    }
                    verifyWalletLedgerHistory()
                    postStatus(res.message)
                }
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun registerNewSocialOAuthAccount(
        provider: String,
        socialEmail: String,
        displayName: String,
        country: CountryOption
    ) {
        viewModelScope.launch {
            when (val res = repository.registerNewSocialOAuthAccount(provider, socialEmail, displayName, country)) {
                is BackendResult.Success -> {
                    _currentUserId.value = res.data.tangerUserId
                    _isSignedUpAndAuthenticated.value = true
                    _showSignupScreen.value = false
                    if (!res.data.isOnboardingCompleted) {
                        _showOnboardingScreen.value = true
                    } else {
                        _selectedTab.value = MainNavTab.HOME
                    }
                    verifyWalletLedgerHistory()
                    postStatus(res.message)
                }
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun completeOnboardingAndOpenProfile(
        selectedCountry: CountryOption,
        selectedGender: String,
        desiredUsername: String,
        fullNameInput: String,
        avatarUriInput: String,
        bioInput: String,
        destinationTab: MainNavTab = MainNavTab.HOME
    ) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.completeUserOnboarding(
                userId = uid,
                selectedCountry = selectedCountry,
                selectedGender = selectedGender,
                desiredUsername = desiredUsername,
                fullNameInput = fullNameInput,
                avatarUriInput = avatarUriInput,
                bioInput = bioInput
            )) {
                is BackendResult.Success -> {
                    _isSignedUpAndAuthenticated.value = true
                    _showOnboardingScreen.value = false
                    _showSignupScreen.value = false
                    _selectedTab.value = destinationTab
                    verifyWalletLedgerHistory()
                    postStatus("${res.message} • Welcome inside TANGER STAR VOICE!")
                }
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun switchUserAccount(userId: String, provider: String = "SESSION_SWITCH") {
        viewModelScope.launch {
            when (val res = repository.switchOrSocialLogin(userId, provider)) {
                is BackendResult.Success -> {
                    _currentUserId.value = res.data.tangerUserId
                    _isSignedUpAndAuthenticated.value = true
                    _showSignupScreen.value = false
                    _showOnboardingScreen.value = !res.data.isOnboardingCompleted
                    if (res.data.isOnboardingCompleted && _selectedTab.value == MainNavTab.SIGNUP) {
                        _selectedTab.value = MainNavTab.HOME
                    }
                    verifyWalletLedgerHistory()
                    postStatus(res.message)
                }
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun updateProfile(
        username: String,
        gender: String,
        dob: String,
        country: CountryOption,
        bio: String,
        avatarUri: String,
        fullName: String? = null,
        allowDirectMessages: Boolean? = null,
        showOnlineStatus: Boolean? = null
    ) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.updateProfile(
                userId = uid,
                username = username,
                gender = gender,
                dob = dob,
                country = country,
                bio = bio,
                avatarUri = avatarUri,
                fullName = fullName,
                allowDirectMessages = allowDirectMessages,
                showOnlineStatus = showOnlineStatus
            )) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    // ================= SOCIAL ACTIONS =================
    fun toggleFollow(targetUserId: String) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.toggleFollow(uid, targetUserId)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun blockUser(targetUserId: String, reason: String = "Blocked from user profile") {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.blockUser(uid, targetUserId, reason)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun reportUser(reportedUserId: String, category: String, details: String) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.reportUser(uid, reportedUserId, category, details)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    // ================= VOICE ROOM & HARDWARE MIC ACTIONS =================
    fun updateAudioPermissionGranted(granted: Boolean) {
        _hasAudioPermission.value = granted
        if (granted && !_isLocalMicMuted.value) {
            realtimeService.startRealMicrophoneCapture(true, false)
            postStatus("Hardware Microphone AudioRecord active (16kHz PCM)")
        } else if (!granted) {
            realtimeService.stopMicrophoneCapture()
        }
    }

    fun toggleLocalMicrophoneMute() {
        val newMuted = !_isLocalMicMuted.value
        _isLocalMicMuted.value = newMuted
        if (!newMuted && _hasAudioPermission.value) {
            realtimeService.startRealMicrophoneCapture(true, false)
            postStatus("Microphone Unmuted • Live Audio Streaming")
        } else {
            realtimeService.stopMicrophoneCapture()
            postStatus("Microphone Muted")
        }
    }

    fun joinOrLeaveSeat(seatIndex: Int) {
        val user = currentUser.value ?: return
        val roomId = _activeRoomId.value
        viewModelScope.launch {
            when (val res = repository.joinOrLeaveSeat(roomId, seatIndex, user)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun manageSeatByOwner(seatIndex: Int, toggleLock: Boolean, toggleMute: Boolean) {
        val uid = _currentUserId.value
        val roomId = _activeRoomId.value
        viewModelScope.launch {
            when (val res = repository.toggleSeatMuteOrLockByOwner(roomId, uid, seatIndex, toggleLock, toggleMute)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun assignRoomRole(targetUserId: String, roleType: String?) {
        val uid = _currentUserId.value
        val roomId = _activeRoomId.value
        viewModelScope.launch {
            when (val res = repository.assignOrRemoveRoomRole(roomId, uid, targetUserId, roleType)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun publishRoomTopBanner(
        title: String,
        messageText: String,
        iconName: String,
        bannerStyle: String,
        durationSeconds: Int
    ) {
        val uid = _currentUserId.value
        val roomId = _activeRoomId.value
        viewModelScope.launch {
            when (val res = repository.publishRoomTopNotification(
                roomId = roomId,
                actorUserId = uid,
                title = title,
                messageText = messageText,
                iconName = iconName,
                bannerStyle = bannerStyle,
                durationSeconds = durationSeconds
            )) {
                is BackendResult.Success -> {
                    _nowMillis.value = System.currentTimeMillis()
                    postStatus(res.message)
                }
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun updateRoomSettingsAndMusic(
        newTheme: String? = null,
        toggleChatLock: Boolean = false,
        musicTrack: String? = null,
        musicArtist: String? = null,
        isMusicPlaying: Boolean? = null,
        musicVolume: Float? = null
    ) {
        val uid = _currentUserId.value
        val roomId = _activeRoomId.value
        viewModelScope.launch {
            when (val res = repository.updateRoomSettingsAndMusic(
                roomId, uid, newTheme, toggleChatLock, musicTrack, musicArtist, isMusicPlaying, musicVolume
            )) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun createNewVoiceRoom(title: String, topic: String, isPrivate: Boolean, pinCode: String?, themeName: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            when (val res = repository.createVoiceRoom(user, title, topic, isPrivate, pinCode, themeName)) {
                is BackendResult.Success -> {
                    _activeRoomId.value = res.data.roomId
                    postStatus(res.message)
                }
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    // ================= ATOMIC GIFT & WALLET ACTIONS =================
    fun setWalletTxTypeFilter(txType: String) {
        _walletTxTypeFilter.value = txType
    }

    fun setWalletSearchQuery(query: String) {
        _walletSearchQuery.value = query
    }

    fun exchangeDiamondsToCoins(diamondsAmount: Long, customIdempotencyKey: String? = null) {
        val uid = _currentUserId.value
        val idemKey = customIdempotencyKey?.ifBlank { null }
            ?: "EXCHANGE_${uid}_${diamondsAmount}_${System.currentTimeMillis()}"
        viewModelScope.launch {
            when (val res = repository.exchangeDiamondsToCoinsAtomic(idemKey, uid, diamondsAmount)) {
                is BackendResult.Success -> {
                    postStatus(res.message)
                    verifyWalletLedgerHistory()
                }
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun claimVipDailyBonus() {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.claimVipDailyBonusAtomic(uid)) {
                is BackendResult.Success -> {
                    postStatus(res.message)
                    verifyWalletLedgerHistory()
                }
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun verifyWalletLedgerHistory() {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.verifyAndRetrieveUserWalletHistory(uid)) {
                is BackendResult.Success -> {
                    _walletVerificationReport.value = res.data
                    postStatus(res.message)
                }
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun sendGiftInRoom(receiverUserId: String, giftId: String, quantity: Int) {
        val senderId = _currentUserId.value
        val roomId = _activeRoomId.value
        val idempotencyKey = "GIFT_${senderId}_${receiverUserId}_${giftId}_${System.currentTimeMillis()}"
        viewModelScope.launch {
            when (val res = repository.sendGiftAtomicServerValidated(
                idempotencyKey = idempotencyKey,
                senderUserId = senderId,
                receiverUserId = receiverUserId,
                roomId = roomId,
                giftId = giftId,
                quantity = quantity
            )) {
                is BackendResult.Success -> {
                    postStatus(res.message)
                    verifyWalletLedgerHistory()
                }
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun sendGlobalGiftToUser(
        receiverUserId: String,
        giftId: String,
        quantity: Int,
        roomId: String = "GLOBAL"
    ) {
        val senderId = _currentUserId.value
        val idempotencyKey = "GLOBAL_GIFT_${senderId}_${receiverUserId}_${giftId}_${System.currentTimeMillis()}"
        viewModelScope.launch {
            when (val res = repository.sendGiftAtomicServerValidated(
                idempotencyKey = idempotencyKey,
                senderUserId = senderId,
                receiverUserId = receiverUserId,
                roomId = roomId,
                giftId = giftId,
                quantity = quantity
            )) {
                is BackendResult.Success -> {
                    postStatus(res.message)
                    verifyWalletLedgerHistory()
                }
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun dismissRecentGiftOverlay() {
        _recentGiftOverlay.value = null
    }

    fun testMonthBoundaryResetSimulation(year: Int, monthZeroBased: Int) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            val cycle = repository.ensureMonthlyDiamondCycleForUser(
                userId = uid,
                overrideYear = year,
                overrideMonthZeroBased = monthZeroBased
            )
            postStatus("Verified Cycle ${cycle.yearMonth} (${cycle.daysInMonth} days) • Previous cycles archived!")
        }
    }

    fun claimRankingReward(category: String, rankPosition: Int) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.claimServerRankingReward(uid, category, rankPosition)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    // ================= CHAT & CALL ACTIONS =================
    fun sendChatMessage(channelType: String, threadId: String, text: String, isVoiceNote: Boolean = false, voiceSec: Int = 0) {
        val sender = currentUser.value ?: return
        if (!isVoiceNote && text.isBlank()) return
        viewModelScope.launch {
            when (val res = repository.sendChatMessage(sender, channelType, threadId, text, isVoiceNote, voiceSec)) {
                is BackendResult.Success -> {}
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun startCallWithUser(receiver: UserEntity, callType: String) {
        val caller = currentUser.value ?: return
        viewModelScope.launch {
            when (val res = repository.startOrEndCallSession(caller, receiver, callType)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    // ================= MULTIPLAYER SERVER GAMES =================
    fun startNewLudoMatch(opponent: UserEntity, entryFeeCoins: Long, gameType: String = "LUDO_STAR") {
        val p1 = currentUser.value ?: return
        viewModelScope.launch {
            when (val res = repository.createServerLudoMatch(p1, opponent, entryFeeCoins, gameType)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun rollServerLudoDice(matchId: String) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.executeServerValidatedLudoTurn(matchId, uid)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    // ================= AGENCY ACTIONS =================
    fun inviteUserToAgency(agencyId: String, targetUserId: String, roleOffered: String) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.createOrInviteToAgency(uid, agencyId, targetUserId, roleOffered)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun respondToInvitation(invite: AgencyInvitationEntity, accept: Boolean) {
        viewModelScope.launch {
            when (val res = repository.respondToAgencyInvitation(invite, accept)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    // ================= GLOBAL OWNER ADMIN PANEL & RBAC =================
    fun onLogoDoubleTapEntryTrigger() {
        // Double-tap logo is ONLY an entry trigger, NEVER authentication.
        _showAdminAuthDialog.value = true
    }

    fun dismissAdminAuthDialog() {
        _showAdminAuthDialog.value = false
    }

    fun verifyOwnerAdminRbac(secretKeyInput: String) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.authenticateGlobalOwnerRbac(uid, secretKeyInput)) {
                is BackendResult.Success -> {
                    _isOwnerAdminAuthenticated.value = true
                    _showAdminAuthDialog.value = false
                    _showOwnerAdminScreen.value = true
                    postStatus(res.message)
                }
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun closeOwnerAdminScreen() {
        _showOwnerAdminScreen.value = false
    }

    fun ownerIssueCoinsOrDiamonds(targetUserId: String, coinsDelta: Long, diamondsDelta: Long, reason: String) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.ownerIssueCoinsOrDiamonds(uid, targetUserId, coinsDelta, diamondsDelta, reason)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun ownerSaveGift(
        giftId: String?,
        name: String,
        category: String,
        emoji: String,
        price: Long,
        active: Boolean,
        animationEffect: String = "LUCKY_SPIN",
        diamondReturnRatePercent: Int = 60,
        triggerCount: Long = 5L,
        rewardCoins: Long = 250L,
        cashbackCoins: Long = 30L,
        expBonus: Long = 50L,
        maxRewardCoins: Long = 1_000L,
        activationLimit: Long = 100L,
        startTimeMillis: Long = 0L,
        endTimeMillis: Long = 0L,
        cooldownSeconds: Int = 5,
        repeatTimingSeconds: Int = 3600,
        isPaused: Boolean = false,
        backupTimingNotes: String = "Auto-Backup & Repeat Timing Active"
    ) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.ownerSaveOrUpdateGift(
                actorUserId = uid,
                giftId = giftId,
                name = name,
                category = category,
                iconEmoji = emoji,
                coinPrice = price,
                isActive = active,
                animationEffect = animationEffect,
                diamondReturnRatePercent = diamondReturnRatePercent,
                triggerCount = triggerCount,
                rewardCoins = rewardCoins,
                cashbackCoins = cashbackCoins,
                expBonus = expBonus,
                maxRewardCoins = maxRewardCoins,
                activationLimit = activationLimit,
                startTimeMillis = startTimeMillis,
                endTimeMillis = endTimeMillis,
                cooldownSeconds = cooldownSeconds,
                repeatTimingSeconds = repeatTimingSeconds,
                isPaused = isPaused,
                backupTimingNotes = backupTimingNotes
            )) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun ownerToggleGiftActiveOrPaused(
        giftId: String,
        toggleActive: Boolean = false,
        togglePaused: Boolean = false
    ) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.ownerToggleGiftActiveOrPaused(uid, giftId, toggleActive, togglePaused)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun ownerResetLuckyGiftCounters(
        giftId: String? = null,
        resetGlobalCounter: Boolean = false
    ) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.ownerResetLuckyGiftCounters(uid, giftId, resetGlobalCounter)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun ownerUpdateGiftBackupAndTiming(
        giftId: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        cooldownSeconds: Int,
        repeatTimingSeconds: Int,
        backupTimingNotes: String
    ) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.ownerUpdateGiftBackupAndTiming(
                actorUserId = uid,
                giftId = giftId,
                startTimeMillis = startTimeMillis,
                endTimeMillis = endTimeMillis,
                cooldownSeconds = cooldownSeconds,
                repeatTimingSeconds = repeatTimingSeconds,
                backupTimingNotes = backupTimingNotes
            )) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun ownerToggleGlobalLuckyPause() {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.ownerToggleGlobalLuckyPause(uid)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun ownerDeleteGift(giftId: String) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.ownerDeleteGift(uid, giftId)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun ownerModerateUser(
        targetUserId: String,
        newRole: String? = null,
        newVipLevel: Int? = null,
        suspend: Boolean? = null,
        ban: Boolean? = null
    ) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.ownerUpdateModerationAndRole(uid, targetUserId, newRole, newVipLevel, suspend, ban)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun ownerPublishEventOrBanner(
        title: String,
        subtitle: String,
        contentType: String,
        targetScope: String,
        targetValue: String,
        rewardInfo: String
    ) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.ownerCreateGlobalEventOrAnnouncement(
                uid, title, subtitle, contentType, targetScope, targetValue, rewardInfo
            )) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun ownerUpdateLuckyMilestones(m1Threshold: Long, m1Reward: Long, jackpotThreshold: Long, jackpotReward: Long) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.ownerUpdateLuckyMilestones(uid, m1Threshold, m1Reward, jackpotThreshold, jackpotReward)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    // ================= SOCIAL POSTS, STORIES, REELS, LIVE STREAMS, CLOUD MEDIA & BACKUPS =================
    fun publishSocialPost(caption: String, mediaUri: String, mediaType: String = "IMAGE", locationTag: String = "Global TANGERAM") {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.createSocialPost(uid, caption, mediaUri, mediaType, locationTag)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun likeOrCommentPost(postId: String, commentText: String? = null) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.likeOrCommentSocialPost(uid, postId, commentText)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun publishStory24h(captionText: String, mediaUri: String, backgroundTheme: String = "EMERALD_GLOW") {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.createStory24h(uid, captionText, mediaUri, backgroundTheme)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun viewOrLikeStory(storyId: String, like: Boolean) {
        viewModelScope.launch {
            when (val res = repository.viewOrLikeStory(storyId, like)) {
                is BackendResult.Success -> if (like) postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun publishReel(title: String, audioTrackName: String, mediaUri: String, durationSec: Int = 20) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.createReel(uid, title, audioTrackName, mediaUri, durationSec)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun likeOrShareReel(reelId: String, share: Boolean = false) {
        viewModelScope.launch {
            when (val res = repository.likeOrShareReel(reelId, share)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun startLiveStream(streamTitle: String, category: String = "PK_BATTLE", isPkBattle: Boolean = true, pkOpponentUserId: String? = null) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.startOrJoinLiveStream(uid, streamTitle, category, isPkBattle, pkOpponentUserId)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun boostLivePkScore(streamId: String, forHost: Boolean, points: Long = 250L) {
        viewModelScope.launch {
            when (val res = repository.boostLiveStreamPkScore(streamId, forHost, points)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun uploadMediaToCloudCdn(assetType: String, localUri: String) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.registerCloudMediaUpload(uid, assetType, localUri)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }

    fun ownerTriggerDatabaseBackup() {
        val uid = _currentUserId.value
        viewModelScope.launch {
            when (val res = repository.ownerCreateDatabaseBackupSnapshot(uid)) {
                is BackendResult.Success -> postStatus(res.message)
                is BackendResult.Error -> postStatus(res.errorMessage)
            }
        }
    }
}
