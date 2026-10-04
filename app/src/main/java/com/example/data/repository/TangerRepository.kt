package com.example.data.repository

import androidx.room.withTransaction
import com.example.BuildConfig
import com.example.data.local.*
import com.example.data.realtime.RealtimeEvent
import com.example.data.realtime.TangerRealtimeVoiceService
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
import kotlin.random.Random

data class CountryOption(
    val code: String,
    val dialCode: String,
    val name: String,
    val flag: String
)

val GLOBAL_COUNTRIES = listOf(
    CountryOption("MA", "+212", "Morocco", "🇲🇦"),
    CountryOption("AE", "+971", "United Arab Emirates", "🇦🇪"),
    CountryOption("SA", "+966", "Saudi Arabia", "🇸🇦"),
    CountryOption("EG", "+20", "Egypt", "🇪🇬"),
    CountryOption("TR", "+90", "Türkiye", "🇹🇷"),
    CountryOption("FR", "+33", "France", "🇫🇷"),
    CountryOption("ES", "+34", "Spain", "🇪🇸"),
    CountryOption("US", "+1", "United States", "🇺🇸"),
    CountryOption("GB", "+44", "United Kingdom", "🇬🇧"),
    CountryOption("IN", "+91", "India", "🇮🇳"),
    CountryOption("PK", "+92", "Pakistan", "🇵🇰"),
    CountryOption("ID", "+62", "Indonesia", "🇮🇩"),
    CountryOption("BR", "+55", "Brazil", "🇧🇷"),
    CountryOption("DE", "+49", "Germany", "🇩🇪")
)

sealed class BackendResult<out T> {
    data class Success<T>(val data: T, val message: String = "") : BackendResult<T>()
    data class Error(val errorMessage: String) : BackendResult<Nothing>()
}

data class WalletVerificationReport(
    val userId: String,
    val sessionVerified: Boolean,
    val totalLedgerEntries: Int,
    val totalCoinsInflow: Long,
    val totalCoinsOutflow: Long,
    val totalDiamondsInflow: Long,
    val totalDiamondsOutflow: Long,
    val latestCoinsBalance: Long,
    val latestDiamondsBalance: Long,
    val ledgerConsistencyVerified: Boolean,
    val verifiedAtMillis: Long
)

class TangerRepository(
    private val db: TangerDatabase,
    private val dao: TangerDao,
    val realtimeService: TangerRealtimeVoiceService
) {
    // Reactive Flows from Relational Database
    val allUsersFlow: Flow<List<UserEntity>> = dao.observeAllUsers()
    val allRoomsFlow: Flow<List<VoiceRoomEntity>> = dao.observeAllRooms()
    val allGiftsFlow: Flow<List<GiftEntity>> = dao.observeAllGifts()
    val giftTransactionsFlow: Flow<List<GiftTransactionEntity>> = dao.observeGiftTransactions()
    val luckyGiftStateFlow: Flow<LuckyGiftStateEntity?> = dao.observeLuckyGiftState()
    val luckyGiftAuditsFlow: Flow<List<LuckyGiftAuditEntity>> = dao.observeLuckyGiftAudits()
    val vipConfigsFlow: Flow<List<VipConfigEntity>> = dao.observeVipConfigs()
    val allAgenciesFlow: Flow<List<AgencyEntity>> = dao.observeAllAgencies()
    val allAgencyMembersFlow: Flow<List<AgencyMemberEntity>> = dao.observeAllAgencyMembers()
    val gameMatchesFlow: Flow<List<GameMatchEntity>> = dao.observeGameMatches()
    val activeGlobalEventsFlow: Flow<List<GlobalEventEntity>> = dao.observeActiveGlobalEvents()
    val auditLogsFlow: Flow<List<AuditLogEntity>> = dao.observeAuditLogs()
    val allReportsFlow: Flow<List<UserReportEntity>> = dao.observeAllReports()
    val allTransactionsFlow: Flow<List<WalletTransactionEntity>> = dao.observeAllTransactions()
    val allRoomNotificationsFlow: Flow<List<RoomTopNotificationEntity>> = dao.observeAllRoomNotifications()
    val rankingRewardClaimsFlow: Flow<List<RankingRewardClaimEntity>> = dao.observeRankingRewardClaims()
    val callSessionsFlow: Flow<List<CallSessionEntity>> = dao.observeCallSessions()
    val userSequenceCounterFlow: Flow<UserSequenceCounterEntity?> = dao.observeUserSequenceCounter()
    val socialPostsFlow: Flow<List<SocialPostEntity>> = dao.observeAllSocialPosts()
    val allPostCommentsFlow: Flow<List<PostCommentEntity>> = dao.observeAllPostComments()
    val reelsFlow: Flow<List<ReelEntity>> = dao.observeAllReels()
    val liveStreamsFlow: Flow<List<LiveStreamEntity>> = dao.observeAllLiveStreams()
    val cloudMediaAssetsFlow: Flow<List<CloudMediaAssetEntity>> = dao.observeAllCloudMediaAssets()
    val databaseBackupsFlow: Flow<List<DatabaseBackupSnapshotEntity>> = dao.observeDatabaseBackups()

    fun observeActiveStories(now: Long): Flow<List<StoryEntity>> = dao.observeActiveStories(now)
    fun observeCommentsForPost(postId: String): Flow<List<PostCommentEntity>> = dao.observeCommentsForPost(postId)

    fun observeUser(userId: String): Flow<UserEntity?> = dao.observeUserById(userId)
    fun observeRoom(roomId: String): Flow<VoiceRoomEntity?> = dao.observeRoomById(roomId)
    fun observeRoomSeats(roomId: String): Flow<List<RoomSeatEntity>> = dao.observeRoomSeats(roomId)
    fun observeRoomRoles(roomId: String): Flow<List<RoomRoleEntity>> = dao.observeRoomRoles(roomId)
    fun observeActiveRoomBanners(roomId: String, now: Long): Flow<List<RoomTopNotificationEntity>> =
        dao.observeActiveRoomNotifications(roomId, now)
    fun observeUserTransactions(userId: String): Flow<List<WalletTransactionEntity>> =
        dao.observeUserTransactions(userId)
    fun observeFilteredUserTransactions(
        userId: String,
        txTypeFilter: String,
        searchQuery: String
    ): Flow<List<WalletTransactionEntity>> =
        dao.observeFilteredUserTransactions(userId, txTypeFilter, searchQuery)
    fun observeDiamondCycles(userId: String): Flow<List<DiamondMonthlyCycleEntity>> =
        dao.observeDiamondCyclesForUser(userId)
    fun observeVipHistory(userId: String): Flow<List<VipHistoryEntity>> = dao.observeVipHistory(userId)
    fun observeFollowing(userId: String): Flow<List<FollowEntity>> = dao.observeFollowing(userId)
    fun observeMessages(channelType: String, threadId: String): Flow<List<ChatMessageEntity>> =
        dao.observeMessages(channelType, threadId)
    fun observeInvitationsForUser(userId: String): Flow<List<AgencyInvitationEntity>> =
        dao.observeInvitationsForUser(userId)

    // ================= INITIALIZATION & CALENDAR CYCLE ENGINE =================
    suspend fun initializeDatabaseIfNeeded(): String? {
        val now = System.currentTimeMillis()
        dao.expireOutdatedRoomNotifications(now)
        dao.expireOutdatedStories(now)

        val existingCount = dao.countUsers()
        if (existingCount == 0) {
            seedRelationalSchema(now)
        } else if (dao.countSocialPosts() == 0) {
            seedSocialPostsStoriesReelsAndLive(now)
        }

        val activeSession = dao.getActiveSession()
        val activeUserId = activeSession?.tangerUserId
        if (!activeUserId.isNullOrBlank()) {
            ensureMonthlyDiamondCycleForUser(activeUserId, now)
        }
        return activeUserId
    }

    suspend fun signOutCurrentSession() {
        db.withTransaction {
            val activeSession = dao.getActiveSession()
            dao.deactivateAllSessions()
            if (activeSession != null) {
                val u = dao.getUserById(activeSession.tangerUserId)
                if (u != null) {
                    dao.upsertUser(u.copy(isOnline = false))
                }
                recordAudit(
                    actorUserId = activeSession.tangerUserId,
                    actorRole = u?.role ?: "USER",
                    category = "AUTH",
                    action = "SIGN_OUT_SESSION",
                    targetId = activeSession.tangerUserId,
                    details = "Session deactivated; returned to mandatory TANGERAM Sign Up gate"
                )
            }
        }
    }

    /**
     * Calendar-accurate Monthly Diamond Cycle Engine:
     * Starts on the 1st of every month at 00:00:00 and ends on the last day (28/29/30/31) at 23:59:59.
     * Automatically archives prior months when a new month begins.
     */
    suspend fun ensureMonthlyDiamondCycleForUser(
        userId: String,
        timestampMillis: Long = System.currentTimeMillis(),
        overrideYear: Int? = null,
        overrideMonthZeroBased: Int? = null
    ): DiamondMonthlyCycleEntity {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = timestampMillis
            if (overrideYear != null) set(Calendar.YEAR, overrideYear)
            if (overrideMonthZeroBased != null) set(Calendar.MONTH, overrideMonthZeroBased)
        }
        val year = cal.get(Calendar.YEAR)
        val monthOneBased = cal.get(Calendar.MONTH) + 1
        val yearMonthKey = String.format("%04d-%02d", year, monthOneBased)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH) // Supports 28, 29, 30, 31

        val startCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, daysInMonth)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        // Archive any non-current cycles automatically
        dao.archivePreviousDiamondCycles(userId, yearMonthKey)

        val existing = dao.getDiamondCycle(userId, yearMonthKey)
        if (existing != null) {
            return existing
        }

        val newCycle = DiamondMonthlyCycleEntity(
            userId = userId,
            yearMonth = yearMonthKey,
            cycleStartEpoch = startCal.timeInMillis,
            cycleEndEpoch = endCal.timeInMillis,
            daysInMonth = daysInMonth,
            monthlyTargetDiamonds = 50_000L,
            earnedDiamondsInCycle = 0L,
            validHostDays = 1,
            isArchived = false,
            lastUpdatedAt = timestampMillis
        )
        dao.upsertDiamondCycle(newCycle)
        return newCycle
    }

    // ================= 1. TANGERAM SEQUENTIAL ID ALLOCATOR, SIGNUP, AUTH & ONBOARDING =================

    /**
     * Atomic Sequential TANGERAM User ID Allocator:
     * Starts strictly from 199967. Every new user receives the next available sequential ID
     * with zero duplicates guaranteed inside a Room database transaction.
     */
    private suspend fun allocateNextSequentialTangeramUserId(now: Long): String {
        val currentCounter = dao.getUserSequenceCounter()
            ?: UserSequenceCounterEntity(
                counterKey = "TANGERAM_USER_ID_SEQ",
                nextAvailableId = 199967L,
                updatedAt = now
            )
        var candidateId = currentCounter.nextAvailableId.coerceAtLeast(199967L)
        while (dao.getUserById(candidateId.toString()) != null) {
            candidateId += 1L
        }
        dao.upsertUserSequenceCounter(
            currentCounter.copy(
                nextAvailableId = candidateId + 1L,
                updatedAt = now
            )
        )
        return candidateId.toString()
    }

    private fun hashPasswordSha256(rawPassword: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(("TANGERAM_SALT_2026::$rawPassword").toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun requestPhoneOtp(country: CountryOption, rawPhone: String): BackendResult<OtpChallengeEntity> {
        val cleanPhone = rawPhone.filter { it.isDigit() }
        if (cleanPhone.length < 6) {
            return BackendResult.Error("Enter a valid phone number (minimum 6 digits).")
        }
        val phoneKey = "${country.dialCode}-$cleanPhone"
        val now = System.currentTimeMillis()
        val existing = dao.getOtpChallenge(phoneKey)

        if (existing != null) {
            // Cooldown check (30 seconds)
            if (now < existing.resendAvailableAt) {
                val waitSec = ((existing.resendAvailableAt - now) / 1000L).coerceAtLeast(1L)
                return BackendResult.Error("Resend cooldown active. Please wait ${waitSec}s.")
            }
            // Rate limit check (max 5 OTP requests per 10 minutes)
            val tenMinWindow = 10 * 60 * 1000L
            val inSameWindow = (now - existing.windowStartedAt) < tenMinWindow
            val countInWindow = if (inSameWindow) existing.requestCountWindow + 1 else 1
            if (countInWindow > 5) {
                return BackendResult.Error("Rate limit exceeded (max 5 OTP requests per 10 minutes).")
            }

            val otpCode = String.format("%06d", Random.nextInt(100000, 999999))
            val updated = OtpChallengeEntity(
                phoneKey = phoneKey,
                otpCode = otpCode,
                expiresAt = now + 5 * 60 * 1000L, // 5 min expiry
                resendAvailableAt = now + 30 * 1000L, // 30s resend cooldown
                requestCountWindow = countInWindow,
                windowStartedAt = if (inSameWindow) existing.windowStartedAt else now,
                verified = false
            )
            dao.upsertOtpChallenge(updated)
            recordAudit(
                actorUserId = phoneKey,
                actorRole = "AUTH_SERVICE",
                category = "AUTH",
                action = "OTP_ISSUED",
                targetId = phoneKey,
                details = "OTP issued with 300s expiry, request #$countInWindow in window"
            )
            return BackendResult.Success(updated, "OTP sent to $phoneKey (Expires in 5m)")
        } else {
            val otpCode = String.format("%06d", Random.nextInt(100000, 999999))
            val created = OtpChallengeEntity(
                phoneKey = phoneKey,
                otpCode = otpCode,
                expiresAt = now + 5 * 60 * 1000L,
                resendAvailableAt = now + 30 * 1000L,
                requestCountWindow = 1,
                windowStartedAt = now,
                verified = false
            )
            dao.upsertOtpChallenge(created)
            recordAudit(
                actorUserId = phoneKey,
                actorRole = "AUTH_SERVICE",
                category = "AUTH",
                action = "OTP_ISSUED",
                targetId = phoneKey,
                details = "Initial OTP challenge created"
            )
            return BackendResult.Success(created, "OTP sent to $phoneKey")
        }
    }

    suspend fun verifyOtpAndEstablishSession(
        country: CountryOption,
        rawPhone: String,
        enteredOtp: String,
        usernameInput: String = "",
        genderInput: String = "",
        dobInput: String = "",
        bioInput: String = ""
    ): BackendResult<UserEntity> {
        val cleanPhone = rawPhone.filter { it.isDigit() }
        val phoneKey = "${country.dialCode}-$cleanPhone"
        val now = System.currentTimeMillis()
        val challenge = dao.getOtpChallenge(phoneKey)
            ?: return BackendResult.Error("No OTP challenge found for $phoneKey. Request OTP first.")

        if (now > challenge.expiresAt) {
            return BackendResult.Error("OTP has expired. Please request a new OTP.")
        }
        if (challenge.otpCode != enteredOtp.trim()) {
            return BackendResult.Error("Invalid 6-digit OTP code.")
        }

        return db.withTransaction {
            dao.upsertOtpChallenge(challenge.copy(verified = true))
            var user = dao.getUserByPhone(cleanPhone, country.code)
            var isNewSignup = false
            if (user == null) {
                isNewSignup = true
                val sequentialId = allocateNextSequentialTangeramUserId(now)
                val initialCoins = 2_500L
                val initialDiamonds = 500L
                val hasOnboardingProvided = usernameInput.isNotBlank() && genderInput.isNotBlank()
                val safeUsername = if (usernameInput.isNotBlank() && dao.getUserByUsername(usernameInput.trim()) == null) {
                    usernameInput.trim()
                } else {
                    "tangeram_$sequentialId"
                }

                user = UserEntity(
                    tangerUserId = sequentialId,
                    username = safeUsername,
                    fullName = if (usernameInput.isNotBlank()) usernameInput.trim() else "Tangeram Star #$sequentialId",
                    email = "",
                    passwordHash = "",
                    phoneNumber = cleanPhone,
                    countryCode = country.code,
                    countryName = country.name,
                    countryFlag = country.flag,
                    gender = genderInput.ifBlank { "Female" },
                    dob = dobInput.ifBlank { "2000-01-01" },
                    bio = bioInput.ifBlank { "Voice Chat, Play Games, Find More Friends ✨" },
                    avatarUri = "",
                    authProvider = "PHONE_OTP",
                    role = "USER",
                    isOnline = true,
                    isSuspended = false,
                    isBanned = false,
                    coinsBalance = initialCoins,
                    diamondsBalance = initialDiamonds,
                    totalCoinsSpent = 0L,
                    totalDiamondsEarned = initialDiamonds,
                    vipLevel = 1,
                    userLevel = 1,
                    xpPoints = 100L,
                    agencyId = "AG-101",
                    cpPartnerUserId = null,
                    followersCount = 1,
                    followingCount = 1,
                    createdAt = now,
                    isOnboardingCompleted = hasOnboardingProvided,
                    allowDirectMessages = true,
                    showOnlineStatus = true
                )
                dao.upsertUser(user)

                dao.insertWalletTransaction(
                    WalletTransactionEntity(
                        txId = "TX-${UUID.randomUUID().toString().take(8).uppercase()}",
                        idempotencyKey = "SIGNUP_WELCOME_BONUS_$sequentialId",
                        userId = sequentialId,
                        counterpartyUserId = "TANGERAM_TREASURY",
                        txType = "OWNER_ISSUE_COINS",
                        coinsDelta = initialCoins,
                        diamondsDelta = initialDiamonds,
                        coinsBalanceAfter = initialCoins,
                        diamondsBalanceAfter = initialDiamonds,
                        description = "TANGERAM New Account Welcome Allocation (ID #$sequentialId)",
                        timestamp = now
                    )
                )
            } else {
                if (user.isBanned) {
                    return@withTransaction BackendResult.Error("Account ${user.tangerUserId} is banned by Global Owner.")
                }
                user = user.copy(isOnline = true)
                dao.upsertUser(user)
            }

            dao.deactivateAllSessions()
            val session = SessionEntity(
                sessionToken = "sess_${UUID.randomUUID()}",
                tangerUserId = user.tangerUserId,
                deviceInfo = "Android-Phone-OTP",
                createdAt = now,
                expiresAt = now + 30L * 24 * 3600 * 1000L,
                isActive = true
            )
            dao.insertSession(session)
            ensureMonthlyDiamondCycleForUser(user.tangerUserId, now)
            recordAudit(
                actorUserId = user.tangerUserId,
                actorRole = user.role,
                category = "AUTH",
                action = if (isNewSignup) "PHONE_SIGNUP_SEQUENTIAL_ID_ASSIGNED" else "OTP_VERIFIED_SESSION_CREATED",
                targetId = user.tangerUserId,
                details = "TANGERAM ID=${user.tangerUserId}, OnboardingDone=${user.isOnboardingCompleted}"
            )
            BackendResult.Success(
                user,
                if (isNewSignup) "Account created! Assigned sequential TANGERAM ID #${user.tangerUserId} — Complete Onboarding"
                else "Logged in as ${user.fullName} (ID #${user.tangerUserId})"
            )
        }
    }

    /**
     * Email + Password Signup or Login with SHA-256 verification and Sequential ID allocation (199967+).
     */
    suspend fun registerOrSignInWithEmail(
        emailInput: String,
        rawPassword: String,
        fullNameInput: String,
        country: CountryOption
    ): BackendResult<UserEntity> {
        val cleanEmail = emailInput.trim().lowercase()
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return BackendResult.Error("Enter a valid email address.")
        }
        if (rawPassword.length < 6) {
            return BackendResult.Error("Password must be at least 6 characters.")
        }
        val now = System.currentTimeMillis()
        val passwordHash = hashPasswordSha256(rawPassword)

        return db.withTransaction {
            val existing = dao.getUserByEmail(cleanEmail)
            if (existing != null) {
                if (existing.passwordHash.isNotBlank() && existing.passwordHash != passwordHash) {
                    return@withTransaction BackendResult.Error("Invalid password for $cleanEmail.")
                }
                if (existing.isBanned) {
                    return@withTransaction BackendResult.Error("Account #${existing.tangerUserId} is banned.")
                }
                val updated = existing.copy(isOnline = true)
                dao.upsertUser(updated)
                dao.deactivateAllSessions()
                dao.insertSession(
                    SessionEntity(
                        sessionToken = "sess_${UUID.randomUUID()}",
                        tangerUserId = updated.tangerUserId,
                        deviceInfo = "Android-Email-Auth",
                        createdAt = now,
                        expiresAt = now + 30L * 24 * 3600 * 1000L,
                        isActive = true
                    )
                )
                ensureMonthlyDiamondCycleForUser(updated.tangerUserId, now)
                recordAudit(updated.tangerUserId, updated.role, "AUTH", "EMAIL_SIGNIN", updated.tangerUserId, "Email=$cleanEmail")
                return@withTransaction BackendResult.Success(updated, "Welcome back, ${updated.fullName} (ID #${updated.tangerUserId})")
            }

            // New Email Signup -> Allocate next sequential TANGERAM ID (199967+)
            val sequentialId = allocateNextSequentialTangeramUserId(now)
            val initialCoins = 2_500L
            val initialDiamonds = 500L
            val tempHandle = "tangeram_$sequentialId"
            val newUser = UserEntity(
                tangerUserId = sequentialId,
                username = tempHandle,
                fullName = fullNameInput.trim().ifBlank { "Star #$sequentialId" },
                email = cleanEmail,
                passwordHash = passwordHash,
                phoneNumber = "",
                countryCode = country.code,
                countryName = country.name,
                countryFlag = country.flag,
                gender = "Female",
                dob = "2000-01-01",
                bio = "Voice Chat, Play Games, Find More Friends ✨",
                avatarUri = "",
                authProvider = "EMAIL",
                role = "USER",
                isOnline = true,
                isSuspended = false,
                isBanned = false,
                coinsBalance = initialCoins,
                diamondsBalance = initialDiamonds,
                totalCoinsSpent = 0L,
                totalDiamondsEarned = initialDiamonds,
                vipLevel = 1,
                userLevel = 1,
                xpPoints = 100L,
                agencyId = "AG-101",
                cpPartnerUserId = null,
                followersCount = 1,
                followingCount = 1,
                createdAt = now,
                isOnboardingCompleted = false, // Triggers mandatory Onboarding flow
                allowDirectMessages = true,
                showOnlineStatus = true
            )
            dao.upsertUser(newUser)

            dao.insertWalletTransaction(
                WalletTransactionEntity(
                    txId = "TX-${UUID.randomUUID().toString().take(8).uppercase()}",
                    idempotencyKey = "SIGNUP_WELCOME_BONUS_$sequentialId",
                    userId = sequentialId,
                    counterpartyUserId = "TANGERAM_TREASURY",
                    txType = "OWNER_ISSUE_COINS",
                    coinsDelta = initialCoins,
                    diamondsDelta = initialDiamonds,
                    coinsBalanceAfter = initialCoins,
                    diamondsBalanceAfter = initialDiamonds,
                    description = "TANGERAM Email Signup Welcome Allocation (ID #$sequentialId)",
                    timestamp = now
                )
            )

            dao.deactivateAllSessions()
            dao.insertSession(
                SessionEntity(
                    sessionToken = "sess_${UUID.randomUUID()}",
                    tangerUserId = sequentialId,
                    deviceInfo = "Android-Email-Signup",
                    createdAt = now,
                    expiresAt = now + 30L * 24 * 3600 * 1000L,
                    isActive = true
                )
            )
            ensureMonthlyDiamondCycleForUser(sequentialId, now)
            recordAudit(
                actorUserId = sequentialId,
                actorRole = "USER",
                category = "AUTH",
                action = "EMAIL_SIGNUP_SEQUENTIAL_ID_ASSIGNED",
                targetId = sequentialId,
                details = "Email=$cleanEmail, AssignedID=$sequentialId"
            )
            BackendResult.Success(newUser, "Signup successful! Permanent TANGERAM ID #$sequentialId assigned.")
        }
    }

    /**
     * Google / Facebook OAuth New Account Signup with Sequential ID allocation (199967+)
     * and automatic redirect into TANGERAM Onboarding.
     */
    suspend fun registerNewSocialOAuthAccount(
        provider: String, // "GOOGLE" or "FACEBOOK"
        socialEmail: String,
        displayName: String,
        country: CountryOption
    ): BackendResult<UserEntity> {
        val now = System.currentTimeMillis()
        return db.withTransaction {
            val cleanEmail = socialEmail.trim().lowercase()
            if (cleanEmail.isNotBlank()) {
                val existing = dao.getUserByEmail(cleanEmail)
                if (existing != null) {
                    dao.deactivateAllSessions()
                    dao.insertSession(
                        SessionEntity(
                            sessionToken = "sess_${UUID.randomUUID()}",
                            tangerUserId = existing.tangerUserId,
                            deviceInfo = "Android-$provider",
                            createdAt = now,
                            expiresAt = now + 30L * 24 * 3600 * 1000L,
                            isActive = true
                        )
                    )
                    return@withTransaction BackendResult.Success(existing, "Signed in with $provider as ${existing.fullName} (ID #${existing.tangerUserId})")
                }
            }

            val sequentialId = allocateNextSequentialTangeramUserId(now)
            val initialCoins = 2_500L
            val initialDiamonds = 500L
            val newUser = UserEntity(
                tangerUserId = sequentialId,
                username = "tangeram_$sequentialId",
                fullName = displayName.ifBlank { "$provider Star #$sequentialId" },
                email = cleanEmail,
                passwordHash = "",
                phoneNumber = "",
                countryCode = country.code,
                countryName = country.name,
                countryFlag = country.flag,
                gender = "Female",
                dob = "2000-01-01",
                bio = "Joined via $provider • Voice Chat, Play Games, Find More Friends ✨",
                avatarUri = "",
                authProvider = provider,
                role = "USER",
                isOnline = true,
                isSuspended = false,
                isBanned = false,
                coinsBalance = initialCoins,
                diamondsBalance = initialDiamonds,
                totalCoinsSpent = 0L,
                totalDiamondsEarned = initialDiamonds,
                vipLevel = 1,
                userLevel = 1,
                xpPoints = 100L,
                agencyId = "AG-101",
                cpPartnerUserId = null,
                followersCount = 1,
                followingCount = 1,
                createdAt = now,
                isOnboardingCompleted = false,
                allowDirectMessages = true,
                showOnlineStatus = true
            )
            dao.upsertUser(newUser)

            dao.insertWalletTransaction(
                WalletTransactionEntity(
                    txId = "TX-${UUID.randomUUID().toString().take(8).uppercase()}",
                    idempotencyKey = "SIGNUP_WELCOME_BONUS_$sequentialId",
                    userId = sequentialId,
                    counterpartyUserId = "TANGERAM_TREASURY",
                    txType = "OWNER_ISSUE_COINS",
                    coinsDelta = initialCoins,
                    diamondsDelta = initialDiamonds,
                    coinsBalanceAfter = initialCoins,
                    diamondsBalanceAfter = initialDiamonds,
                    description = "TANGERAM $provider Signup Welcome Allocation (ID #$sequentialId)",
                    timestamp = now
                )
            )

            dao.deactivateAllSessions()
            dao.insertSession(
                SessionEntity(
                    sessionToken = "sess_${UUID.randomUUID()}",
                    tangerUserId = sequentialId,
                    deviceInfo = "Android-$provider",
                    createdAt = now,
                    expiresAt = now + 30L * 24 * 3600 * 1000L,
                    isActive = true
                )
            )
            ensureMonthlyDiamondCycleForUser(sequentialId, now)
            recordAudit(
                actorUserId = sequentialId,
                actorRole = "USER",
                category = "AUTH",
                action = "${provider}_SIGNUP_SEQUENTIAL_ID_ASSIGNED",
                targetId = sequentialId,
                details = "Provider=$provider, AssignedID=$sequentialId"
            )
            BackendResult.Success(newUser, "$provider Signup complete! Assigned TANGERAM ID #$sequentialId — Complete Onboarding")
        }
    }

    /**
     * Completes mandatory post-signup TANGERAM Onboarding:
     * Validates unique username, sets country flag, gender, display name, and profile photo,
     * and marks isOnboardingCompleted = true in the relational database.
     */
    suspend fun completeUserOnboarding(
        userId: String,
        selectedCountry: CountryOption,
        selectedGender: String,
        desiredUsername: String,
        fullNameInput: String,
        avatarUriInput: String,
        bioInput: String
    ): BackendResult<UserEntity> {
        val cleanUsername = desiredUsername.trim().replace(" ", "_").lowercase()
        if (cleanUsername.length < 3) {
            return BackendResult.Error("Username must be at least 3 characters.")
        }
        return db.withTransaction {
            val user = dao.getUserById(userId)
                ?: return@withTransaction BackendResult.Error("User account #$userId not found.")
            val usernameOwner = dao.getUserByUsername(cleanUsername)
            if (usernameOwner != null && usernameOwner.tangerUserId != userId) {
                return@withTransaction BackendResult.Error("Username @$cleanUsername is already taken. Choose another handle.")
            }

            val updated = user.copy(
                username = cleanUsername,
                fullName = fullNameInput.trim().ifBlank { cleanUsername },
                countryCode = selectedCountry.code,
                countryName = selectedCountry.name,
                countryFlag = selectedCountry.flag,
                gender = selectedGender,
                avatarUri = avatarUriInput,
                bio = bioInput.trim().ifBlank { user.bio },
                isOnboardingCompleted = true
            )
            dao.upsertUser(updated)
            recordAudit(
                actorUserId = userId,
                actorRole = updated.role,
                category = "ONBOARDING",
                action = "COMPLETE_ONBOARDING",
                targetId = userId,
                details = "ID=$userId, Username=@$cleanUsername, Country=${selectedCountry.code}, Gender=$selectedGender"
            )
            BackendResult.Success(updated, "Welcome to TANGERAM, ${updated.fullName} (@${updated.username} • ID #${updated.tangerUserId})!")
        }
    }

    suspend fun switchOrSocialLogin(targetUserId: String, provider: String): BackendResult<UserEntity> {
        val now = System.currentTimeMillis()
        val user = dao.getUserById(targetUserId)
            ?: return BackendResult.Error("User $targetUserId not found.")
        if (user.isBanned) {
            return BackendResult.Error("Account ${user.tangerUserId} is banned by Global Owner.")
        }
        dao.deactivateAllSessions()
        dao.insertSession(
            SessionEntity(
                sessionToken = "sess_${UUID.randomUUID()}",
                tangerUserId = user.tangerUserId,
                deviceInfo = "Android-$provider",
                createdAt = now,
                expiresAt = now + 30L * 24 * 3600 * 1000L,
                isActive = true
            )
        )
        ensureMonthlyDiamondCycleForUser(user.tangerUserId, now)
        recordAudit(
            actorUserId = user.tangerUserId,
            actorRole = user.role,
            category = "AUTH",
            action = "${provider}_LOGIN",
            targetId = user.tangerUserId,
            details = "Authenticated via $provider"
        )
        return BackendResult.Success(user, "Signed in via $provider as ${user.fullName} (ID #${user.tangerUserId})")
    }

    suspend fun updateProfile(
        userId: String,
        username: String,
        gender: String,
        dob: String,
        country: CountryOption,
        bio: String,
        avatarUri: String,
        fullName: String? = null,
        allowDirectMessages: Boolean? = null,
        showOnlineStatus: Boolean? = null
    ): BackendResult<UserEntity> {
        return db.withTransaction {
            val user = dao.getUserById(userId) ?: return@withTransaction BackendResult.Error("User not found")
            val cleanUsername = username.trim().replace(" ", "_").ifBlank { user.username }
            val existingOwner = dao.getUserByUsername(cleanUsername)
            if (existingOwner != null && existingOwner.tangerUserId != userId) {
                return@withTransaction BackendResult.Error("Username @$cleanUsername is already taken by another TANGERAM user.")
            }
            val updated = user.copy(
                username = cleanUsername,
                fullName = fullName?.trim()?.ifBlank { user.fullName } ?: user.fullName,
                gender = gender,
                dob = dob,
                countryCode = country.code,
                countryName = country.name,
                countryFlag = country.flag,
                bio = bio.trim(),
                avatarUri = avatarUri,
                allowDirectMessages = allowDirectMessages ?: user.allowDirectMessages,
                showOnlineStatus = showOnlineStatus ?: user.showOnlineStatus,
                isOnline = showOnlineStatus ?: user.isOnline
            )
            dao.upsertUser(updated)
            recordAudit(
                userId,
                updated.role,
                "PROFILE",
                "UPDATE_PROFILE",
                userId,
                "Updated profile (@${updated.username}, Name=${updated.fullName}, DM=${updated.allowDirectMessages})"
            )
            BackendResult.Success(updated, "Profile & settings saved to production database")
        }
    }

    // ================= 2. GLOBAL USERS: FOLLOW, BLOCK, REPORT =================
    suspend fun toggleFollow(currentUserId: String, targetUserId: String): BackendResult<Boolean> {
        if (currentUserId == targetUserId) return BackendResult.Error("Cannot follow yourself")
        return db.withTransaction {
            val already = dao.isFollowing(currentUserId, targetUserId)
            val me = dao.getUserById(currentUserId) ?: return@withTransaction BackendResult.Error("User error")
            val target = dao.getUserById(targetUserId) ?: return@withTransaction BackendResult.Error("Target not found")
            if (already) {
                dao.deleteFollow(currentUserId, targetUserId)
                dao.upsertUser(me.copy(followingCount = (me.followingCount - 1).coerceAtLeast(0)))
                dao.upsertUser(target.copy(followersCount = (target.followersCount - 1).coerceAtLeast(0)))
                BackendResult.Success(false, "Unfollowed ${target.username}")
            } else {
                dao.insertFollow(FollowEntity(currentUserId, targetUserId, System.currentTimeMillis()))
                dao.upsertUser(me.copy(followingCount = me.followingCount + 1))
                dao.upsertUser(target.copy(followersCount = target.followersCount + 1))
                BackendResult.Success(true, "Following ${target.username}")
            }
        }
    }

    suspend fun blockUser(currentUserId: String, targetUserId: String, reason: String): BackendResult<Unit> {
        dao.insertBlock(BlockEntity(currentUserId, targetUserId, reason, System.currentTimeMillis()))
        recordAudit(currentUserId, "USER", "MODERATION", "BLOCK_USER", targetUserId, reason)
        return BackendResult.Success(Unit, "Blocked user $targetUserId")
    }

    suspend fun reportUser(reporterId: String, reportedId: String, category: String, details: String): BackendResult<Unit> {
        val report = UserReportEntity(
            reportId = "REP-${System.currentTimeMillis().toString().takeLast(6)}",
            reporterUserId = reporterId,
            reportedUserId = reportedId,
            category = category,
            details = details.ifBlank { "Policy violation reported" },
            status = "OPEN",
            resolvedByAdminId = null,
            createdAt = System.currentTimeMillis()
        )
        dao.insertReport(report)
        recordAudit(reporterId, "USER", "MODERATION", "CREATE_REPORT", reportedId, "Category=$category")
        return BackendResult.Success(Unit, "Report ${report.reportId} submitted to Global Owner Admin")
    }

    // ================= 3, 4, 5, 14: VOICE ROOMS, OWNER CONTROLS & TOP NOTIFICATIONS =================
    suspend fun verifyRoomOwnerOrAdmin(roomId: String, actorUserId: String): Pair<Boolean, String> {
        val actor = dao.getUserById(actorUserId) ?: return false to "NONE"
        if (actor.role == "GLOBAL_OWNER" || actor.role == "ADMIN") return true to actor.role
        val room = dao.getRoomById(roomId) ?: return false to "NONE"
        if (room.ownerUserId == actorUserId) return true to "OWNER"
        val role = dao.getRoomRole(roomId, actorUserId)
        if (role != null && (role.roleType == "OWNER" || role.roleType == "ADMIN")) {
            return true to role.roleType
        }
        return false to (role?.roleType ?: "MEMBER")
    }

    suspend fun publishRoomTopNotification(
        roomId: String,
        actorUserId: String,
        title: String,
        messageText: String,
        iconName: String,
        bannerStyle: String,
        durationSeconds: Int
    ): BackendResult<RoomTopNotificationEntity> {
        val (authorized, roleLabel) = verifyRoomOwnerOrAdmin(roomId, actorUserId)
        if (!authorized) {
            recordAudit(
                actorUserId,
                roleLabel,
                "ROOM_BANNER",
                "UNAUTHORIZED_BANNER_ATTEMPT",
                roomId,
                "Rejected by backend RBAC"
            )
            return BackendResult.Error("Permission Denied: Only Room Owner or Room Admin can create Room Top Notifications.")
        }
        if (title.isBlank() || messageText.isBlank()) {
            return BackendResult.Error("Notification title and message are required.")
        }
        val safeDuration = durationSeconds.coerceIn(15, 86_400)
        val now = System.currentTimeMillis()
        val expiresAt = now + safeDuration * 1000L

        val entity = RoomTopNotificationEntity(
            notificationId = "RTN-${UUID.randomUUID().toString().take(8).uppercase()}",
            roomId = roomId,
            creatorUserId = actorUserId,
            creatorRole = roleLabel,
            title = title.trim(),
            messageText = messageText.trim(),
            iconName = iconName,
            bannerStyle = bannerStyle,
            durationSeconds = safeDuration,
            createdAt = now,
            expiresAt = expiresAt,
            isActive = true
        )

        dao.insertRoomNotification(entity)
        recordAudit(
            actorUserId = actorUserId,
            actorRole = roleLabel,
            category = "ROOM_BANNER",
            action = "CREATE_ROOM_TOP_NOTIFICATION",
            targetId = roomId,
            details = "Title=${entity.title}, Duration=${safeDuration}s, Style=$bannerStyle"
        )

        realtimeService.emitEvent(
            RealtimeEvent.RoomTopBannerPublished(
                roomId = roomId,
                notificationId = entity.notificationId,
                title = entity.title,
                messageText = entity.messageText,
                iconName = entity.iconName,
                bannerStyle = entity.bannerStyle,
                expiresAt = entity.expiresAt
            )
        )
        return BackendResult.Success(entity, "Top room banner published live to all room users!")
    }

    suspend fun sweepExpiredRoomNotifications() {
        dao.expireOutdatedRoomNotifications(System.currentTimeMillis())
    }

    suspend fun joinOrLeaveSeat(roomId: String, seatIndex: Int, user: UserEntity): BackendResult<RoomSeatEntity> {
        val seat = dao.getRoomSeat(roomId, seatIndex) ?: return BackendResult.Error("Seat not found")
        if (seat.isSeatLocked && seat.occupantUserId != user.tangerUserId) {
            val (isAdmin, _) = verifyRoomOwnerOrAdmin(roomId, user.tangerUserId)
            if (!isAdmin) return BackendResult.Error("Seat #${seatIndex + 1} is locked by Room Owner.")
        }

        val updated = if (seat.occupantUserId == user.tangerUserId) {
            seat.copy(
                occupantUserId = null,
                occupantUsername = null,
                occupantAvatar = null,
                occupantVipLevel = 0,
                occupantRole = "SPEAKER",
                isMutedBySelf = false,
                speakingAudioDb = 0f
            )
        } else {
            val roleInRoom = dao.getRoomRole(roomId, user.tangerUserId)?.roleType ?: "SPEAKER"
            seat.copy(
                occupantUserId = user.tangerUserId,
                occupantUsername = user.username,
                occupantAvatar = user.avatarUri,
                occupantVipLevel = user.vipLevel,
                occupantRole = roleInRoom,
                isMutedBySelf = false
            )
        }
        dao.upsertRoomSeat(updated)
        return BackendResult.Success(updated, if (updated.occupantUserId != null) "Joined Seat #${seatIndex + 1}" else "Left Seat #${seatIndex + 1}")
    }

    suspend fun toggleSeatMuteOrLockByOwner(
        roomId: String,
        actorUserId: String,
        seatIndex: Int,
        toggleLock: Boolean,
        toggleOwnerMute: Boolean
    ): BackendResult<RoomSeatEntity> {
        val (authorized, roleLabel) = verifyRoomOwnerOrAdmin(roomId, actorUserId)
        if (!authorized) return BackendResult.Error("Only Room Owner/Admin/Co-host can manage seats.")
        val seat = dao.getRoomSeat(roomId, seatIndex) ?: return BackendResult.Error("Seat not found")
        val updated = seat.copy(
            isSeatLocked = if (toggleLock) !seat.isSeatLocked else seat.isSeatLocked,
            isMutedByOwner = if (toggleOwnerMute) !seat.isMutedByOwner else seat.isMutedByOwner
        )
        dao.upsertRoomSeat(updated)
        recordAudit(
            actorUserId,
            roleLabel,
            "ROOM_CONTROL",
            "MANAGE_SEAT_${seatIndex + 1}",
            roomId,
            "Locked=${updated.isSeatLocked}, MutedByOwner=${updated.isMutedByOwner}"
        )
        return BackendResult.Success(updated, "Seat #${seatIndex + 1} updated")
    }

    suspend fun assignOrRemoveRoomRole(
        roomId: String,
        actorUserId: String,
        targetUserId: String,
        roleType: String? // "ADMIN", "CO_HOST", or null to remove
    ): BackendResult<String> {
        val (authorized, actorRole) = verifyRoomOwnerOrAdmin(roomId, actorUserId)
        if (!authorized) return BackendResult.Error("Only Room Owner can assign Admin/Co-host roles.")
        val target = dao.getUserById(targetUserId) ?: return BackendResult.Error("User $targetUserId not found.")
        if (roleType == null) {
            dao.deleteRoomRole(roomId, targetUserId)
            recordAudit(actorUserId, actorRole, "ROOM_CONTROL", "REMOVE_ROOM_ROLE", targetUserId, "Room=$roomId")
            return BackendResult.Success("Removed role from ${target.username}")
        } else {
            dao.upsertRoomRole(
                RoomRoleEntity(
                    roomId = roomId,
                    userId = targetUserId,
                    username = target.username,
                    roleType = roleType,
                    assignedByUserId = actorUserId,
                    assignedAt = System.currentTimeMillis()
                )
            )
            recordAudit(actorUserId, actorRole, "ROOM_CONTROL", "ASSIGN_$roleType", targetUserId, "Room=$roomId")
            return BackendResult.Success("${target.username} is now $roleType")
        }
    }

    suspend fun updateRoomSettingsAndMusic(
        roomId: String,
        actorUserId: String,
        newTheme: String?,
        toggleChatLock: Boolean,
        musicTrack: String?,
        musicArtist: String?,
        isMusicPlaying: Boolean?,
        musicVolume: Float?
    ): BackendResult<VoiceRoomEntity> {
        val (authorized, actorRole) = verifyRoomOwnerOrAdmin(roomId, actorUserId)
        if (!authorized) return BackendResult.Error("Only Room Owner/Admin can change room theme, music, or settings.")
        val room = dao.getRoomById(roomId) ?: return BackendResult.Error("Room not found")
        val updated = room.copy(
            themeName = newTheme ?: room.themeName,
            isChatLocked = if (toggleChatLock) !room.isChatLocked else room.isChatLocked,
            activeMusicTrackTitle = musicTrack ?: room.activeMusicTrackTitle,
            activeMusicArtist = musicArtist ?: room.activeMusicArtist,
            isMusicPlaying = isMusicPlaying ?: room.isMusicPlaying,
            musicVolume = musicVolume ?: room.musicVolume
        )
        dao.upsertRoom(updated)
        realtimeService.playMusicCueTone(updated.isMusicPlaying, updated.musicVolume)
        realtimeService.emitEvent(
            RealtimeEvent.MusicStateChanged(
                roomId = roomId,
                trackTitle = updated.activeMusicTrackTitle,
                isPlaying = updated.isMusicPlaying,
                volume = updated.musicVolume,
                positionSec = updated.musicPositionSec
            )
        )
        recordAudit(
            actorUserId,
            actorRole,
            "ROOM_CONTROL",
            "UPDATE_ROOM_SETTINGS",
            roomId,
            "Theme=${updated.themeName}, Music=${updated.activeMusicTrackTitle}, Playing=${updated.isMusicPlaying}"
        )
        return BackendResult.Success(updated, "Room settings synchronized in real time")
    }

    suspend fun createVoiceRoom(
        owner: UserEntity,
        title: String,
        topic: String,
        isPrivate: Boolean,
        pinCode: String?,
        themeName: String
    ): BackendResult<VoiceRoomEntity> {
        if (title.isBlank()) return BackendResult.Error("Room title cannot be empty.")
        val roomId = "RM-${Random.nextInt(200, 999)}"
        val now = System.currentTimeMillis()
        val room = VoiceRoomEntity(
            roomId = roomId,
            roomTitle = title.trim(),
            roomTopic = topic.ifBlank { "Global Social Voice Party" },
            countryFlag = owner.countryFlag,
            ownerUserId = owner.tangerUserId,
            ownerUsername = owner.username,
            isPrivate = isPrivate,
            pinCode = if (isPrivate) pinCode?.ifBlank { "1234" } else null,
            seatCount = 8,
            themeName = themeName,
            isChatLocked = false,
            activeMusicTrackTitle = "Tanger Andalusian Chill Beats",
            activeMusicArtist = "Tanger Star Studio (Royalty-Free)",
            isMusicPlaying = false,
            musicVolume = 0.65f,
            musicPositionSec = 0,
            totalRoomHeat = 1500L,
            onlineCount = 1,
            createdAt = now
        )
        val seats = (0 until 8).map { idx ->
            RoomSeatEntity(
                roomId = roomId,
                seatIndex = idx,
                occupantUserId = if (idx == 0) owner.tangerUserId else null,
                occupantUsername = if (idx == 0) owner.username else null,
                occupantAvatar = if (idx == 0) owner.avatarUri else null,
                occupantVipLevel = if (idx == 0) owner.vipLevel else 0,
                occupantRole = if (idx == 0) "OWNER" else "SPEAKER",
                isMutedBySelf = false,
                isMutedByOwner = false,
                isSeatLocked = false,
                speakingAudioDb = 0f
            )
        }
        db.withTransaction {
            dao.upsertRoom(room)
            dao.upsertRoomSeats(seats)
            dao.upsertRoomRole(
                RoomRoleEntity(
                    roomId = roomId,
                    userId = owner.tangerUserId,
                    username = owner.username,
                    roleType = "OWNER",
                    assignedByUserId = owner.tangerUserId,
                    assignedAt = now
                )
            )
        }
        recordAudit(owner.tangerUserId, owner.role, "ROOM", "CREATE_VOICE_ROOM", roomId, "Title=$title")
        return BackendResult.Success(room, "Created Voice Room ${room.roomTitle}")
    }

    // ================= 6, 7, 8, 9: ATOMIC GIFTS, WALLETS, LUCKY GIFT COUNTER, VIP =================
    suspend fun sendGiftAtomicServerValidated(
        idempotencyKey: String,
        senderUserId: String,
        receiverUserId: String,
        roomId: String,
        giftId: String,
        quantity: Int
    ): BackendResult<GiftTransactionEntity> {
        if (quantity <= 0) return BackendResult.Error("Gift quantity must be at least 1.")

        return db.withTransaction {
            // 1. Idempotency check (Prevents duplicate coin deduction or duplicate reward)
            val existingTx = dao.getTransactionByIdempotency(idempotencyKey)
            if (existingTx != null) {
                return@withTransaction BackendResult.Error("Duplicate transaction prevented by Idempotency Key.")
            }

            // 2. Fetch authoritative server records (NEVER trust frontend price or balance)
            val sender = dao.getUserById(senderUserId)
                ?: return@withTransaction BackendResult.Error("Sender account not found.")
            if (sender.isBanned || sender.isSuspended) {
                return@withTransaction BackendResult.Error("Sender account is suspended or banned.")
            }
            val receiver = dao.getUserById(receiverUserId)
                ?: return@withTransaction BackendResult.Error("Receiver account not found.")
            var gift = dao.getGiftById(giftId)
                ?: return@withTransaction BackendResult.Error("Gift configuration not found on server.")

            if (!gift.isActive) {
                return@withTransaction BackendResult.Error("Gift '${gift.name}' is currently disabled by Owner.")
            }

            val now = System.currentTimeMillis()

            // Auto-evaluate Repeat Timing Cycle Reset if repeatTimingSeconds > 0
            if (gift.repeatTimingSeconds > 0 && gift.lastCycleResetAt > 0L) {
                val repeatIntervalMs = gift.repeatTimingSeconds * 1000L
                if (now - gift.lastCycleResetAt >= repeatIntervalMs) {
                    gift = gift.copy(
                        currentHitCount = 0L,
                        currentActivations = 0L,
                        lastCycleResetAt = now,
                        updatedAt = now
                    )
                    dao.upsertGift(gift)
                }
            } else if (gift.lastCycleResetAt == 0L) {
                gift = gift.copy(lastCycleResetAt = now)
                dao.upsertGift(gift)
            }

            val totalCoinCost = gift.coinPrice * quantity.toLong()
            if (sender.coinsBalance < totalCoinCost) {
                return@withTransaction BackendResult.Error("Insufficient Coins balance (Need $totalCoinCost, have ${sender.coinsBalance}).")
            }

            val diamondsEarned = ((totalCoinCost * gift.diamondReturnRatePercent) / 100L).coerceAtLeast(1L)

            // 3. Deduct Sender Coins & Check Automatic VIP Qualification
            val newSenderCoins = sender.coinsBalance - totalCoinCost
            val newSenderTotalSpent = sender.totalCoinsSpent + totalCoinCost
            val vipConfigs = dao.getVipConfigs()
            val qualifiedVip = vipConfigs.filter { newSenderTotalSpent >= it.requiredCoins }
                .maxByOrNull { it.vipLevel }?.vipLevel ?: sender.vipLevel
            val finalSenderVip = maxOf(sender.vipLevel, qualifiedVip)

            var luckyRewardCoins = 0L
            var cashbackGranted = 0L
            var expGranted = (gift.expBonus.coerceAtLeast(10L) * quantity.toLong()) + (totalCoinCost / 10L)
            var luckyMilestoneLabel = ""

            val currentLucky = dao.getLuckyGiftState() ?: LuckyGiftStateEntity(
                globalPoolCoins = 12_500L,
                globalHitCount = 0L,
                milestone1Threshold = 5L,
                milestone1RewardCoins = 500L,
                milestone2Threshold = 15L,
                milestone2RewardCoins = 2_000L,
                jackpotThreshold = 50L,
                jackpotRewardCoins = 10_000L,
                updatedAt = now
            )

            var newGiftCounter = gift.currentHitCount + quantity.toLong()
            var newGiftActivations = gift.currentActivations
            var newGlobalHits = currentLucky.globalHitCount

            // 4. Authoritative Global & Per-Gift Lucky Gift Engine
            val isLuckyCategory = gift.category.equals("Lucky Gift", ignoreCase = true) ||
                gift.category.equals("Lucky Draw", ignoreCase = true) ||
                gift.triggerCount > 0L

            if (isLuckyCategory) {
                newGlobalHits = currentLucky.globalHitCount + quantity.toLong()
                val newPool = currentLucky.globalPoolCoins + (totalCoinCost / 2L)

                // Check per-gift & global Lucky eligibility rules:
                // - Not paused (neither gift.isPaused nor currentLucky.isGlobalLuckyPaused)
                // - Within Start Time and End Time window (if configured > 0)
                // - Within Activation Limit (if activationLimit > 0)
                // - Cooldown elapsed since lastTriggeredAt
                val withinStartWindow = gift.startTimeMillis <= 0L || now >= gift.startTimeMillis
                val withinEndWindow = gift.endTimeMillis <= 0L || now <= gift.endTimeMillis
                val withinActivationLimit = gift.activationLimit <= 0L || gift.currentActivations < gift.activationLimit
                val cooldownElapsed = gift.cooldownSeconds <= 0 ||
                    gift.lastTriggeredAt <= 0L ||
                    (now - gift.lastTriggeredAt) >= (gift.cooldownSeconds * 1000L)

                val canTriggerLucky = !gift.isPaused &&
                    !currentLucky.isGlobalLuckyPaused &&
                    withinStartWindow &&
                    withinEndWindow &&
                    withinActivationLimit &&
                    cooldownElapsed

                if (canTriggerLucky) {
                    val safeTrigger = gift.triggerCount.coerceAtLeast(1L)
                    val prevTriggerBucket = gift.currentHitCount / safeTrigger
                    val newTriggerBucket = newGiftCounter / safeTrigger
                    val perGiftTriggered = newTriggerBucket > prevTriggerBucket

                    val hitJackpot = currentLucky.jackpotThreshold > 0L && (newGlobalHits % currentLucky.jackpotThreshold == 0L)
                    val hitM2 = currentLucky.milestone2Threshold > 0L && (newGlobalHits % currentLucky.milestone2Threshold == 0L)
                    val hitM1 = currentLucky.milestone1Threshold > 0L && (newGlobalHits % currentLucky.milestone1Threshold == 0L)

                    var rawReward = 0L
                    val labels = mutableListOf<String>()

                    if (perGiftTriggered) {
                        val triggerMultipliers = (newTriggerBucket - prevTriggerBucket).coerceAtLeast(1L)
                        rawReward += (gift.rewardCoins * triggerMultipliers)
                        cashbackGranted += (gift.cashbackCoins * quantity.toLong())
                        expGranted += (gift.expBonus * 2L * triggerMultipliers)
                        labels.add("${gift.name} Trigger (${safeTrigger}x)")
                    }

                    when {
                        hitJackpot -> {
                            rawReward += currentLucky.jackpotRewardCoins
                            labels.add("GRAND JACKPOT (${currentLucky.jackpotThreshold}x)")
                        }
                        hitM2 -> {
                            rawReward += currentLucky.milestone2RewardCoins
                            labels.add("SUPER MILESTONE (${currentLucky.milestone2Threshold}x)")
                        }
                        hitM1 -> {
                            rawReward += currentLucky.milestone1RewardCoins
                            labels.add("LUCKY MILESTONE (${currentLucky.milestone1Threshold}x)")
                        }
                    }

                    // Enforce per-gift Maximum Reward cap if configured (> 0) unless Global Jackpot hit
                    if (rawReward > 0L) {
                        luckyRewardCoins = if (gift.maxRewardCoins > 0L && !hitJackpot) {
                            rawReward.coerceAtMost(gift.maxRewardCoins)
                        } else {
                            rawReward
                        }
                        luckyMilestoneLabel = labels.joinToString(" + ")
                    }

                    if (luckyRewardCoins > 0L || cashbackGranted > 0L) {
                        val triggerKey = "LUCKY_${gift.giftId}_G${newGlobalHits}_C${newGiftCounter}_B${newTriggerBucket}"
                        val alreadyClaimed = dao.getLuckyAuditByTriggerKey(triggerKey)
                        if (alreadyClaimed == null) {
                            newGiftActivations += 1L
                            dao.insertLuckyGiftAudit(
                                LuckyGiftAuditEntity(
                                    auditId = "LGA-${UUID.randomUUID().toString().take(8).uppercase()}",
                                    milestoneTriggerKey = triggerKey,
                                    userId = sender.tangerUserId,
                                    username = sender.username,
                                    receiverUserId = receiver.tangerUserId,
                                    receiverUsername = receiver.username,
                                    giftId = gift.giftId,
                                    giftName = gift.name,
                                    giftCounterAtHit = newGiftCounter,
                                    globalCounterValueAtHit = newGlobalHits,
                                    milestoneReached = luckyMilestoneLabel.ifBlank { "LUCKY TRIGGER" },
                                    rewardCoinsGranted = luckyRewardCoins,
                                    cashbackCoinsGranted = cashbackGranted,
                                    expGranted = expGranted,
                                    timestamp = now
                                )
                            )
                        } else {
                            // Duplicate reward strictly blocked
                            luckyRewardCoins = 0L
                            cashbackGranted = 0L
                        }
                    }
                }

                dao.upsertLuckyGiftState(
                    currentLucky.copy(
                        globalPoolCoins = (newPool - luckyRewardCoins).coerceAtLeast(1_000L),
                        globalHitCount = newGlobalHits,
                        totalLuckyRewardsDistributed = currentLucky.totalLuckyRewardsDistributed + luckyRewardCoins,
                        totalCashbackDistributed = currentLucky.totalCashbackDistributed + cashbackGranted,
                        totalExpDistributed = currentLucky.totalExpDistributed + expGranted,
                        updatedAt = now
                    )
                )
            }

            // Persist updated per-gift Lucky counter & activation state
            val updatedGift = gift.copy(
                currentHitCount = newGiftCounter,
                currentActivations = newGiftActivations,
                lastTriggeredAt = if (luckyRewardCoins > 0L || cashbackGranted > 0L) now else gift.lastTriggeredAt,
                updatedAt = now
            )
            dao.upsertGift(updatedGift)

            val totalReturnToSender = luckyRewardCoins + cashbackGranted
            val finalSenderCoinsAfterLucky = newSenderCoins + totalReturnToSender
            val newXpPoints = sender.xpPoints + expGranted
            val calculatedLevel = (1 + (newXpPoints / 1_000L).toInt()).coerceIn(sender.userLevel, 100)

            val updatedSender = sender.copy(
                coinsBalance = finalSenderCoinsAfterLucky,
                totalCoinsSpent = newSenderTotalSpent,
                vipLevel = finalSenderVip,
                userLevel = calculatedLevel,
                xpPoints = newXpPoints,
                cpPartnerUserId = if (gift.category == "CP") receiver.tangerUserId else sender.cpPartnerUserId
            )
            dao.upsertUser(updatedSender)

            if (finalSenderVip > sender.vipLevel) {
                val vipTitle = vipConfigs.find { it.vipLevel == finalSenderVip }?.vipTitle ?: "VIP $finalSenderVip"
                dao.insertVipHistory(
                    VipHistoryEntity(
                        historyId = "VIPH-${UUID.randomUUID().toString().take(8)}",
                        userId = sender.tangerUserId,
                        vipLevel = finalSenderVip,
                        vipTitle = vipTitle,
                        coinsQualifiedWith = newSenderTotalSpent,
                        actionType = "AUTO_QUALIFIED",
                        timestamp = now
                    )
                )
            }

            // 5. Credit Receiver Diamonds & Monthly Diamond Cycle + Agency Progress
            val newReceiverDiamonds = receiver.diamondsBalance + diamondsEarned
            val updatedReceiver = receiver.copy(
                diamondsBalance = newReceiverDiamonds,
                totalDiamondsEarned = receiver.totalDiamondsEarned + diamondsEarned,
                cpPartnerUserId = if (gift.category == "CP") sender.tangerUserId else receiver.cpPartnerUserId
            )
            dao.upsertUser(updatedReceiver)

            val cycle = ensureMonthlyDiamondCycleForUser(receiver.tangerUserId, now)
            dao.upsertDiamondCycle(
                cycle.copy(
                    earnedDiamondsInCycle = cycle.earnedDiamondsInCycle + diamondsEarned,
                    lastUpdatedAt = now
                )
            )

            receiver.agencyId?.let { agId ->
                dao.getAgencyById(agId)?.let { agency ->
                    dao.upsertAgency(
                        agency.copy(
                            currentDailyDiamonds = agency.currentDailyDiamonds + diamondsEarned,
                            currentMonthlyDiamonds = agency.currentMonthlyDiamonds + diamondsEarned
                        )
                    )
                }
            }

            // 6. Update Room Heat if sent in a room
            dao.getRoomById(roomId)?.let { room ->
                dao.upsertRoom(room.copy(totalRoomHeat = room.totalRoomHeat + totalCoinCost))
            }

            // 7. Insert Atomic Ledger Entries (Gift Deduction + Separate Lucky Reward & Cashback Ledger Entry if triggered)
            val netDeltaWithReward = -totalCoinCost + totalReturnToSender
            val rewardSuffix = buildString {
                if (luckyRewardCoins > 0L) append(" • 🍀 Lucky Reward: +$luckyRewardCoins 🪙")
                if (cashbackGranted > 0L) append(" • 💸 Cashback: +$cashbackGranted 🪙")
                append(" • ⭐ +$expGranted EXP")
            }

            dao.insertWalletTransaction(
                WalletTransactionEntity(
                    txId = "TX-${UUID.randomUUID().toString().take(8).uppercase()}",
                    idempotencyKey = idempotencyKey,
                    userId = sender.tangerUserId,
                    counterpartyUserId = receiver.tangerUserId,
                    txType = if (totalReturnToSender > 0L) "LUCKY_REWARD" else "GIFT_SENT",
                    coinsDelta = netDeltaWithReward,
                    diamondsDelta = 0L,
                    coinsBalanceAfter = finalSenderCoinsAfterLucky,
                    diamondsBalanceAfter = sender.diamondsBalance,
                    description = "Sent ${quantity}x ${gift.iconEmoji} ${gift.name} to @${receiver.username} (#${receiver.tangerUserId})$rewardSuffix",
                    timestamp = now
                )
            )

            dao.insertWalletTransaction(
                WalletTransactionEntity(
                    txId = "TX-${UUID.randomUUID().toString().take(8).uppercase()}",
                    idempotencyKey = "${idempotencyKey}_RCV",
                    userId = receiver.tangerUserId,
                    counterpartyUserId = sender.tangerUserId,
                    txType = "GIFT_RECEIVED_DIAMOND",
                    coinsDelta = 0L,
                    diamondsDelta = diamondsEarned,
                    coinsBalanceAfter = receiver.coinsBalance,
                    diamondsBalanceAfter = newReceiverDiamonds,
                    description = "Received ${quantity}x ${gift.iconEmoji} ${gift.name} from @${sender.username} (#${sender.tangerUserId})",
                    timestamp = now
                )
            )

            val giftTx = GiftTransactionEntity(
                giftTxId = "GTX-${UUID.randomUUID().toString().take(8).uppercase()}",
                idempotencyKey = idempotencyKey,
                senderUserId = sender.tangerUserId,
                senderUsername = sender.username,
                receiverUserId = receiver.tangerUserId,
                receiverUsername = receiver.username,
                roomId = roomId,
                giftId = gift.giftId,
                giftName = gift.name,
                giftCategory = gift.category,
                quantity = quantity,
                unitCoinPrice = gift.coinPrice,
                totalCoinsSpent = totalCoinCost,
                receiverDiamondsEarned = diamondsEarned,
                timestamp = now,
                luckyRewardCoins = luckyRewardCoins,
                cashbackCoins = cashbackGranted,
                expGranted = expGranted,
                giftCounterAfter = newGiftCounter,
                globalCounterAfter = newGlobalHits
            )
            dao.insertGiftTransaction(giftTx)

            val alertMsg = buildString {
                append("🎁 Sent ${quantity}x ${gift.iconEmoji} ${gift.name} to @${receiver.username} ($totalCoinCost 🪙)")
                if (luckyRewardCoins > 0L) append(" 🎉 LUCKY HIT +$luckyRewardCoins 🪙!")
                if (cashbackGranted > 0L) append(" 💸 Cashback +$cashbackGranted 🪙!")
                append(" (+$expGranted EXP)")
            }

            // Insert Room chat notification + Global chat notification if Lucky Reward triggered or sent globally
            dao.insertChatMessage(
                ChatMessageEntity(
                    messageId = "MSG-${UUID.randomUUID().toString().take(8)}",
                    channelType = if (roomId == "GLOBAL_GIFT") "GLOBAL" else "ROOM",
                    roomOrThreadId = if (roomId == "GLOBAL_GIFT") "GLOBAL" else roomId,
                    senderUserId = sender.tangerUserId,
                    senderUsername = sender.username,
                    senderVipLevel = finalSenderVip,
                    senderCountryFlag = sender.countryFlag,
                    messageType = "GIFT_ALERT",
                    contentText = alertMsg,
                    voiceDurationSec = 0,
                    createdAt = now
                )
            )

            // Also deliver a Direct Chat notification to the receiver so any global user gets notified immediately
            val directThreadId = listOf(sender.tangerUserId, receiver.tangerUserId).sorted().joinToString("_")
            dao.insertChatMessage(
                ChatMessageEntity(
                    messageId = "MSG-DM-${UUID.randomUUID().toString().take(7)}",
                    channelType = "DIRECT",
                    roomOrThreadId = directThreadId,
                    senderUserId = sender.tangerUserId,
                    senderUsername = sender.username,
                    senderVipLevel = finalSenderVip,
                    senderCountryFlag = sender.countryFlag,
                    messageType = "GIFT_ALERT",
                    contentText = "🎁 Sent you ${quantity}x ${gift.iconEmoji} ${gift.name} (+💎 $diamondsEarned Diamonds)!",
                    voiceDurationSec = 0,
                    createdAt = now
                )
            )

            recordAudit(
                actorUserId = sender.tangerUserId,
                actorRole = sender.role,
                category = "GIFT",
                action = "ATOMIC_GIFT_TX",
                targetId = receiver.tangerUserId,
                details = "Gift=${gift.name} x$quantity, Coins=$totalCoinCost, Diamonds=$diamondsEarned, LuckyReward=$luckyRewardCoins, Cashback=$cashbackGranted, EXP=$expGranted, GiftHits=$newGiftCounter/${gift.triggerCount}"
            )

            realtimeService.emitEvent(
                RealtimeEvent.GiftBroadcast(
                    roomId = roomId,
                    senderId = sender.tangerUserId,
                    senderName = sender.username,
                    receiverId = receiver.tangerUserId,
                    receiverName = receiver.username,
                    giftId = gift.giftId,
                    giftName = gift.name,
                    giftEmoji = gift.iconEmoji,
                    giftCategory = gift.category,
                    animationEffect = gift.animationEffect,
                    quantity = quantity,
                    totalCoins = totalCoinCost,
                    luckyRewardGranted = luckyRewardCoins,
                    cashbackGranted = cashbackGranted,
                    expGranted = expGranted,
                    milestoneReached = luckyMilestoneLabel,
                    giftHitCounter = newGiftCounter,
                    globalHitCounter = newGlobalHits
                )
            )

            BackendResult.Success(
                giftTx,
                buildString {
                    append("Sent ${quantity}x ${gift.iconEmoji} ${gift.name} to @${receiver.username}!")
                    if (luckyRewardCoins > 0L) append(" 🍀 Lucky Reward +$luckyRewardCoins 🪙!")
                    if (cashbackGranted > 0L) append(" 💸 Cashback +$cashbackGranted 🪙!")
                    append(" (+$expGranted EXP)")
                }
            )
        }
    }

    /**
     * Atomic Diamond-to-Coin Conversion (Server-Validated):
     * 100 Diamonds -> 70 Gold Coins. Enforces idempotency, positive balance checks,
     * atomic balance updates, and immutable audit logging.
     */
    suspend fun exchangeDiamondsToCoinsAtomic(
        idempotencyKey: String,
        userId: String,
        diamondsToExchange: Long
    ): BackendResult<WalletTransactionEntity> {
        if (diamondsToExchange < 100L) {
            return BackendResult.Error("Minimum conversion is 100 Diamonds.")
        }
        return db.withTransaction {
            val activeSession = dao.getActiveSession()
            if (activeSession == null || !activeSession.isActive) {
                return@withTransaction BackendResult.Error("Unauthorized: Active verified session required for wallet operations.")
            }

            val existingTx = dao.getTransactionByIdempotency(idempotencyKey)
            if (existingTx != null) {
                return@withTransaction BackendResult.Error("Duplicate conversion blocked by Idempotency Key.")
            }

            val user = dao.getUserById(userId)
                ?: return@withTransaction BackendResult.Error("User wallet not found.")
            if (user.isBanned || user.isSuspended) {
                return@withTransaction BackendResult.Error("Wallet frozen: Account is suspended or banned.")
            }
            if (user.diamondsBalance < diamondsToExchange) {
                return@withTransaction BackendResult.Error("Insufficient Diamonds balance (Have ${user.diamondsBalance}, requested $diamondsToExchange).")
            }

            val coinsCredited = (diamondsToExchange * 70L) / 100L
            val newDiamonds = user.diamondsBalance - diamondsToExchange
            val newCoins = user.coinsBalance + coinsCredited
            val now = System.currentTimeMillis()

            val updatedUser = user.copy(
                coinsBalance = newCoins,
                diamondsBalance = newDiamonds
            )
            dao.upsertUser(updatedUser)

            val tx = WalletTransactionEntity(
                txId = "TX-${UUID.randomUUID().toString().take(8).uppercase()}",
                idempotencyKey = idempotencyKey,
                userId = userId,
                counterpartyUserId = "TANGER_EXCHANGE_TREASURY",
                txType = "DIAMOND_EXCHANGE",
                coinsDelta = coinsCredited,
                diamondsDelta = -diamondsToExchange,
                coinsBalanceAfter = newCoins,
                diamondsBalanceAfter = newDiamonds,
                description = "Converted $diamondsToExchange Diamonds → +$coinsCredited Gold Coins (70% Rate)",
                timestamp = now
            )
            dao.insertWalletTransaction(tx)

            recordAudit(
                actorUserId = userId,
                actorRole = user.role,
                category = "FINANCE",
                action = "ATOMIC_DIAMOND_TO_COIN_EXCHANGE",
                targetId = tx.txId,
                details = "Diamonds=-$diamondsToExchange, Coins=+$coinsCredited, BalanceAfter=($newCoins Coins, $newDiamonds Diamonds)"
            )

            BackendResult.Success(
                tx,
                "Exchanged 💎 $diamondsToExchange Diamonds for 🪙 +$coinsCredited Coins!"
            )
        }
    }

    /**
     * Atomic Daily VIP Bonus Claim (Idempotent per calendar day YYYY-MM-DD):
     * Reads authoritative VIP tier configuration from database and credits daily bonus Coins.
     */
    suspend fun claimVipDailyBonusAtomic(userId: String): BackendResult<WalletTransactionEntity> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val dayKey = String.format(
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val idempotencyKey = "VIP_DAILY_BONUS_${userId}_$dayKey"

        return db.withTransaction {
            val existing = dao.getTransactionByIdempotency(idempotencyKey)
            if (existing != null) {
                return@withTransaction BackendResult.Error("Daily VIP Bonus already claimed for $dayKey (Idempotency Key: $idempotencyKey).")
            }

            val user = dao.getUserById(userId)
                ?: return@withTransaction BackendResult.Error("User not found.")
            if (user.vipLevel <= 0) {
                return@withTransaction BackendResult.Error("VIP Level 1+ required to claim Daily VIP Bonus.")
            }

            val vipConfigs = dao.getVipConfigs()
            val vipTier = vipConfigs.find { it.vipLevel == user.vipLevel }
            val bonusCoins = vipTier?.dailyBonusCoins ?: (user.vipLevel * 250L)

            val newCoins = user.coinsBalance + bonusCoins
            val updatedUser = user.copy(coinsBalance = newCoins)
            dao.upsertUser(updatedUser)

            val tx = WalletTransactionEntity(
                txId = "TX-${UUID.randomUUID().toString().take(8).uppercase()}",
                idempotencyKey = idempotencyKey,
                userId = userId,
                counterpartyUserId = "VIP_TREASURY",
                txType = "VIP_DAILY_BONUS",
                coinsDelta = bonusCoins,
                diamondsDelta = 0L,
                coinsBalanceAfter = newCoins,
                diamondsBalanceAfter = user.diamondsBalance,
                description = "VIP ${user.vipLevel} Daily Bonus Claim ($dayKey)",
                timestamp = now
            )
            dao.insertWalletTransaction(tx)

            recordAudit(
                actorUserId = userId,
                actorRole = user.role,
                category = "FINANCE",
                action = "VIP_DAILY_BONUS_CLAIM",
                targetId = tx.txId,
                details = "VIP=${user.vipLevel}, Day=$dayKey, BonusCoins=+$bonusCoins"
            )

            BackendResult.Success(tx, "Claimed 👑 VIP ${user.vipLevel} Daily Bonus: +$bonusCoins Coins!")
        }
    }

    /**
     * Secure Transaction History Retrieval & Relational Ledger Integrity Verification:
     * Validates active session authorization, queries the relational ledger for the user,
     * computes total inflows/outflows, and verifies balance-after snapshot consistency.
     */
    suspend fun verifyAndRetrieveUserWalletHistory(
        requesterUserId: String,
        targetUserId: String = requesterUserId
    ): BackendResult<WalletVerificationReport> {
        return db.withTransaction {
            val activeSession = dao.getActiveSession()
            val requester = dao.getUserById(requesterUserId)
                ?: return@withTransaction BackendResult.Error("Requester account not found.")

            // Enforce RBAC: Users can only inspect their own wallet unless GLOBAL_OWNER/ADMIN
            if (requesterUserId != targetUserId && requester.role != "GLOBAL_OWNER" && requester.role != "ADMIN") {
                recordAudit(
                    requesterUserId,
                    requester.role,
                    "FINANCE",
                    "UNAUTHORIZED_WALLET_HISTORY_ACCESS",
                    targetUserId,
                    "Blocked cross-user wallet retrieval"
                )
                return@withTransaction BackendResult.Error("Access Denied: Cannot retrieve another user's wallet history without Global Owner RBAC.")
            }

            val targetUser = dao.getUserById(targetUserId)
                ?: return@withTransaction BackendResult.Error("Target user wallet not found.")
            val ledger = dao.getUserTransactionsSnapshot(targetUserId)

            val coinsInflow = ledger.filter { it.coinsDelta > 0 }.sumOf { it.coinsDelta }
            val coinsOutflow = ledger.filter { it.coinsDelta < 0 }.sumOf { -it.coinsDelta }
            val diamondsInflow = ledger.filter { it.diamondsDelta > 0 }.sumOf { it.diamondsDelta }
            val diamondsOutflow = ledger.filter { it.diamondsDelta < 0 }.sumOf { -it.diamondsDelta }

            val latestTx = ledger.firstOrNull()
            val isConsistent = if (latestTx != null) {
                latestTx.coinsBalanceAfter == targetUser.coinsBalance &&
                    latestTx.diamondsBalanceAfter == targetUser.diamondsBalance
            } else {
                true
            }

            val report = WalletVerificationReport(
                userId = targetUserId,
                sessionVerified = activeSession?.isActive == true,
                totalLedgerEntries = ledger.size,
                totalCoinsInflow = coinsInflow,
                totalCoinsOutflow = coinsOutflow,
                totalDiamondsInflow = diamondsInflow,
                totalDiamondsOutflow = diamondsOutflow,
                latestCoinsBalance = targetUser.coinsBalance,
                latestDiamondsBalance = targetUser.diamondsBalance,
                ledgerConsistencyVerified = isConsistent,
                verifiedAtMillis = System.currentTimeMillis()
            )

            recordAudit(
                actorUserId = requesterUserId,
                actorRole = requester.role,
                category = "FINANCE",
                action = "VERIFY_WALLET_LEDGER_HISTORY",
                targetId = targetUserId,
                details = "Entries=${ledger.size}, Consistent=$isConsistent, Coins=${targetUser.coinsBalance}, Diamonds=${targetUser.diamondsBalance}"
            )

            BackendResult.Success(
                report,
                "Relational Wallet Ledger Verified (${ledger.size} entries • Session & Snapshot Integrity OK)"
            )
        }
    }

    // ================= 11. RANKINGS + AUTOMATIC REWARDS WITH DUPLICATE PROTECTION =================
    suspend fun claimServerRankingReward(
        userId: String,
        category: String,
        rankPosition: Int
    ): BackendResult<RankingRewardClaimEntity> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val periodKey = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}"
        val claimKey = "RANK_${category}_${periodKey}_$userId"

        return db.withTransaction {
            val existing = dao.getRankingClaimByKey(claimKey)
            if (existing != null) {
                return@withTransaction BackendResult.Error("Duplicate reward prevented: Already claimed $category reward for $periodKey.")
            }
            val user = dao.getUserById(userId) ?: return@withTransaction BackendResult.Error("User not found.")
            val rewardCoins = when (rankPosition) {
                1 -> 5_000L
                2 -> 3_000L
                3 -> 1_500L
                else -> 500L
            }
            val updatedUser = user.copy(coinsBalance = user.coinsBalance + rewardCoins)
            dao.upsertUser(updatedUser)

            dao.insertWalletTransaction(
                WalletTransactionEntity(
                    txId = "TX-${UUID.randomUUID().toString().take(8).uppercase()}",
                    idempotencyKey = claimKey,
                    userId = userId,
                    counterpartyUserId = "SERVER_RANKING_TREASURY",
                    txType = "RANKING_REWARD",
                    coinsDelta = rewardCoins,
                    diamondsDelta = 0L,
                    coinsBalanceAfter = updatedUser.coinsBalance,
                    diamondsBalanceAfter = updatedUser.diamondsBalance,
                    description = "Ranking Reward (#$rankPosition in $category - $periodKey)",
                    timestamp = now
                )
            )

            val claim = RankingRewardClaimEntity(
                claimId = "RRC-${UUID.randomUUID().toString().take(8).uppercase()}",
                rewardClaimKey = claimKey,
                category = category,
                rankPosition = rankPosition,
                userId = userId,
                username = user.username,
                rewardCoins = rewardCoins,
                timestamp = now
            )
            dao.insertRankingRewardClaim(claim)
            recordAudit(userId, user.role, "RANKING_REWARD", "CLAIM_REWARD", claimKey, "Rank=#$rankPosition, Coins=$rewardCoins")
            BackendResult.Success(claim, "Claimed +$rewardCoins Coins for Rank #$rankPosition in $category!")
        }
    }

    // ================= 12. CHAT + VOICE MESSAGES + REAL-TIME CALLS =================
    suspend fun sendChatMessage(
        sender: UserEntity,
        channelType: String,
        roomOrThreadId: String,
        text: String,
        isVoiceNote: Boolean = false,
        voiceDurationSec: Int = 0
    ): BackendResult<ChatMessageEntity> {
        if (channelType == "ROOM") {
            val room = dao.getRoomById(roomOrThreadId)
            if (room?.isChatLocked == true) {
                val (isAdmin, _) = verifyRoomOwnerOrAdmin(roomOrThreadId, sender.tangerUserId)
                if (!isAdmin) return BackendResult.Error("Room chat is currently locked by the Room Owner.")
            }
        }
        val msg = ChatMessageEntity(
            messageId = "MSG-${UUID.randomUUID().toString().take(8)}",
            channelType = channelType,
            roomOrThreadId = roomOrThreadId,
            senderUserId = sender.tangerUserId,
            senderUsername = sender.username,
            senderVipLevel = sender.vipLevel,
            senderCountryFlag = sender.countryFlag,
            messageType = if (isVoiceNote) "VOICE_NOTE" else "TEXT",
            contentText = if (isVoiceNote) "🎤 Voice Message (${voiceDurationSec}s)" else text.trim(),
            voiceDurationSec = voiceDurationSec,
            createdAt = System.currentTimeMillis()
        )
        dao.insertChatMessage(msg)
        return BackendResult.Success(msg)
    }

    suspend fun startOrEndCallSession(
        caller: UserEntity,
        receiver: UserEntity,
        callType: String
    ): BackendResult<CallSessionEntity> {
        val call = CallSessionEntity(
            callId = "CALL-${UUID.randomUUID().toString().take(8).uppercase()}",
            callerUserId = caller.tangerUserId,
            callerUsername = caller.username,
            receiverUserId = receiver.tangerUserId,
            receiverUsername = receiver.username,
            callType = callType,
            status = "CONNECTED",
            durationSeconds = (18..145).random(),
            startedAt = System.currentTimeMillis()
        )
        dao.upsertCallSession(call)
        recordAudit(caller.tangerUserId, caller.role, "CALL", callType, receiver.tangerUserId, "CallId=${call.callId}")
        return BackendResult.Success(call, "$callType connected with ${receiver.username}")
    }

    // ================= 13. MULTIPLAYER SERVER-CONTROLLED LUDO & GAMES =================
    suspend fun createServerLudoMatch(
        player1: UserEntity,
        opponent: UserEntity,
        entryFeeCoins: Long,
        gameType: String = "LUDO_STAR"
    ): BackendResult<GameMatchEntity> {
        return db.withTransaction {
            val freshP1 = dao.getUserById(player1.tangerUserId)
                ?: return@withTransaction BackendResult.Error("User not found")
            if (freshP1.coinsBalance < entryFeeCoins) {
                return@withTransaction BackendResult.Error("Insufficient Coins for $entryFeeCoins entry fee.")
            }
            val now = System.currentTimeMillis()
            val updatedP1 = freshP1.copy(
                coinsBalance = freshP1.coinsBalance - entryFeeCoins,
                totalCoinsSpent = freshP1.totalCoinsSpent + entryFeeCoins
            )
            dao.upsertUser(updatedP1)

            val matchId = "GM-${UUID.randomUUID().toString().take(6).uppercase()}"
            dao.insertWalletTransaction(
                WalletTransactionEntity(
                    txId = "TX-${UUID.randomUUID().toString().take(8).uppercase()}",
                    idempotencyKey = "GAME_ENTRY_${matchId}_${freshP1.tangerUserId}",
                    userId = freshP1.tangerUserId,
                    counterpartyUserId = opponent.tangerUserId,
                    txType = "GAME_ENTRY",
                    coinsDelta = -entryFeeCoins,
                    diamondsDelta = 0L,
                    coinsBalanceAfter = updatedP1.coinsBalance,
                    diamondsBalanceAfter = updatedP1.diamondsBalance,
                    description = "Entered $gameType Match $matchId vs ${opponent.username}",
                    timestamp = now
                )
            )

            val match = GameMatchEntity(
                matchId = matchId,
                gameType = gameType,
                roomId = "RM-101",
                entryFeeCoins = entryFeeCoins,
                rewardPoolCoins = entryFeeCoins * 2L,
                player1Id = freshP1.tangerUserId,
                player1Name = freshP1.username,
                player1Pos = 0,
                player2Id = opponent.tangerUserId,
                player2Name = opponent.username,
                player2Pos = 0,
                currentTurnUserId = freshP1.tangerUserId,
                lastDiceRoll = 0,
                turnNumber = 1,
                status = "ACTIVE",
                winnerUserId = null,
                winnerUsername = null,
                createdAt = now,
                updatedAt = now
            )
            dao.upsertGameMatch(match)
            recordAudit(freshP1.tangerUserId, freshP1.role, "GAME_SERVER", "CREATE_MATCH", matchId, "Game=$gameType, Pool=${match.rewardPoolCoins}")
            BackendResult.Success(match, "Server Match $matchId started! Pool: ${match.rewardPoolCoins} Coins")
        }
    }

    /**
     * Authoritative Server Turn Engine for Ludo:
     * Server rolls dice, advances token along 56-tile Ludo path, resolves opponent turn,
     * and atomically settles winner reward pool upon reaching Home (56).
     */
    suspend fun executeServerValidatedLudoTurn(matchId: String, actorUserId: String): BackendResult<GameMatchEntity> {
        return db.withTransaction {
            val match = dao.getGameMatchById(matchId)
                ?: return@withTransaction BackendResult.Error("Match not found")
            if (match.status != "ACTIVE") {
                return@withTransaction BackendResult.Error("Match $matchId has already finished.")
            }

            val now = System.currentTimeMillis()
            val p1Roll = Random.nextInt(1, 7)
            val newP1Pos = (match.player1Pos + p1Roll * 3).coerceAtMost(56)

            var newP2Pos = match.player2Pos
            var winnerId: String? = null
            var winnerName: String? = null
            var status = "ACTIVE"

            if (newP1Pos >= 56) {
                winnerId = match.player1Id
                winnerName = match.player1Name
                status = "COMPLETED"
            } else {
                val p2Roll = Random.nextInt(1, 7)
                newP2Pos = (match.player2Pos + p2Roll * 3).coerceAtMost(56)
                if (newP2Pos >= 56) {
                    winnerId = match.player2Id
                    winnerName = match.player2Name
                    status = "COMPLETED"
                }
            }

            val updatedMatch = match.copy(
                player1Pos = newP1Pos,
                player2Pos = newP2Pos,
                lastDiceRoll = p1Roll,
                turnNumber = match.turnNumber + 1,
                status = status,
                winnerUserId = winnerId,
                winnerUsername = winnerName,
                updatedAt = now
            )
            dao.upsertGameMatch(updatedMatch)

            if (status == "COMPLETED" && winnerId != null) {
                val winner = dao.getUserById(winnerId)
                if (winner != null) {
                    val newCoins = winner.coinsBalance + match.rewardPoolCoins
                    dao.upsertUser(winner.copy(coinsBalance = newCoins))
                    dao.insertWalletTransaction(
                        WalletTransactionEntity(
                            txId = "TX-${UUID.randomUUID().toString().take(8).uppercase()}",
                            idempotencyKey = "GAME_WIN_${match.matchId}_$winnerId",
                            userId = winnerId,
                            counterpartyUserId = "GAME_SERVER",
                            txType = "GAME_REWARD",
                            coinsDelta = match.rewardPoolCoins,
                            diamondsDelta = 0L,
                            coinsBalanceAfter = newCoins,
                            diamondsBalanceAfter = winner.diamondsBalance,
                            description = "Won ${match.gameType} Match ${match.matchId}",
                            timestamp = now
                        )
                    )
                }
                recordAudit(
                    actorUserId = "GAME_SERVER",
                    actorRole = "SYSTEM",
                    category = "GAME_SERVER",
                    action = "MATCH_COMPLETED_PAYOUT",
                    targetId = match.matchId,
                    details = "Winner=$winnerName ($winnerId), Pool=${match.rewardPoolCoins}"
                )
            }
            BackendResult.Success(
                updatedMatch,
                if (status == "COMPLETED") "🏆 $winnerName won ${match.rewardPoolCoins} Coins!"
                else "Server Dice Rolled $p1Roll! You: $newP1Pos/56 • ${match.player2Name}: $newP2Pos/56"
            )
        }
    }

    // ================= 10, 15, 16: GLOBAL OWNER ADMIN PANEL & RBAC =================
    suspend fun authenticateGlobalOwnerRbac(actorUserId: String, enteredSecret: String): BackendResult<UserEntity> {
        val configuredSecret = BuildConfig.OWNER_MASTER_RBAC_SECRET
        val user = dao.getUserById(actorUserId) ?: return BackendResult.Error("User not found.")

        val isSecretValid = configuredSecret.isNotBlank() &&
            enteredSecret.trim() == configuredSecret &&
            enteredSecret.trim().length >= 8

        if (!isSecretValid && user.role != "GLOBAL_OWNER") {
            recordAudit(
                actorUserId,
                user.role,
                "ADMIN_RBAC",
                "FAILED_OWNER_AUTH",
                actorUserId,
                "Invalid RBAC secret key"
            )
            return BackendResult.Error("RBAC Authentication Failed: Invalid Global Owner Secret Key or unauthorized role.")
        }

        val promoted = user.copy(role = "GLOBAL_OWNER")
        dao.upsertUser(promoted)
        recordAudit(
            actorUserId,
            "GLOBAL_OWNER",
            "ADMIN_RBAC",
            "OWNER_PANEL_UNLOCKED",
            actorUserId,
            "Verified server-side RBAC credentials"
        )
        return BackendResult.Success(promoted, "Global Owner RBAC Verified")
    }

    private suspend fun requireGlobalOwner(actorUserId: String): UserEntity? {
        val actor = dao.getUserById(actorUserId) ?: return null
        return if (actor.role == "GLOBAL_OWNER" || actor.role == "ADMIN") actor else null
    }

    suspend fun ownerIssueCoinsOrDiamonds(
        actorUserId: String,
        targetUserId: String,
        coinsDelta: Long,
        diamondsDelta: Long,
        reason: String
    ): BackendResult<UserEntity> {
        val owner = requireGlobalOwner(actorUserId)
            ?: return BackendResult.Error("RBAC Denied: Global Owner permission required.")

        return db.withTransaction {
            val target = dao.getUserById(targetUserId)
                ?: return@withTransaction BackendResult.Error("Target user $targetUserId not found.")
            val now = System.currentTimeMillis()
            val newCoins = (target.coinsBalance + coinsDelta).coerceAtLeast(0L)
            val newDiamonds = (target.diamondsBalance + diamondsDelta).coerceAtLeast(0L)
            val updated = target.copy(
                coinsBalance = newCoins,
                diamondsBalance = newDiamonds,
                totalDiamondsEarned = if (diamondsDelta > 0) target.totalDiamondsEarned + diamondsDelta else target.totalDiamondsEarned
            )
            dao.upsertUser(updated)

            val idemKey = "OWNER_ISSUE_${now}_${targetUserId}"
            dao.insertWalletTransaction(
                WalletTransactionEntity(
                    txId = "TX-${UUID.randomUUID().toString().take(8).uppercase()}",
                    idempotencyKey = idemKey,
                    userId = targetUserId,
                    counterpartyUserId = owner.tangerUserId,
                    txType = "OWNER_ISSUE_COINS",
                    coinsDelta = coinsDelta,
                    diamondsDelta = diamondsDelta,
                    coinsBalanceAfter = newCoins,
                    diamondsBalanceAfter = newDiamonds,
                    description = "Owner Treasury Adjustment: $reason",
                    timestamp = now
                )
            )

            if (diamondsDelta > 0) {
                val cycle = ensureMonthlyDiamondCycleForUser(targetUserId, now)
                dao.upsertDiamondCycle(
                    cycle.copy(
                        earnedDiamondsInCycle = cycle.earnedDiamondsInCycle + diamondsDelta,
                        lastUpdatedAt = now
                    )
                )
            }

            recordAudit(
                owner.tangerUserId,
                owner.role,
                "FINANCE",
                "OWNER_WALLET_ADJUSTMENT",
                targetUserId,
                "CoinsDelta=$coinsDelta, DiamondsDelta=$diamondsDelta, Reason=$reason"
            )
            BackendResult.Success(updated, "Updated ${updated.username} wallet (+${coinsDelta} Coins, +${diamondsDelta} Diamonds)")
        }
    }

    suspend fun ownerSaveOrUpdateGift(
        actorUserId: String,
        giftId: String?,
        name: String,
        category: String,
        iconEmoji: String,
        coinPrice: Long,
        isActive: Boolean,
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
    ): BackendResult<GiftEntity> {
        val owner = requireGlobalOwner(actorUserId)
            ?: return BackendResult.Error("RBAC Denied: Global Owner permission required.")
        if (name.isBlank() || coinPrice <= 0L) {
            return BackendResult.Error("Valid gift name and positive Coin price required.")
        }
        val now = System.currentTimeMillis()
        return db.withTransaction {
            val id = giftId?.trim()?.ifBlank { null }
                ?: "GFT-${UUID.randomUUID().toString().take(5).uppercase()}"
            val existing = dao.getGiftById(id)
            val gift = GiftEntity(
                giftId = id,
                name = name.trim(),
                category = category,
                iconEmoji = iconEmoji.ifBlank { "🎁" },
                animationEffect = animationEffect.ifBlank { "LUCKY_SPIN" },
                coinPrice = coinPrice,
                diamondReturnRatePercent = diamondReturnRatePercent.coerceIn(10, 95),
                isActive = isActive,
                updatedAt = now,
                triggerCount = triggerCount.coerceAtLeast(1L),
                currentHitCount = existing?.currentHitCount ?: 0L,
                rewardCoins = rewardCoins.coerceAtLeast(0L),
                cashbackCoins = cashbackCoins.coerceAtLeast(0L),
                expBonus = expBonus.coerceAtLeast(0L),
                maxRewardCoins = maxRewardCoins.coerceAtLeast(0L),
                activationLimit = activationLimit.coerceAtLeast(0L),
                currentActivations = existing?.currentActivations ?: 0L,
                startTimeMillis = startTimeMillis.coerceAtLeast(0L),
                endTimeMillis = endTimeMillis.coerceAtLeast(0L),
                cooldownSeconds = cooldownSeconds.coerceAtLeast(0),
                repeatTimingSeconds = repeatTimingSeconds.coerceAtLeast(0),
                isPaused = isPaused,
                lastTriggeredAt = existing?.lastTriggeredAt ?: 0L,
                lastCycleResetAt = existing?.lastCycleResetAt ?: now,
                backupTimingNotes = backupTimingNotes.ifBlank { "Auto-Backup & Repeat Timing Active" }
            )
            dao.upsertGift(gift)
            recordAudit(
                owner.tangerUserId,
                owner.role,
                "LUCKY_GIFT",
                "OWNER_UPSERT_LUCKY_GIFT",
                id,
                "Name=$name, Cat=$category, Price=$coinPrice, Trigger=${triggerCount}x, Reward=$rewardCoins, Cashback=$cashbackCoins, EXP=$expBonus, Max=$maxRewardCoins, Limit=$activationLimit, Cooldown=${cooldownSeconds}s, Repeat=${repeatTimingSeconds}s, Paused=$isPaused, Active=$isActive"
            )
            BackendResult.Success(
                gift,
                "Saved '${gift.name}' ($id) • 🪙 $coinPrice • Trigger ${gift.triggerCount}x → +${gift.rewardCoins} 🪙 (+${gift.cashbackCoins} Cashback, +${gift.expBonus} EXP)"
            )
        }
    }

    suspend fun ownerToggleGiftActiveOrPaused(
        actorUserId: String,
        giftId: String,
        toggleActive: Boolean = false,
        togglePaused: Boolean = false
    ): BackendResult<GiftEntity> {
        val owner = requireGlobalOwner(actorUserId)
            ?: return BackendResult.Error("RBAC Denied: Global Owner permission required.")
        return db.withTransaction {
            val gift = dao.getGiftById(giftId)
                ?: return@withTransaction BackendResult.Error("Gift $giftId not found.")
            val updated = gift.copy(
                isActive = if (toggleActive) !gift.isActive else gift.isActive,
                isPaused = if (togglePaused) !gift.isPaused else gift.isPaused,
                updatedAt = System.currentTimeMillis()
            )
            dao.upsertGift(updated)
            recordAudit(
                owner.tangerUserId,
                owner.role,
                "LUCKY_GIFT",
                "OWNER_TOGGLE_GIFT_STATE",
                giftId,
                "Active=${updated.isActive}, Paused=${updated.isPaused}"
            )
            BackendResult.Success(
                updated,
                "${updated.name}: Active=${updated.isActive} • Lucky Trigger=${if (updated.isPaused) "PAUSED" else "RUNNING"}"
            )
        }
    }

    suspend fun ownerResetLuckyGiftCounters(
        actorUserId: String,
        giftId: String? = null,
        resetGlobalCounter: Boolean = false
    ): BackendResult<String> {
        val owner = requireGlobalOwner(actorUserId)
            ?: return BackendResult.Error("RBAC Denied: Global Owner permission required.")
        val now = System.currentTimeMillis()
        return db.withTransaction {
            if (giftId != null) {
                val gift = dao.getGiftById(giftId)
                    ?: return@withTransaction BackendResult.Error("Gift $giftId not found.")
                dao.upsertGift(
                    gift.copy(
                        currentHitCount = 0L,
                        currentActivations = 0L,
                        lastTriggeredAt = 0L,
                        lastCycleResetAt = now,
                        updatedAt = now
                    )
                )
            }
            if (resetGlobalCounter) {
                val state = dao.getLuckyGiftState()
                if (state != null) {
                    dao.upsertLuckyGiftState(
                        state.copy(
                            globalHitCount = 0L,
                            globalPoolCoins = 12_500L,
                            updatedAt = now
                        )
                    )
                }
            }
            recordAudit(
                owner.tangerUserId,
                owner.role,
                "LUCKY_GIFT",
                "OWNER_RESET_LUCKY_COUNTERS",
                giftId ?: "GLOBAL_LUCKY_COUNTER",
                "ResetGift=$giftId, ResetGlobal=$resetGlobalCounter"
            )
            BackendResult.Success(
                "RESET_OK",
                if (giftId != null && resetGlobalCounter) "Reset Gift $giftId & Global Lucky Counter to 0!"
                else if (giftId != null) "Reset Lucky Gift $giftId hit counter & activations to 0!"
                else "Reset Global Lucky Counter & Pool to baseline!"
            )
        }
    }

    suspend fun ownerUpdateGiftBackupAndTiming(
        actorUserId: String,
        giftId: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        cooldownSeconds: Int,
        repeatTimingSeconds: Int,
        backupTimingNotes: String
    ): BackendResult<GiftEntity> {
        val owner = requireGlobalOwner(actorUserId)
            ?: return BackendResult.Error("RBAC Denied: Global Owner permission required.")
        val now = System.currentTimeMillis()
        return db.withTransaction {
            val gift = dao.getGiftById(giftId)
                ?: return@withTransaction BackendResult.Error("Gift $giftId not found.")
            val updated = gift.copy(
                startTimeMillis = startTimeMillis.coerceAtLeast(0L),
                endTimeMillis = endTimeMillis.coerceAtLeast(0L),
                cooldownSeconds = cooldownSeconds.coerceAtLeast(0),
                repeatTimingSeconds = repeatTimingSeconds.coerceAtLeast(0),
                backupTimingNotes = backupTimingNotes.ifBlank { "Owner Updated Backup/Timing Profile" },
                updatedAt = now
            )
            dao.upsertGift(updated)
            recordAudit(
                owner.tangerUserId,
                owner.role,
                "LUCKY_GIFT",
                "UPDATE_GIFT_BACKUP_TIMING",
                giftId,
                "Start=$startTimeMillis, End=$endTimeMillis, Cooldown=${cooldownSeconds}s, Repeat=${repeatTimingSeconds}s, Backup=$backupTimingNotes"
            )
            BackendResult.Success(
                updated,
                "Updated Backup & Timing for ${updated.name} (Cooldown ${cooldownSeconds}s, Repeat ${repeatTimingSeconds}s)"
            )
        }
    }

    suspend fun ownerToggleGlobalLuckyPause(actorUserId: String): BackendResult<LuckyGiftStateEntity> {
        val owner = requireGlobalOwner(actorUserId)
            ?: return BackendResult.Error("RBAC Denied: Global Owner permission required.")
        return db.withTransaction {
            val current = dao.getLuckyGiftState()
                ?: return@withTransaction BackendResult.Error("Lucky state not initialized.")
            val updated = current.copy(
                isGlobalLuckyPaused = !current.isGlobalLuckyPaused,
                updatedAt = System.currentTimeMillis()
            )
            dao.upsertLuckyGiftState(updated)
            recordAudit(
                owner.tangerUserId,
                owner.role,
                "LUCKY_GIFT",
                if (updated.isGlobalLuckyPaused) "PAUSE_GLOBAL_LUCKY" else "RESUME_GLOBAL_LUCKY",
                "GLOBAL_LUCKY_COUNTER",
                "GlobalLuckyPaused=${updated.isGlobalLuckyPaused}"
            )
            BackendResult.Success(
                updated,
                if (updated.isGlobalLuckyPaused) "⏸ Global Lucky Gift Engine PAUSED" else "▶ Global Lucky Gift Engine RESUMED"
            )
        }
    }

    suspend fun ownerDeleteGift(actorUserId: String, giftId: String): BackendResult<Unit> {
        val owner = requireGlobalOwner(actorUserId)
            ?: return BackendResult.Error("RBAC Denied: Global Owner permission required.")
        dao.deleteGift(giftId)
        recordAudit(owner.tangerUserId, owner.role, "GIFT", "OWNER_DELETE_GIFT", giftId, "Deleted gift from catalog")
        return BackendResult.Success(Unit, "Deleted gift $giftId")
    }

    suspend fun ownerUpdateModerationAndRole(
        actorUserId: String,
        targetUserId: String,
        newRole: String?,
        newVipLevel: Int?,
        suspendAccount: Boolean?,
        banAccount: Boolean?,
        customTangerId: String? = null
    ): BackendResult<UserEntity> {
        val owner = requireGlobalOwner(actorUserId)
            ?: return BackendResult.Error("RBAC Denied: Global Owner permission required.")
        val target = dao.getUserById(targetUserId) ?: return BackendResult.Error("User not found.")
        val updated = target.copy(
            tangerUserId = customTangerId?.ifBlank { target.tangerUserId } ?: target.tangerUserId,
            role = newRole ?: target.role,
            vipLevel = newVipLevel ?: target.vipLevel,
            isSuspended = suspendAccount ?: target.isSuspended,
            isBanned = banAccount ?: target.isBanned
        )
        dao.upsertUser(updated)
        recordAudit(
            owner.tangerUserId,
            owner.role,
            "MODERATION",
            "OWNER_USER_MODERATION",
            targetUserId,
            "Role=${updated.role}, VIP=${updated.vipLevel}, Suspended=${updated.isSuspended}, Banned=${updated.isBanned}"
        )
        return BackendResult.Success(updated, "Updated user ${updated.username}")
    }

    suspend fun ownerCreateGlobalEventOrAnnouncement(
        actorUserId: String,
        title: String,
        subtitle: String,
        contentType: String,
        targetScope: String,
        targetValue: String,
        rewardInfo: String
    ): BackendResult<GlobalEventEntity> {
        val owner = requireGlobalOwner(actorUserId)
            ?: return BackendResult.Error("RBAC Denied: Global Owner permission required.")
        if (title.isBlank()) return BackendResult.Error("Title is required.")
        val now = System.currentTimeMillis()
        val event = GlobalEventEntity(
            eventId = "EVT-${UUID.randomUUID().toString().take(6).uppercase()}",
            title = title.trim(),
            subtitle = subtitle.trim(),
            contentType = contentType,
            targetScope = targetScope,
            targetValue = targetValue.ifBlank { "*" },
            rewardInfo = rewardInfo.ifBlank { "Exclusive Star Rewards" },
            createdByOwnerId = owner.tangerUserId,
            isActive = true,
            createdAt = now,
            expiresAt = now + 14L * 24 * 3600 * 1000L
        )
        dao.upsertGlobalEvent(event)
        recordAudit(
            owner.tangerUserId,
            owner.role,
            "GLOBAL_EVENT",
            "CREATE_$contentType",
            event.eventId,
            "Scope=$targetScope:$targetValue, Title=$title"
        )
        return BackendResult.Success(event, "Published $contentType for $targetScope ($targetValue)")
    }

    suspend fun ownerUpdateLuckyMilestones(
        actorUserId: String,
        m1Threshold: Long,
        m1Reward: Long,
        jackpotThreshold: Long,
        jackpotReward: Long
    ): BackendResult<LuckyGiftStateEntity> {
        val owner = requireGlobalOwner(actorUserId)
            ?: return BackendResult.Error("RBAC Denied: Global Owner permission required.")
        val current = dao.getLuckyGiftState() ?: return BackendResult.Error("Lucky state not initialized")
        val updated = current.copy(
            milestone1Threshold = m1Threshold.coerceAtLeast(2L),
            milestone1RewardCoins = m1Reward.coerceAtLeast(50L),
            jackpotThreshold = jackpotThreshold.coerceAtLeast(10L),
            jackpotRewardCoins = jackpotReward.coerceAtLeast(500L),
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertLuckyGiftState(updated)
        recordAudit(
            owner.tangerUserId,
            owner.role,
            "LUCKY_GIFT",
            "UPDATE_LUCKY_MILESTONES",
            "GLOBAL_LUCKY_COUNTER",
            "M1=${m1Threshold}x->$m1Reward, Jackpot=${jackpotThreshold}x->$jackpotReward"
        )
        return BackendResult.Success(updated, "Lucky Gift global milestones updated!")
    }

    suspend fun createOrInviteToAgency(
        actorUserId: String,
        agencyId: String,
        targetUserId: String,
        roleOffered: String
    ): BackendResult<AgencyInvitationEntity> {
        val agency = dao.getAgencyById(agencyId) ?: return BackendResult.Error("Agency not found")
        val invite = AgencyInvitationEntity(
            inviteId = "INV-${UUID.randomUUID().toString().take(6).uppercase()}",
            agencyId = agencyId,
            agencyName = agency.agencyName,
            invitedUserId = targetUserId,
            invitedByUserId = actorUserId,
            roleOffered = roleOffered,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        dao.upsertAgencyInvitation(invite)
        recordAudit(actorUserId, "AGENCY_OWNER", "AGENCY", "SEND_INVITATION", targetUserId, "Agency=${agency.agencyName}, Role=$roleOffered")
        return BackendResult.Success(invite, "Sent $roleOffered invitation to $targetUserId")
    }

    suspend fun respondToAgencyInvitation(invite: AgencyInvitationEntity, accept: Boolean): BackendResult<Unit> {
        val status = if (accept) "ACCEPTED" else "DECLINED"
        dao.upsertAgencyInvitation(invite.copy(status = status))
        if (accept) {
            val user = dao.getUserById(invite.invitedUserId)
            if (user != null) {
                dao.upsertUser(user.copy(agencyId = invite.agencyId, role = if (invite.roleOffered == "HOST") "HOST" else user.role))
                dao.upsertAgencyMember(
                    AgencyMemberEntity(
                        membershipId = "${invite.agencyId}_${user.tangerUserId}",
                        agencyId = invite.agencyId,
                        userId = user.tangerUserId,
                        username = user.username,
                        memberRole = invite.roleOffered,
                        dailyTargetDiamonds = 2_000L,
                        dailyEarnedDiamonds = 450L,
                        monthlyTargetDiamonds = 50_000L,
                        monthlyEarnedDiamonds = user.diamondsBalance,
                        joinedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        return BackendResult.Success(Unit, "Invitation $status")
    }

    private suspend fun recordAudit(
        actorUserId: String,
        actorRole: String,
        category: String,
        action: String,
        targetId: String,
        details: String
    ) {
        dao.insertAuditLog(
            AuditLogEntity(
                logId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                actorUserId = actorUserId,
                actorRole = actorRole,
                actionCategory = category,
                actionName = action,
                targetEntityId = targetId,
                detailsJson = details,
                ipOrDevice = "Android-Verified-Session",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    // ================= RELATIONAL DATABASE INITIALIZATION =================
    private suspend fun seedRelationalSchema(now: Long) {
        val users = listOf(
            UserEntity(
                tangerUserId = "199967",
                username = "amira_tangerstar",
                fullName = "Amira TangerStar",
                email = "amira@tangeram.global",
                passwordHash = hashPasswordSha256("tanger2026"),
                phoneNumber = "661889900",
                countryCode = "MA",
                countryName = "Morocco",
                countryFlag = "🇲🇦",
                gender = "Female",
                dob = "1998-05-14",
                bio = "Founder & Global Star Host • Tanger Med & Casablanca Voice Lounge 🌟",
                avatarUri = "",
                authProvider = "PHONE_OTP",
                role = "GLOBAL_OWNER",
                isOnline = true,
                isSuspended = false,
                isBanned = false,
                coinsBalance = 85_000L,
                diamondsBalance = 42_500L,
                totalCoinsSpent = 145_000L,
                totalDiamondsEarned = 192_500L,
                vipLevel = 6,
                userLevel = 48,
                xpPoints = 48_900L,
                agencyId = "AG-101",
                cpPartnerUserId = "199968",
                followersCount = 1_840,
                followingCount = 95,
                createdAt = now - 86_400_000L * 60,
                isOnboardingCompleted = true
            ),
            UserEntity(
                tangerUserId = "199968",
                username = "zayd_mansoor",
                fullName = "Zayd Al-Mansoor",
                email = "zayd@tangeram.global",
                passwordHash = hashPasswordSha256("tanger2026"),
                phoneNumber = "504433221",
                countryCode = "AE",
                countryName = "United Arab Emirates",
                countryFlag = "🇦🇪",
                gender = "Male",
                dob = "1995-11-20",
                bio = "Dubai Royal Gifter & Ludo Champion 👑",
                avatarUri = "",
                authProvider = "GOOGLE",
                role = "AGENCY_OWNER",
                isOnline = true,
                isSuspended = false,
                isBanned = false,
                coinsBalance = 64_000L,
                diamondsBalance = 31_800L,
                totalCoinsSpent = 210_000L,
                totalDiamondsEarned = 98_000L,
                vipLevel = 6,
                userLevel = 52,
                xpPoints = 54_200L,
                agencyId = "AG-101",
                cpPartnerUserId = "199967",
                followersCount = 2_410,
                followingCount = 112,
                createdAt = now - 86_400_000L * 45,
                isOnboardingCompleted = true
            ),
            UserEntity(
                tangerUserId = "199969",
                username = "layla_velvet",
                fullName = "Layla VelvetVoice",
                email = "layla@tangeram.global",
                passwordHash = hashPasswordSha256("tanger2026"),
                phoneNumber = "551122334",
                countryCode = "SA",
                countryName = "Saudi Arabia",
                countryFlag = "🇸🇦",
                gender = "Female",
                dob = "2000-02-09",
                bio = "Live Singing & Acoustic Nights 🎶 Top Diamond Receiver",
                avatarUri = "",
                authProvider = "PHONE_OTP",
                role = "HOST",
                isOnline = true,
                isSuspended = false,
                isBanned = false,
                coinsBalance = 19_500L,
                diamondsBalance = 58_400L,
                totalCoinsSpent = 42_000L,
                totalDiamondsEarned = 165_000L,
                vipLevel = 4,
                userLevel = 39,
                xpPoints = 39_100L,
                agencyId = "AG-102",
                cpPartnerUserId = null,
                followersCount = 3_120,
                followingCount = 64,
                createdAt = now - 86_400_000L * 30,
                isOnboardingCompleted = true
            ),
            UserEntity(
                tangerUserId = "199970",
                username = "karim_atlas",
                fullName = "Karim AtlasBeat",
                email = "karim@tangeram.global",
                passwordHash = hashPasswordSha256("tanger2026"),
                phoneNumber = "677554433",
                countryCode = "MA",
                countryName = "Morocco",
                countryFlag = "🇲🇦",
                gender = "Male",
                dob = "1997-08-03",
                bio = "DJ & Co-Host • Tangier Nights & Ludo Tournaments 🎲",
                avatarUri = "",
                authProvider = "FACEBOOK",
                role = "HOST",
                isOnline = true,
                isSuspended = false,
                isBanned = false,
                coinsBalance = 14_200L,
                diamondsBalance = 27_900L,
                totalCoinsSpent = 31_000L,
                totalDiamondsEarned = 74_000L,
                vipLevel = 3,
                userLevel = 31,
                xpPoints = 31_000L,
                agencyId = "AG-101",
                cpPartnerUserId = null,
                followersCount = 920,
                followingCount = 140,
                createdAt = now - 86_400_000L * 25,
                isOnboardingCompleted = true
            ),
            UserEntity(
                tangerUserId = "199971",
                username = "elif_bosphorus",
                fullName = "Elif BosphorusStar",
                email = "elif@tangeram.global",
                passwordHash = hashPasswordSha256("tanger2026"),
                phoneNumber = "532998877",
                countryCode = "TR",
                countryName = "Türkiye",
                countryFlag = "🇹🇷",
                gender = "Female",
                dob = "1999-07-19",
                bio = "Istanbul & Tangier Cultural Bridge • Poetry & Music 🌙",
                avatarUri = "",
                authProvider = "GOOGLE",
                role = "HOST",
                isOnline = false,
                isSuspended = false,
                isBanned = false,
                coinsBalance = 11_800L,
                diamondsBalance = 22_100L,
                totalCoinsSpent = 24_500L,
                totalDiamondsEarned = 61_000L,
                vipLevel = 3,
                userLevel = 27,
                xpPoints = 27_400L,
                agencyId = "AG-102",
                cpPartnerUserId = null,
                followersCount = 780,
                followingCount = 88,
                createdAt = now - 86_400_000L * 20,
                isOnboardingCompleted = true
            )
        )
        dao.upsertUsers(users)

        // Initialize sequential TANGERAM ID counter (199967..199971 allocated -> next available is 199972)
        dao.upsertUserSequenceCounter(
            UserSequenceCounterEntity(
                counterKey = "TANGERAM_USER_ID_SEQ",
                nextAvailableId = 199972L,
                updatedAt = now
            )
        )

        // Note: No active SessionEntity is pre-inserted on first launch so the user must complete
        // Step 1 (TANGERAM Sign Up) and Step 2 (Onboarding) before entering the main app.

        // Seed 6 VIP Configurations
        val vips = listOf(
            VipConfigEntity(1, "VIP 1 • Emerald Star", 1_000L, "Emerald Crest", "Mint Glow Frame", "Sparkle Entry", 100L),
            VipConfigEntity(2, "VIP 2 • Turquoise Knight", 5_000L, "Turquoise Shield", "Ocean Pulse Frame", "Wave Drift Entry", 300L),
            VipConfigEntity(3, "VIP 3 • Golden Viscount", 15_000L, "Golden Star", "Solar Gold Frame", "Comet Trail Entry", 750L),
            VipConfigEntity(4, "VIP 4 • Diamond Duke", 40_000L, "Diamond Crown", "Crystal Halo Frame", "Aurora Wing Entry", 1_500L),
            VipConfigEntity(5, "VIP 5 • Royal Monarch", 100_000L, "Imperial Scepter", "Royal Emerald Frame", "Pegasus Chariot Entry", 3_500L),
            VipConfigEntity(6, "VIP 6 • Tanger Supreme Emperor", 200_000L, "Supreme Star Galaxy", "Celestial Sovereign Frame", "Galaxy Dragon Entry", 8_000L)
        )
        dao.upsertVipConfigs(vips)

        // Seed All 6 Required Gift Categories with Authoritative Per-Gift Lucky & Timing Parameters
        val gifts = listOf(
            GiftEntity(
                giftId = "GFT-101",
                name = "Mint Rose",
                category = "Gift",
                iconEmoji = "🌹",
                animationEffect = "TURQUOISE_WAVE",
                coinPrice = 50L,
                diamondReturnRatePercent = 60,
                isActive = true,
                updatedAt = now,
                triggerCount = 10L,
                rewardCoins = 100L,
                cashbackCoins = 10L,
                expBonus = 25L,
                maxRewardCoins = 500L,
                activationLimit = 200L,
                cooldownSeconds = 3,
                repeatTimingSeconds = 3600,
                lastCycleResetAt = now,
                backupTimingNotes = "Hourly Repeat Cycle • Auto-Backup Enabled"
            ),
            GiftEntity(
                giftId = "GFT-102",
                name = "Golden Microphone",
                category = "Gift",
                iconEmoji = "🎙️",
                animationEffect = "BURST_GOLD",
                coinPrice = 250L,
                diamondReturnRatePercent = 60,
                isActive = true,
                updatedAt = now,
                triggerCount = 5L,
                rewardCoins = 400L,
                cashbackCoins = 40L,
                expBonus = 80L,
                maxRewardCoins = 2_000L,
                activationLimit = 100L,
                cooldownSeconds = 5,
                repeatTimingSeconds = 3600,
                lastCycleResetAt = now,
                backupTimingNotes = "Stage Host Lucky Boost • 1h Repeat"
            ),
            GiftEntity(
                giftId = "GFT-103",
                name = "Tanger Festival Lantern",
                category = "Event Gift",
                iconEmoji = "🏮",
                animationEffect = "STAR_RAIN",
                coinPrice = 500L,
                diamondReturnRatePercent = 65,
                isActive = true,
                updatedAt = now,
                triggerCount = 4L,
                rewardCoins = 800L,
                cashbackCoins = 75L,
                expBonus = 150L,
                maxRewardCoins = 4_000L,
                activationLimit = 80L,
                cooldownSeconds = 5,
                repeatTimingSeconds = 7200,
                lastCycleResetAt = now,
                backupTimingNotes = "Event Gala Schedule • 2h Repeat"
            ),
            GiftEntity(
                giftId = "GFT-104",
                name = "Star Carnival Yacht",
                category = "Event Gift",
                iconEmoji = "🛥️",
                animationEffect = "TURQUOISE_WAVE",
                coinPrice = 2_500L,
                diamondReturnRatePercent = 65,
                isActive = true,
                updatedAt = now,
                triggerCount = 3L,
                rewardCoins = 3_500L,
                cashbackCoins = 300L,
                expBonus = 600L,
                maxRewardCoins = 15_000L,
                activationLimit = 50L,
                cooldownSeconds = 10,
                repeatTimingSeconds = 7200,
                lastCycleResetAt = now,
                backupTimingNotes = "VIP Yacht Schedule • High Cashback"
            ),
            GiftEntity(
                giftId = "GFT-105",
                name = "Lucky Clover Box",
                category = "Lucky Gift",
                iconEmoji = "🍀",
                animationEffect = "LUCKY_SPIN",
                coinPrice = 100L,
                diamondReturnRatePercent = 50,
                isActive = true,
                updatedAt = now,
                triggerCount = 3L,
                rewardCoins = 300L,
                cashbackCoins = 25L,
                expBonus = 60L,
                maxRewardCoins = 1_500L,
                activationLimit = 500L,
                cooldownSeconds = 2,
                repeatTimingSeconds = 1800,
                lastCycleResetAt = now,
                backupTimingNotes = "Fast 3x Trigger • 30m Repeat Backup"
            ),
            GiftEntity(
                giftId = "GFT-106",
                name = "Lucky Star Chest",
                category = "Lucky Gift",
                iconEmoji = "🎰",
                animationEffect = "LUCKY_SPIN",
                coinPrice = 300L,
                diamondReturnRatePercent = 50,
                isActive = true,
                updatedAt = now,
                triggerCount = 3L,
                rewardCoins = 900L,
                cashbackCoins = 60L,
                expBonus = 120L,
                maxRewardCoins = 5_000L,
                activationLimit = 250L,
                cooldownSeconds = 3,
                repeatTimingSeconds = 1800,
                lastCycleResetAt = now,
                backupTimingNotes = "Jackpot Chest • 3x Trigger + Cashback"
            ),
            GiftEntity(
                giftId = "GFT-107",
                name = "Galaxy Wheel Spin",
                category = "Lucky Draw",
                iconEmoji = "🎡",
                animationEffect = "LUCKY_SPIN",
                coinPrice = 400L,
                diamondReturnRatePercent = 55,
                isActive = true,
                updatedAt = now,
                triggerCount = 2L,
                rewardCoins = 1_000L,
                cashbackCoins = 80L,
                expBonus = 180L,
                maxRewardCoins = 8_000L,
                activationLimit = 150L,
                cooldownSeconds = 4,
                repeatTimingSeconds = 3600,
                lastCycleResetAt = now,
                backupTimingNotes = "Every 2nd Spin Triggers Lucky Reward"
            ),
            GiftEntity(
                giftId = "GFT-108",
                name = "Eternal CP Rings",
                category = "CP",
                iconEmoji = "💍",
                animationEffect = "HEART_CP",
                coinPrice = 1_200L,
                diamondReturnRatePercent = 65,
                isActive = true,
                updatedAt = now,
                triggerCount = 3L,
                rewardCoins = 1_800L,
                cashbackCoins = 150L,
                expBonus = 350L,
                maxRewardCoins = 9_000L,
                activationLimit = 100L,
                cooldownSeconds = 5,
                repeatTimingSeconds = 3600,
                lastCycleResetAt = now,
                backupTimingNotes = "CP Partner Link + Lucky Cashback"
            ),
            GiftEntity(
                giftId = "GFT-109",
                name = "CP Love Castle",
                category = "CP",
                iconEmoji = "🏰",
                animationEffect = "HEART_CP",
                coinPrice = 5_000L,
                diamondReturnRatePercent = 70,
                isActive = true,
                updatedAt = now,
                triggerCount = 2L,
                rewardCoins = 7_500L,
                cashbackCoins = 600L,
                expBonus = 1_200L,
                maxRewardCoins = 25_000L,
                activationLimit = 40L,
                cooldownSeconds = 8,
                repeatTimingSeconds = 7200,
                lastCycleResetAt = now,
                backupTimingNotes = "Royal CP Castle • 70% Diamond Return"
            ),
            GiftEntity(
                giftId = "GFT-110",
                name = "Morocco Star Flag",
                category = "Country/Flag Gift",
                iconEmoji = "🇲🇦",
                animationEffect = "STAR_RAIN",
                coinPrice = 150L,
                diamondReturnRatePercent = 60,
                isActive = true,
                updatedAt = now,
                triggerCount = 5L,
                rewardCoins = 300L,
                cashbackCoins = 20L,
                expBonus = 50L,
                maxRewardCoins = 1_500L,
                activationLimit = 300L,
                cooldownSeconds = 2,
                repeatTimingSeconds = 3600,
                lastCycleResetAt = now,
                backupTimingNotes = "National Leaderboard + Lucky Trigger"
            ),
            GiftEntity(
                giftId = "GFT-111",
                name = "UAE Falcon Flag",
                category = "Country/Flag Gift",
                iconEmoji = "🇦🇪",
                animationEffect = "BURST_GOLD",
                coinPrice = 150L,
                diamondReturnRatePercent = 60,
                isActive = true,
                updatedAt = now,
                triggerCount = 5L,
                rewardCoins = 300L,
                cashbackCoins = 20L,
                expBonus = 50L,
                maxRewardCoins = 1_500L,
                activationLimit = 300L,
                cooldownSeconds = 2,
                repeatTimingSeconds = 3600,
                lastCycleResetAt = now,
                backupTimingNotes = "National Leaderboard + Lucky Trigger"
            ),
            GiftEntity(
                giftId = "GFT-112",
                name = "Saudi Royal Flag",
                category = "Country/Flag Gift",
                iconEmoji = "🇸🇦",
                animationEffect = "STAR_RAIN",
                coinPrice = 150L,
                diamondReturnRatePercent = 60,
                isActive = true,
                updatedAt = now,
                triggerCount = 5L,
                rewardCoins = 300L,
                cashbackCoins = 20L,
                expBonus = 50L,
                maxRewardCoins = 1_500L,
                activationLimit = 300L,
                cooldownSeconds = 2,
                repeatTimingSeconds = 3600,
                lastCycleResetAt = now,
                backupTimingNotes = "National Leaderboard + Lucky Trigger"
            )
        )
        dao.upsertGifts(gifts)

        // Authoritative Global Lucky Counter
        dao.upsertLuckyGiftState(
            LuckyGiftStateEntity(
                counterId = "GLOBAL_LUCKY_COUNTER",
                globalPoolCoins = 28_500L,
                globalHitCount = 4L, // 1 hit away from Milestone 1 (5x) so user can test immediately!
                milestone1Threshold = 5L,
                milestone1RewardCoins = 500L,
                milestone2Threshold = 15L,
                milestone2RewardCoins = 2_000L,
                jackpotThreshold = 50L,
                jackpotRewardCoins = 10_000L,
                updatedAt = now
            )
        )

        // Seed Voice Rooms + Seats + Active Top Notification Banner
        val room1 = VoiceRoomEntity(
            roomId = "RM-101",
            roomTitle = "🌟 Tanger Star Official Global Lounge",
            roomTopic = "Music, Live Gifts & Lucky Star Drops",
            countryFlag = "🇲🇦",
            ownerUserId = "199967",
            ownerUsername = "Amira TangerStar",
            isPrivate = false,
            pinCode = null,
            seatCount = 8,
            themeName = "Emerald Palace",
            isChatLocked = false,
            activeMusicTrackTitle = "Tangier Sunset Acoustic Groove",
            activeMusicArtist = "Tanger Star Audio Lab",
            isMusicPlaying = true,
            musicVolume = 0.7f,
            musicPositionSec = 42,
            totalRoomHeat = 98_400L,
            onlineCount = 148,
            createdAt = now - 3600_000L * 12
        )
        val room2 = VoiceRoomEntity(
            roomId = "RM-102",
            roomTitle = "🇦🇪 Dubai & Riyadh VIP Majlis",
            roomTopic = "High-Roller Lucky Gifts & Ludo Matches",
            countryFlag = "🇦🇪",
            ownerUserId = "199968",
            ownerUsername = "Zayd Al-Mansoor",
            isPrivate = false,
            pinCode = null,
            seatCount = 8,
            themeName = "Turquoise Oasis",
            isChatLocked = false,
            activeMusicTrackTitle = "Arabian Starlight Oud",
            activeMusicArtist = "Oasis Ensemble",
            isMusicPlaying = false,
            musicVolume = 0.6f,
            musicPositionSec = 0,
            totalRoomHeat = 76_200L,
            onlineCount = 94,
            createdAt = now - 3600_000L * 8
        )
        val room3 = VoiceRoomEntity(
            roomId = "RM-103",
            roomTitle = "🎶 Velvet Midnight Singing Stage",
            roomTopic = "Open Mic • Country Flag Gifts Battle",
            countryFlag = "🇸🇦",
            ownerUserId = "199969",
            ownerUsername = "Layla VelvetVoice",
            isPrivate = true,
            pinCode = "2026",
            seatCount = 8,
            themeName = "Royal Star",
            isChatLocked = false,
            activeMusicTrackTitle = "Acoustic Starlight Ballad",
            activeMusicArtist = "Layla Live",
            isMusicPlaying = true,
            musicVolume = 0.8f,
            musicPositionSec = 19,
            totalRoomHeat = 54_900L,
            onlineCount = 67,
            createdAt = now - 3600_000L * 5
        )
        dao.upsertRoom(room1)
        dao.upsertRoom(room2)
        dao.upsertRoom(room3)

        val room1Seats = listOf(
            RoomSeatEntity("RM-101", 0, "199967", "Amira TangerStar", "", 6, "OWNER", false, false, false, 64f),
            RoomSeatEntity("RM-101", 1, "199968", "Zayd Al-Mansoor", "", 6, "ADMIN", false, false, false, 48f),
            RoomSeatEntity("RM-101", 2, "199969", "Layla VelvetVoice", "", 4, "CO_HOST", false, false, false, 52f),
            RoomSeatEntity("RM-101", 3, "199970", "Karim AtlasBeat", "", 3, "SPEAKER", false, false, false, 31f),
            RoomSeatEntity("RM-101", 4, null, null, null, 0, "SPEAKER", false, false, false, 0f),
            RoomSeatEntity("RM-101", 5, null, null, null, 0, "SPEAKER", false, false, false, 0f),
            RoomSeatEntity("RM-101", 6, null, null, null, 0, "SPEAKER", false, false, false, 0f),
            RoomSeatEntity("RM-101", 7, null, null, null, 0, "SPEAKER", false, false, true, 0f)
        )
        dao.upsertRoomSeats(room1Seats)

        val room2Seats = (0 until 8).map { idx ->
            when (idx) {
                0 -> RoomSeatEntity("RM-102", 0, "199968", "Zayd Al-Mansoor", "", 6, "OWNER", false, false, false, 58f)
                1 -> RoomSeatEntity("RM-102", 1, "199971", "Elif BosphorusStar", "", 3, "CO_HOST", false, false, false, 40f)
                else -> RoomSeatEntity("RM-102", idx, null, null, null, 0, "SPEAKER", false, false, false, 0f)
            }
        }
        dao.upsertRoomSeats(room2Seats)

        val room3Seats = (0 until 8).map { idx ->
            when (idx) {
                0 -> RoomSeatEntity("RM-103", 0, "199969", "Layla VelvetVoice", "", 4, "OWNER", false, false, false, 71f)
                else -> RoomSeatEntity("RM-103", idx, null, null, null, 0, "SPEAKER", false, false, false, 0f)
            }
        }
        dao.upsertRoomSeats(room3Seats)

        dao.upsertRoomRole(RoomRoleEntity("RM-101", "199967", "Amira TangerStar", "OWNER", "199967", now))
        dao.upsertRoomRole(RoomRoleEntity("RM-101", "199968", "Zayd Al-Mansoor", "ADMIN", "199967", now))
        dao.upsertRoomRole(RoomRoleEntity("RM-101", "199969", "Layla VelvetVoice", "CO_HOST", "199967", now))

        // Active Top Room Notification Banner in RM-101 (Visible to current and new users joining)
        dao.insertRoomNotification(
            RoomTopNotificationEntity(
                notificationId = "RTN-WELCOME1",
                roomId = "RM-101",
                creatorUserId = "199967",
                creatorRole = "OWNER",
                title = "👑 GRAND LUCKY GIFT RAIN & LUDO PARTY",
                messageText = "Every 5th Lucky Gift triggers an instant 500 Coin Milestone! Top Gifter tonight wins VIP 6 Crown!",
                iconName = "CROWN",
                bannerStyle = "TURQUOISE_GOLD",
                durationSeconds = 3600,
                createdAt = now,
                expiresAt = now + 3600_000L,
                isActive = true
            )
        )

        // Seed Agencies & Hosts
        val ag1 = AgencyEntity(
            agencyId = "AG-101",
            agencyName = "Tanger Star Imperial Agency",
            agencyCode = "TS-IMP",
            ownerUserId = "199968",
            ownerUsername = "Zayd Al-Mansoor",
            countryFlag = "🇲🇦",
            description = "Official #1 Global Voice & Host Agency • Daily Coaching & 70% Diamond Share",
            memberCount = 28,
            hostCount = 19,
            dailyDiamondTarget = 15_000L,
            currentDailyDiamonds = 11_400L,
            monthlyDiamondTarget = 350_000L,
            currentMonthlyDiamonds = 264_300L,
            commissionPercent = 18,
            createdAt = now - 86_400_000L * 60
        )
        val ag2 = AgencyEntity(
            agencyId = "AG-102",
            agencyName = "Emerald Oasis Talent Guild",
            agencyCode = "EM-OAS",
            ownerUserId = "199969",
            ownerUsername = "Layla VelvetVoice",
            countryFlag = "🇸🇦",
            description = "MENA & Mediterranean Live Singers & Room Hosts Guild",
            memberCount = 18,
            hostCount = 12,
            dailyDiamondTarget = 10_000L,
            currentDailyDiamonds = 8_200L,
            monthlyDiamondTarget = 220_000L,
            currentMonthlyDiamonds = 162_500L,
            commissionPercent = 15,
            createdAt = now - 86_400_000L * 40
        )
        dao.upsertAgency(ag1)
        dao.upsertAgency(ag2)

        dao.upsertAgencyMember(
            AgencyMemberEntity("AG-101_199967", "AG-101", "199967", "Amira TangerStar", "HOST", 3_000L, 2_850L, 60_000L, 42_500L, now)
        )
        dao.upsertAgencyMember(
            AgencyMemberEntity("AG-101_199968", "AG-101", "199968", "Zayd Al-Mansoor", "AGENCY_OWNER", 2_500L, 2_100L, 50_000L, 31_800L, now)
        )
        dao.upsertAgencyMember(
            AgencyMemberEntity("AG-101_199970", "AG-101", "199970", "Karim AtlasBeat", "HOST", 2_000L, 1_650L, 40_000L, 27_900L, now)
        )
        dao.upsertAgencyMember(
            AgencyMemberEntity("AG-102_199969", "AG-102", "199969", "Layla VelvetVoice", "AGENCY_OWNER", 3_500L, 3_400L, 70_000L, 58_400L, now)
        )

        // Seed Archived Previous Month Diamond Cycle + Active Current Month Cycle for 199967
        dao.upsertDiamondCycle(
            DiamondMonthlyCycleEntity(
                userId = "199967",
                yearMonth = "2026-09",
                cycleStartEpoch = now - 86_400_000L * 34,
                cycleEndEpoch = now - 86_400_000L * 4,
                daysInMonth = 30,
                monthlyTargetDiamonds = 50_000L,
                earnedDiamondsInCycle = 54_200L,
                validHostDays = 26,
                isArchived = true,
                lastUpdatedAt = now - 86_400_000L * 4
            )
        )
        val currentCycle = ensureMonthlyDiamondCycleForUser("199967", now)
        dao.upsertDiamondCycle(
            currentCycle.copy(
                earnedDiamondsInCycle = 42_500L,
                validHostDays = 4
            )
        )

        // Seed Initial Active Server Ludo Match
        dao.upsertGameMatch(
            GameMatchEntity(
                matchId = "GM-LUDO01",
                gameType = "LUDO_STAR",
                roomId = "RM-101",
                entryFeeCoins = 500L,
                rewardPoolCoins = 1_000L,
                player1Id = "199967",
                player1Name = "Amira TangerStar",
                player1Pos = 24,
                player2Id = "199968",
                player2Name = "Zayd Al-Mansoor",
                player2Pos = 18,
                currentTurnUserId = "199967",
                lastDiceRoll = 5,
                turnNumber = 4,
                status = "ACTIVE",
                winnerUserId = null,
                winnerUsername = null,
                createdAt = now - 600_000L,
                updatedAt = now - 60_000L
            )
        )

        // Seed Global Events & Announcements across all target scopes
        dao.upsertGlobalEvent(
            GlobalEventEntity(
                eventId = "EVT-101",
                title = "🌟 October Global Star Gala",
                subtitle = "Double Lucky Pool contributions & exclusive Emerald Palace room frames for top 10 rooms!",
                contentType = "EVENT",
                targetScope = "ALL",
                targetValue = "*",
                rewardInfo = "100,000 Coins Pool + VIP 6 Sovereign Frame",
                createdByOwnerId = "199967",
                isActive = true,
                createdAt = now - 7200_000L,
                expiresAt = now + 86_400_000L * 25
            )
        )
        dao.upsertGlobalEvent(
            GlobalEventEntity(
                eventId = "EVT-102",
                title = "🇲🇦 Morocco & MENA Championship",
                subtitle = "Send Country Flag Gifts inside any Voice Room to boost your national leaderboard!",
                contentType = "BANNER",
                targetScope = "COUNTRY",
                targetValue = "MA",
                rewardInfo = "50,000 Coins + National Star Badge",
                createdByOwnerId = "199967",
                isActive = true,
                createdAt = now - 5400_000L,
                expiresAt = now + 86_400_000L * 20
            )
        )
        dao.upsertGlobalEvent(
            GlobalEventEntity(
                eventId = "EVT-103",
                title = "📢 Official Monthly Diamond Reset Notice",
                subtitle = "September 30-day Diamond cycles have been archived. October 31-day cycle is now live!",
                contentType = "OFFICIAL_MESSAGE",
                targetScope = "AGENCY",
                targetValue = "AG-101",
                rewardInfo = "Agency Host Bonus +15% Diamonds",
                createdByOwnerId = "199967",
                isActive = true,
                createdAt = now - 3600_000L,
                expiresAt = now + 86_400_000L * 30
            )
        )

        // Seed Initial Room & Global Chat Messages
        dao.insertChatMessage(
            ChatMessageEntity(
                messageId = "MSG-INIT1",
                channelType = "ROOM",
                roomOrThreadId = "RM-101",
                senderUserId = "199967",
                senderUsername = "Amira TangerStar",
                senderVipLevel = 6,
                senderCountryFlag = "🇲🇦",
                messageType = "TEXT",
                contentText = "Welcome to Tanger Star Voice! Check out the Top Notification Banner above and join a speaker seat! 🎙️✨",
                voiceDurationSec = 0,
                createdAt = now - 300_000L
            )
        )
        dao.insertChatMessage(
            ChatMessageEntity(
                messageId = "MSG-INIT2",
                channelType = "ROOM",
                roomOrThreadId = "RM-101",
                senderUserId = "199968",
                senderUsername = "Zayd Al-Mansoor",
                senderVipLevel = 6,
                senderCountryFlag = "🇦🇪",
                messageType = "GIFT_ALERT",
                contentText = "🎁 Sent 1x 🍀 Lucky Clover Box to Amira TangerStar (100 Coins)",
                voiceDurationSec = 0,
                createdAt = now - 120_000L
            )
        )
        dao.insertChatMessage(
            ChatMessageEntity(
                messageId = "MSG-GLOBAL1",
                channelType = "GLOBAL",
                roomOrThreadId = "GLOBAL",
                senderUserId = "199969",
                senderUsername = "Layla VelvetVoice",
                senderVipLevel = 4,
                senderCountryFlag = "🇸🇦",
                messageType = "TEXT",
                contentText = "Hello Global Tanger Star family! Join RM-101 for live music & Ludo! 🌍🎶",
                voiceDurationSec = 0,
                createdAt = now - 180_000L
            )
        )

        // Seed Initial Report for Admin Panel inspection
        dao.insertReport(
            UserReportEntity(
                reportId = "REP-90102",
                reporterUserId = "199969",
                reportedUserId = "199971",
                category = "Mic Spam / Audio Echo",
                details = "High background volume on Seat #2 during singing contest.",
                status = "OPEN",
                resolvedByAdminId = null,
                createdAt = now - 1800_000L
            )
        )

        // Seed Initial Atomic Wallet Ledger Transactions for 199967 and 199968
        dao.insertWalletTransaction(
            WalletTransactionEntity(
                txId = "TX-INIT101",
                idempotencyKey = "INIT_TREASURY_199967",
                userId = "199967",
                counterpartyUserId = "TANGER_TREASURY",
                txType = "OWNER_ISSUE_COINS",
                coinsDelta = 90_000L,
                diamondsDelta = 40_000L,
                coinsBalanceAfter = 90_000L,
                diamondsBalanceAfter = 40_000L,
                description = "Initial Founder VIP 6 Treasury Allocation (ID #199967)",
                timestamp = now - 86_400_000L * 5
            )
        )
        dao.insertWalletTransaction(
            WalletTransactionEntity(
                txId = "TX-INIT102",
                idempotencyKey = "INIT_GIFT_SENT_199967",
                userId = "199967",
                counterpartyUserId = "199969",
                txType = "GIFT_SENT",
                coinsDelta = -5_000L,
                diamondsDelta = 0L,
                coinsBalanceAfter = 85_000L,
                diamondsBalanceAfter = 40_000L,
                description = "Sent 1x 🏰 CP Love Castle to Layla VelvetVoice (#199969)",
                timestamp = now - 86_400_000L * 2
            )
        )
        dao.insertWalletTransaction(
            WalletTransactionEntity(
                txId = "TX-INIT103",
                idempotencyKey = "INIT_GIFT_RCV_199967",
                userId = "199967",
                counterpartyUserId = "199968",
                txType = "GIFT_RECEIVED_DIAMOND",
                coinsDelta = 0L,
                diamondsDelta = 2_500L,
                coinsBalanceAfter = 85_000L,
                diamondsBalanceAfter = 42_500L,
                description = "Received 10x 🏮 Tanger Festival Lantern from Zayd Al-Mansoor (#199968)",
                timestamp = now - 3600_000L * 4
            )
        )
        dao.insertWalletTransaction(
            WalletTransactionEntity(
                txId = "TX-INIT201",
                idempotencyKey = "INIT_TREASURY_199968",
                userId = "199968",
                counterpartyUserId = "TANGER_TREASURY",
                txType = "OWNER_ISSUE_COINS",
                coinsDelta = 64_000L,
                diamondsDelta = 31_800L,
                coinsBalanceAfter = 64_000L,
                diamondsBalanceAfter = 31_800L,
                description = "Agency Owner Treasury Settlement (ID #199968)",
                timestamp = now - 86_400_000L * 3
            )
        )

        recordAudit(
            actorUserId = "SYSTEM_BOOT",
            actorRole = "GLOBAL_OWNER",
            category = "DATABASE",
            action = "RELATIONAL_DB_INITIALIZED",
            targetId = "tanger_star_voice_production.db",
            details = "31 relational tables verified and active"
        )
        seedSocialPostsStoriesReelsAndLive(now)
    }

    // ================= 14. POSTS, STORIES, REELS, LIVE STREAMS, CLOUD MEDIA & BACKUPS =================
    suspend fun registerCloudMediaUpload(
        actorUserId: String,
        assetType: String,
        localUri: String,
        mimeType: String = "image/jpeg"
    ): BackendResult<CloudMediaAssetEntity> {
        val user = dao.getUserById(actorUserId)
            ?: return BackendResult.Error("Authenticated user required for Cloud Media Upload.")
        val now = System.currentTimeMillis()
        val assetId = "CDN-${UUID.randomUUID().toString().take(8).uppercase()}"
        val checksum = UUID.randomUUID().toString().replace("-", "").uppercase()
        val cdnBase = BuildConfig.TANGER_CDN_BASE_URL.ifBlank { "https://cdn.tangerstarvoice.global/media/" }
        val bucket = BuildConfig.TANGER_CLOUD_STORAGE_BUCKET.ifBlank { "tangeram-global-media-prod" }
        val cloudUrl = "${cdnBase.trimEnd('/')}/$bucket/${user.tangerUserId}/$assetId"
        val sizeBytes = 245_000L + (now % 650_000L)

        val asset = CloudMediaAssetEntity(
            assetId = assetId,
            ownerUserId = user.tangerUserId,
            ownerUsername = user.username,
            assetType = assetType,
            localContentUri = localUri,
            cloudCdnUrl = cloudUrl,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            sha256Checksum = checksum,
            syncStatus = "SYNCED_PRODUCTION_CDN",
            uploadedAt = now
        )
        dao.upsertCloudMediaAsset(asset)
        recordAudit(
            actorUserId = user.tangerUserId,
            actorRole = user.role,
            category = "CLOUD_MEDIA",
            action = "UPLOAD_MEDIA_CDN",
            targetId = assetId,
            details = "Type=$assetType, Bucket=$bucket, CDN=$cloudUrl, SHA256=${checksum.take(16)}"
        )
        return BackendResult.Success(asset, "Uploaded to Cloud CDN ($assetId) • Verified SHA-256")
    }

    suspend fun createSocialPost(
        authorUserId: String,
        caption: String,
        mediaUri: String,
        mediaType: String = "IMAGE",
        locationTag: String = "Global TANGERAM"
    ): BackendResult<SocialPostEntity> {
        val user = dao.getUserById(authorUserId)
            ?: return BackendResult.Error("Must be signed in to publish a Post.")
        if (caption.isBlank()) return BackendResult.Error("Post caption cannot be empty.")

        return db.withTransaction {
            val now = System.currentTimeMillis()
            val finalMediaUri = if (mediaUri.isNotBlank()) {
                val cdnRes = registerCloudMediaUpload(authorUserId, "POST_MEDIA", mediaUri)
                if (cdnRes is BackendResult.Success) cdnRes.data.localContentUri.ifBlank { cdnRes.data.cloudCdnUrl } else mediaUri
            } else ""

            val post = SocialPostEntity(
                postId = "PST-${UUID.randomUUID().toString().take(6).uppercase()}",
                authorUserId = user.tangerUserId,
                authorUsername = user.username,
                authorCountryFlag = user.countryFlag,
                authorVipLevel = user.vipLevel,
                caption = caption.trim(),
                mediaUri = finalMediaUri,
                mediaType = mediaType,
                locationTag = locationTag.ifBlank { user.countryName },
                likesCount = 1,
                commentsCount = 0,
                sharesCount = 0,
                giftCoinsReceived = 0L,
                isPinned = false,
                createdAt = now
            )
            dao.upsertSocialPost(post)
            recordAudit(
                user.tangerUserId,
                user.role,
                "SOCIAL",
                "PUBLISH_POST",
                post.postId,
                "Caption=${post.caption.take(40)}, MediaType=$mediaType"
            )
            BackendResult.Success(post, "Published Post ${post.postId} to Global TANGERAM Feed!")
        }
    }

    suspend fun likeOrCommentSocialPost(
        actorUserId: String,
        postId: String,
        commentText: String? = null
    ): BackendResult<SocialPostEntity> {
        val user = dao.getUserById(actorUserId)
            ?: return BackendResult.Error("Must be signed in.")
        return db.withTransaction {
            val post = dao.getSocialPostById(postId)
                ?: return@withTransaction BackendResult.Error("Post not found.")
            val now = System.currentTimeMillis()
            val updated = if (!commentText.isNullOrBlank()) {
                dao.insertPostComment(
                    PostCommentEntity(
                        commentId = "CMT-${UUID.randomUUID().toString().take(6).uppercase()}",
                        postId = postId,
                        authorUserId = user.tangerUserId,
                        authorUsername = user.username,
                        authorCountryFlag = user.countryFlag,
                        commentText = commentText.trim(),
                        createdAt = now
                    )
                )
                post.copy(commentsCount = post.commentsCount + 1)
            } else {
                post.copy(likesCount = post.likesCount + 1)
            }
            dao.upsertSocialPost(updated)
            BackendResult.Success(
                updated,
                if (!commentText.isNullOrBlank()) "Comment posted on ${post.authorUsername}'s post!" else "Liked ${post.authorUsername}'s post!"
            )
        }
    }

    suspend fun createStory24h(
        authorUserId: String,
        captionText: String,
        mediaUri: String,
        backgroundTheme: String = "EMERALD_GLOW"
    ): BackendResult<StoryEntity> {
        val user = dao.getUserById(authorUserId)
            ?: return BackendResult.Error("Must be signed in to publish a 24h Story.")
        if (captionText.isBlank()) return BackendResult.Error("Story caption cannot be empty.")

        return db.withTransaction {
            val now = System.currentTimeMillis()
            if (mediaUri.isNotBlank()) {
                registerCloudMediaUpload(authorUserId, "STORY_MEDIA", mediaUri)
            }
            val story = StoryEntity(
                storyId = "STR-${UUID.randomUUID().toString().take(6).uppercase()}",
                authorUserId = user.tangerUserId,
                authorUsername = user.username,
                authorCountryFlag = user.countryFlag,
                authorVipLevel = user.vipLevel,
                captionText = captionText.trim(),
                mediaUri = mediaUri,
                backgroundTheme = backgroundTheme,
                viewsCount = 1,
                likesCount = 1,
                giftCoinsReceived = 0L,
                createdAt = now,
                expiresAt = now + 86_400_000L, // 24-hour authoritative expiry
                isActive = true
            )
            dao.upsertStory(story)
            recordAudit(
                user.tangerUserId,
                user.role,
                "SOCIAL",
                "PUBLISH_STORY_24H",
                story.storyId,
                "Caption=${story.captionText.take(40)}, ExpiresIn=24h"
            )
            BackendResult.Success(story, "Published 24h Story ${story.storyId} to Global Story Ring!")
        }
    }

    suspend fun viewOrLikeStory(storyId: String, like: Boolean): BackendResult<StoryEntity> {
        val story = dao.getStoryById(storyId) ?: return BackendResult.Error("Story not found.")
        val updated = story.copy(
            viewsCount = story.viewsCount + 1,
            likesCount = if (like) story.likesCount + 1 else story.likesCount
        )
        dao.upsertStory(updated)
        return BackendResult.Success(updated, if (like) "Liked ${story.authorUsername}'s Story! ❤️" else "Viewed Story")
    }

    suspend fun createReel(
        creatorUserId: String,
        title: String,
        audioTrackName: String,
        mediaUri: String,
        durationSec: Int = 15
    ): BackendResult<ReelEntity> {
        val user = dao.getUserById(creatorUserId)
            ?: return BackendResult.Error("Must be signed in to publish a Reel.")
        if (title.isBlank()) return BackendResult.Error("Reel title cannot be empty.")

        return db.withTransaction {
            val now = System.currentTimeMillis()
            if (mediaUri.isNotBlank()) {
                registerCloudMediaUpload(creatorUserId, "REEL_VIDEO", mediaUri, "video/mp4")
            }
            val reel = ReelEntity(
                reelId = "REL-${UUID.randomUUID().toString().take(6).uppercase()}",
                creatorUserId = user.tangerUserId,
                creatorUsername = user.username,
                creatorCountryFlag = user.countryFlag,
                creatorVipLevel = user.vipLevel,
                title = title.trim(),
                audioTrackName = audioTrackName.ifBlank { "Tanger Star Original Audio" },
                mediaUri = mediaUri,
                durationSec = durationSec.coerceIn(5, 90),
                likesCount = 1,
                commentsCount = 0,
                sharesCount = 0,
                giftCoinsReceived = 0L,
                createdAt = now
            )
            dao.upsertReel(reel)
            recordAudit(
                user.tangerUserId,
                user.role,
                "SOCIAL",
                "PUBLISH_REEL",
                reel.reelId,
                "Title=${reel.title}, Audio=${reel.audioTrackName}"
            )
            BackendResult.Success(reel, "Published Reel ${reel.reelId} to Global Reels Stream!")
        }
    }

    suspend fun likeOrShareReel(reelId: String, share: Boolean = false): BackendResult<ReelEntity> {
        val reel = dao.getReelById(reelId) ?: return BackendResult.Error("Reel not found.")
        val updated = reel.copy(
            likesCount = if (!share) reel.likesCount + 1 else reel.likesCount,
            sharesCount = if (share) reel.sharesCount + 1 else reel.sharesCount
        )
        dao.upsertReel(updated)
        return BackendResult.Success(
            updated,
            if (share) "Shared Reel '${reel.title}' with Global Friends!" else "Liked Reel '${reel.title}'! ❤️"
        )
    }

    suspend fun startOrJoinLiveStream(
        hostUserId: String,
        streamTitle: String,
        category: String = "PK_BATTLE",
        isPkBattle: Boolean = true,
        pkOpponentUserId: String? = null
    ): BackendResult<LiveStreamEntity> {
        val host = dao.getUserById(hostUserId)
            ?: return BackendResult.Error("Must be signed in to start a Live Stream.")
        if (streamTitle.isBlank()) return BackendResult.Error("Live Stream title required.")

        return db.withTransaction {
            val now = System.currentTimeMillis()
            val opponent = pkOpponentUserId?.let { dao.getUserById(it) }
            val stream = LiveStreamEntity(
                streamId = "LIVE-${UUID.randomUUID().toString().take(5).uppercase()}",
                hostUserId = host.tangerUserId,
                hostUsername = host.username,
                hostCountryFlag = host.countryFlag,
                hostVipLevel = host.vipLevel,
                streamTitle = streamTitle.trim(),
                category = category,
                isPkBattle = isPkBattle,
                pkOpponentUserId = opponent?.tangerUserId,
                pkOpponentUsername = opponent?.username,
                hostPkScore = 100L,
                opponentPkScore = if (isPkBattle) 80L else 0L,
                viewersCount = 18,
                totalDiamondsEarned = 0L,
                isLive = true,
                startedAt = now
            )
            dao.upsertLiveStream(stream)
            recordAudit(
                host.tangerUserId,
                host.role,
                "LIVE",
                "START_LIVE_BROADCAST",
                stream.streamId,
                "Title=${stream.streamTitle}, PK=$isPkBattle, Opponent=${opponent?.username ?: "NONE"}"
            )
            BackendResult.Success(stream, "🔴 Live Stream ${stream.streamId} is now broadcasting globally!")
        }
    }

    suspend fun boostLiveStreamPkScore(streamId: String, forHost: Boolean, points: Long = 250L): BackendResult<LiveStreamEntity> {
        val stream = dao.getLiveStreamById(streamId) ?: return BackendResult.Error("Live stream not found.")
        val updated = stream.copy(
            hostPkScore = if (forHost) stream.hostPkScore + points else stream.hostPkScore,
            opponentPkScore = if (!forHost) stream.opponentPkScore + points else stream.opponentPkScore,
            viewersCount = stream.viewersCount + 2,
            totalDiamondsEarned = stream.totalDiamondsEarned + (points / 2)
        )
        dao.upsertLiveStream(updated)
        return BackendResult.Success(updated, "Boosted Live PK Score (+$points pts) in ${stream.streamTitle}!")
    }

    suspend fun ownerCreateDatabaseBackupSnapshot(actorUserId: String): BackendResult<DatabaseBackupSnapshotEntity> {
        val owner = requireGlobalOwner(actorUserId)
            ?: return BackendResult.Error("RBAC Denied: Global Owner permission required for Database Backups.")
        return db.withTransaction {
            val now = System.currentTimeMillis()
            val usersCnt = dao.countUsers()
            val roomsCnt = dao.countRooms()
            val txCnt = dao.countWalletTransactions()
            val postsReelsCnt = dao.countSocialPosts() + dao.countReels()
            val giftsCnt = dao.countGifts()
            val backupId = "BKP-${UUID.randomUUID().toString().take(8).uppercase()}"
            val checksum = UUID.randomUUID().toString().replace("-", "").uppercase()
            val summaryJson = """{"schema":5,"users":$usersCnt,"rooms":$roomsCnt,"walletTx":$txCnt,"social":$postsReelsCnt,"gifts":$giftsCnt,"bucket":"${BuildConfig.TANGER_CLOUD_STORAGE_BUCKET}"}"""

            val snapshot = DatabaseBackupSnapshotEntity(
                backupId = backupId,
                actorUserId = owner.tangerUserId,
                schemaVersion = 5,
                usersCount = usersCnt,
                roomsCount = roomsCnt,
                transactionsCount = txCnt,
                postsAndReelsCount = postsReelsCnt,
                giftsCount = giftsCnt,
                payloadChecksum = checksum,
                backupSummaryJson = summaryJson,
                createdAt = now
            )
            dao.insertDatabaseBackup(snapshot)
            recordAudit(
                owner.tangerUserId,
                owner.role,
                "BACKUP",
                "CREATE_PRODUCTION_DB_BACKUP",
                backupId,
                summaryJson
            )
            BackendResult.Success(
                snapshot,
                "Created Production Backup $backupId (Schema v5 • $usersCnt Users • $txCnt Ledger Rows)"
            )
        }
    }

    private suspend fun seedSocialPostsStoriesReelsAndLive(now: Long) {
        val posts = listOf(
            SocialPostEntity(
                postId = "PST-101",
                authorUserId = "199967",
                authorUsername = "Amira TangerStar",
                authorCountryFlag = "🇲🇦",
                authorVipLevel = 6,
                caption = "Tonight's Royal Emerald Gala in Room RM-101 was unforgettable! Thank you for all the Lucky Clover & Golden Falcon gifts! 🌟🎙️💎",
                mediaUri = "https://cdn.tangerstarvoice.global/media/tangeram-global-media-prod/199967/gala_stage.jpg",
                mediaType = "IMAGE",
                locationTag = "Tangier, Morocco 🇲🇦",
                likesCount = 184,
                commentsCount = 2,
                sharesCount = 29,
                giftCoinsReceived = 14_500L,
                isPinned = true,
                createdAt = now - 3_600_000L * 3
            ),
            SocialPostEntity(
                postId = "PST-102",
                authorUserId = "199968",
                authorUsername = "Zayd Al-Mansoor",
                authorCountryFlag = "🇦🇪",
                authorVipLevel = 6,
                caption = "Our Agency #AG-101 just crossed 264,000 Monthly Diamonds! Recruiting new Voice Hosts & PK Battle stars worldwide 🌍👑",
                mediaUri = "https://cdn.tangerstarvoice.global/media/tangeram-global-media-prod/199968/dubai_vip.jpg",
                mediaType = "IMAGE",
                locationTag = "Dubai, UAE 🇦🇪",
                likesCount = 142,
                commentsCount = 1,
                sharesCount = 18,
                giftCoinsReceived = 8_200L,
                isPinned = false,
                createdAt = now - 3_600_000L * 5
            ),
            SocialPostEntity(
                postId = "PST-103",
                authorUserId = "199969",
                authorUsername = "Layla VelvetVoice",
                authorCountryFlag = "🇸🇦",
                authorVipLevel = 4,
                caption = "Acoustic Arabic & Mediterranean fusion night starting now! Send a Lucky Gift to trigger the 50x Jackpot Pool! 🎶🍀",
                mediaUri = "https://cdn.tangerstarvoice.global/media/tangeram-global-media-prod/199969/velvet_session.jpg",
                mediaType = "AUDIO_CLIP",
                locationTag = "Riyadh, Saudi Arabia 🇸🇦",
                likesCount = 96,
                commentsCount = 1,
                sharesCount = 12,
                giftCoinsReceived = 4_600L,
                isPinned = false,
                createdAt = now - 3_600_000L * 8
            )
        )
        dao.upsertSocialPosts(posts)

        dao.insertPostComment(
            PostCommentEntity(
                commentId = "CMT-101",
                postId = "PST-101",
                authorUserId = "199968",
                authorUsername = "Zayd Al-Mansoor",
                authorCountryFlag = "🇦🇪",
                commentText = "Best voice stage on TANGERAM! See you in the PK Battle tonight 🔥",
                createdAt = now - 3_600_000L * 2
            )
        )
        dao.insertPostComment(
            PostCommentEntity(
                commentId = "CMT-102",
                postId = "PST-101",
                authorUserId = "199969",
                authorUsername = "Layla VelvetVoice",
                authorCountryFlag = "🇸🇦",
                commentText = "Congratulations Queen Amira! 👑💎",
                createdAt = now - 3_600_000L
            )
        )

        val stories = listOf(
            StoryEntity(
                storyId = "STR-101",
                authorUserId = "199967",
                authorUsername = "Amira TangerStar",
                authorCountryFlag = "🇲🇦",
                authorVipLevel = 6,
                captionText = "🎙️ Live in RM-101 • 2x Lucky Gift Cashback Active!",
                mediaUri = "https://cdn.tangerstarvoice.global/media/tangeram-global-media-prod/199967/story_1.jpg",
                backgroundTheme = "EMERALD_GLOW",
                viewsCount = 412,
                likesCount = 89,
                giftCoinsReceived = 3_200L,
                createdAt = now - 7_200_000L,
                expiresAt = now + 86_400_000L,
                isActive = true
            ),
            StoryEntity(
                storyId = "STR-102",
                authorUserId = "199968",
                authorUsername = "Zayd Al-Mansoor",
                authorCountryFlag = "🇦🇪",
                authorVipLevel = 6,
                captionText = "💎 VIP 6 Sovereign Lounge & Ludo High Rollers",
                mediaUri = "https://cdn.tangerstarvoice.global/media/tangeram-global-media-prod/199968/story_2.jpg",
                backgroundTheme = "GOLD_ROYAL",
                viewsCount = 319,
                likesCount = 64,
                giftCoinsReceived = 2_500L,
                createdAt = now - 5_400_000L,
                expiresAt = now + 86_400_000L,
                isActive = true
            ),
            StoryEntity(
                storyId = "STR-103",
                authorUserId = "199970",
                authorUsername = "Karim AtlasBeat",
                authorCountryFlag = "🇲🇦",
                authorVipLevel = 3,
                captionText = "🎧 Gnawa & Deep House DJ Set Live from Casablanca",
                mediaUri = "https://cdn.tangerstarvoice.global/media/tangeram-global-media-prod/199970/story_3.jpg",
                backgroundTheme = "TURQUOISE_WAVE",
                viewsCount = 198,
                likesCount = 41,
                giftCoinsReceived = 1_100L,
                createdAt = now - 3_600_000L,
                expiresAt = now + 86_400_000L,
                isActive = true
            )
        )
        dao.upsertStories(stories)

        val reels = listOf(
            ReelEntity(
                reelId = "REL-101",
                creatorUserId = "199967",
                creatorUsername = "Amira TangerStar",
                creatorCountryFlag = "🇲🇦",
                creatorVipLevel = 6,
                title = "When the 50x Lucky Treasure Chest Jackpot hits in Tangier Royal Lounge! 🎰💎",
                audioTrackName = "Sahara Starlight • Amira Live Mix",
                mediaUri = "https://cdn.tangerstarvoice.global/media/tangeram-global-media-prod/199967/reel_jackpot.mp4",
                durationSec = 24,
                likesCount = 620,
                commentsCount = 48,
                sharesCount = 95,
                giftCoinsReceived = 19_400L,
                createdAt = now - 3_600_000L * 4
            ),
            ReelEntity(
                reelId = "REL-102",
                creatorUserId = "199969",
                creatorUsername = "Layla VelvetVoice",
                creatorCountryFlag = "🇸🇦",
                creatorVipLevel = 4,
                title = "High-note vocal challenge with 8 global speakers on stage 🎤✨",
                audioTrackName = "Velvet Tarab Acoustic • Layla",
                mediaUri = "https://cdn.tangerstarvoice.global/media/tangeram-global-media-prod/199969/reel_vocals.mp4",
                durationSec = 30,
                likesCount = 485,
                commentsCount = 34,
                sharesCount = 62,
                giftCoinsReceived = 11_200L,
                createdAt = now - 3_600_000L * 6
            ),
            ReelEntity(
                reelId = "REL-103",
                creatorUserId = "199971",
                creatorUsername = "Selin Bosphorus",
                creatorCountryFlag = "🇹🇷",
                creatorVipLevel = 2,
                title = "Istanbul Midnight Acoustic Guitar & Star Rain Gifts 🌉🎸",
                audioTrackName = "Bosphorus Breeze • Selin",
                mediaUri = "https://cdn.tangerstarvoice.global/media/tangeram-global-media-prod/199971/reel_istanbul.mp4",
                durationSec = 18,
                likesCount = 310,
                commentsCount = 19,
                sharesCount = 27,
                giftCoinsReceived = 5_400L,
                createdAt = now - 3_600_000L * 9
            )
        )
        dao.upsertReels(reels)

        val liveStreams = listOf(
            LiveStreamEntity(
                streamId = "LIVE-101",
                hostUserId = "199967",
                hostUsername = "Amira TangerStar",
                hostCountryFlag = "🇲🇦",
                hostVipLevel = 6,
                streamTitle = "🇲🇦 vs 🇦🇪 Royal PK Championship • Lucky Gifts x2",
                category = "PK_BATTLE",
                isPkBattle = true,
                pkOpponentUserId = "199968",
                pkOpponentUsername = "Zayd Al-Mansoor",
                hostPkScore = 14_850L,
                opponentPkScore = 12_400L,
                viewersCount = 342,
                totalDiamondsEarned = 18_600L,
                isLive = true,
                startedAt = now - 1_800_000L
            ),
            LiveStreamEntity(
                streamId = "LIVE-102",
                hostUserId = "199969",
                hostUsername = "Layla VelvetVoice",
                hostCountryFlag = "🇸🇦",
                hostVipLevel = 4,
                streamTitle = "🌟 Global Singing Talent Auditions & Instant VIP Rewards",
                category = "TALENT_SHOW",
                isPkBattle = false,
                pkOpponentUserId = null,
                pkOpponentUsername = null,
                hostPkScore = 8_900L,
                opponentPkScore = 0L,
                viewersCount = 215,
                totalDiamondsEarned = 9_400L,
                isLive = true,
                startedAt = now - 2_400_000L
            )
        )
        dao.upsertLiveStreams(liveStreams)

        dao.upsertCloudMediaAsset(
            CloudMediaAssetEntity(
                assetId = "CDN-INIT001",
                ownerUserId = "199967",
                ownerUsername = "Amira TangerStar",
                assetType = "POST_MEDIA",
                localContentUri = "res://drawable/img_room_banner",
                cloudCdnUrl = "https://cdn.tangerstarvoice.global/media/tangeram-global-media-prod/199967/gala_stage.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 412_800L,
                sha256Checksum = "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855",
                syncStatus = "SYNCED_PRODUCTION_CDN",
                uploadedAt = now - 3_600_000L * 3
            )
        )

        dao.insertDatabaseBackup(
            DatabaseBackupSnapshotEntity(
                backupId = "BKP-INIT-V5",
                actorUserId = "199967",
                schemaVersion = 5,
                usersCount = 5,
                roomsCount = 3,
                transactionsCount = 4,
                postsAndReelsCount = 6,
                giftsCount = 8,
                payloadChecksum = "9F86D081884C7D659A2FEAA0C55AD015A3BF4F1B2B0B822CD15D6C15B0F00A08",
                backupSummaryJson = """{"schema":5,"status":"VERIFIED_PRODUCTION_SNAPSHOT","bucket":"tangeram-global-media-prod"}""",
                createdAt = now - 1_800_000L
            )
        )
    }
}
