package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["tangerUserId"], unique = true),
        Index(value = ["username"], unique = true),
        Index(value = ["phoneNumber"], unique = false),
        Index(value = ["email"], unique = false)
    ]
)
data class UserEntity(
    @PrimaryKey val tangerUserId: String, // Sequential permanent TANGERAM ID starting from "199967"
    val username: String, // Unique handle, e.g., "amira_star"
    val phoneNumber: String,
    val countryCode: String, // e.g., "MA", "AE", "SA", "US", "TR", "EG"
    val countryName: String,
    val countryFlag: String,
    val gender: String, // "Male", "Female", "Other"
    val dob: String, // YYYY-MM-DD
    val bio: String,
    val avatarUri: String,
    val authProvider: String, // "PHONE_OTP", "EMAIL", "GOOGLE", "FACEBOOK"
    val role: String, // "USER", "HOST", "AGENCY_OWNER", "ADMIN", "GLOBAL_OWNER"
    val isOnline: Boolean,
    val isSuspended: Boolean,
    val isBanned: Boolean,
    val coinsBalance: Long,
    val diamondsBalance: Long,
    val totalCoinsSpent: Long,
    val totalDiamondsEarned: Long,
    val vipLevel: Int, // 0..6
    val userLevel: Int, // 1..100
    val xpPoints: Long,
    val agencyId: String?,
    val cpPartnerUserId: String?,
    val followersCount: Int,
    val followingCount: Int,
    val createdAt: Long,
    val fullName: String = username,
    val email: String = "",
    val passwordHash: String = "",
    val isOnboardingCompleted: Boolean = true,
    val allowDirectMessages: Boolean = true,
    val showOnlineStatus: Boolean = true
)

@Entity(tableName = "user_id_sequence")
data class UserSequenceCounterEntity(
    @PrimaryKey val counterKey: String = "TANGERAM_USER_ID_SEQ",
    val nextAvailableId: Long = 199967L,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "otp_challenges")
data class OtpChallengeEntity(
    @PrimaryKey val phoneKey: String, // e.g. "+212-600112233"
    val otpCode: String,
    val expiresAt: Long,
    val resendAvailableAt: Long,
    val requestCountWindow: Int,
    val windowStartedAt: Long,
    val verified: Boolean
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val sessionToken: String,
    val tangerUserId: String,
    val deviceInfo: String,
    val createdAt: Long,
    val expiresAt: Long,
    val isActive: Boolean
)

@Entity(
    tableName = "follows",
    primaryKeys = ["followerUserId", "targetUserId"]
)
data class FollowEntity(
    val followerUserId: String,
    val targetUserId: String,
    val createdAt: Long
)

@Entity(
    tableName = "blocks",
    primaryKeys = ["blockerUserId", "blockedUserId"]
)
data class BlockEntity(
    val blockerUserId: String,
    val blockedUserId: String,
    val reason: String,
    val createdAt: Long
)

@Entity(tableName = "user_reports")
data class UserReportEntity(
    @PrimaryKey val reportId: String,
    val reporterUserId: String,
    val reportedUserId: String,
    val category: String,
    val details: String,
    val status: String, // "OPEN", "RESOLVED", "DISMISSED"
    val resolvedByAdminId: String?,
    val createdAt: Long
)

@Entity(tableName = "voice_rooms")
data class VoiceRoomEntity(
    @PrimaryKey val roomId: String,
    val roomTitle: String,
    val roomTopic: String,
    val countryFlag: String,
    val ownerUserId: String,
    val ownerUsername: String,
    val isPrivate: Boolean,
    val pinCode: String?,
    val seatCount: Int, // 8 or 12 seats
    val themeName: String, // "Emerald Palace", "Turquoise Oasis", "Royal Star", "Cyber Neon"
    val isChatLocked: Boolean,
    val activeMusicTrackTitle: String?,
    val activeMusicArtist: String?,
    val isMusicPlaying: Boolean,
    val musicVolume: Float,
    val musicPositionSec: Int,
    val totalRoomHeat: Long,
    val onlineCount: Int,
    val createdAt: Long
)

@Entity(
    tableName = "room_seats",
    primaryKeys = ["roomId", "seatIndex"]
)
data class RoomSeatEntity(
    val roomId: String,
    val seatIndex: Int, // 0..7
    val occupantUserId: String?,
    val occupantUsername: String?,
    val occupantAvatar: String?,
    val occupantVipLevel: Int,
    val occupantRole: String, // "OWNER", "ADMIN", "CO_HOST", "SPEAKER"
    val isMutedBySelf: Boolean,
    val isMutedByOwner: Boolean,
    val isSeatLocked: Boolean,
    val speakingAudioDb: Float
)

@Entity(
    tableName = "room_roles",
    primaryKeys = ["roomId", "userId"]
)
data class RoomRoleEntity(
    val roomId: String,
    val userId: String,
    val username: String,
    val roleType: String, // "OWNER", "ADMIN", "CO_HOST"
    val assignedByUserId: String,
    val assignedAt: Long
)

@Entity(tableName = "room_top_notifications")
data class RoomTopNotificationEntity(
    @PrimaryKey val notificationId: String,
    val roomId: String,
    val creatorUserId: String,
    val creatorRole: String, // Verified "OWNER", "ADMIN", or "GLOBAL_OWNER"
    val title: String,
    val messageText: String,
    val iconName: String, // "STAR", "CROWN", "GIFT", "FIRE", "MEGAPHONE"
    val bannerStyle: String, // "TURQUOISE_GOLD", "EMERALD_ROYAL", "RUBY_ALERT"
    val durationSeconds: Int,
    val createdAt: Long,
    val expiresAt: Long,
    val isActive: Boolean
)

@Entity(
    tableName = "wallet_transactions",
    indices = [Index(value = ["idempotencyKey"], unique = true)]
)
data class WalletTransactionEntity(
    @PrimaryKey val txId: String,
    val idempotencyKey: String,
    val userId: String,
    val counterpartyUserId: String?,
    val txType: String, // "OWNER_ISSUE_COINS", "GIFT_SENT", "GIFT_RECEIVED_DIAMOND", "LUCKY_REWARD", "VIP_PURCHASE", "GAME_ENTRY", "GAME_REWARD", "RANKING_REWARD"
    val coinsDelta: Long,
    val diamondsDelta: Long,
    val coinsBalanceAfter: Long,
    val diamondsBalanceAfter: Long,
    val description: String,
    val timestamp: Long
)

@Entity(
    tableName = "diamond_monthly_cycles",
    primaryKeys = ["userId", "yearMonth"]
)
data class DiamondMonthlyCycleEntity(
    val userId: String,
    val yearMonth: String, // e.g., "2026-10", "2026-09"
    val cycleStartEpoch: Long,
    val cycleEndEpoch: Long,
    val daysInMonth: Int, // 28, 29, 30, or 31
    val monthlyTargetDiamonds: Long,
    val earnedDiamondsInCycle: Long,
    val validHostDays: Int,
    val isArchived: Boolean,
    val lastUpdatedAt: Long
)

@Entity(tableName = "gifts")
data class GiftEntity(
    @PrimaryKey val giftId: String,
    val name: String,
    val category: String, // "Gift", "Event Gift", "Lucky Gift", "Lucky Draw", "CP", "Country/Flag Gift"
    val iconEmoji: String,
    val animationEffect: String, // "BURST_GOLD", "TURQUOISE_WAVE", "STAR_RAIN", "HEART_CP", "LUCKY_SPIN"
    val coinPrice: Long,
    val diamondReturnRatePercent: Int, // e.g. 60% of coins converted to receiver diamonds
    val isActive: Boolean,
    val updatedAt: Long,
    // Per-Gift Authoritative Lucky Gift Engine & Timing/Backup Configuration
    val triggerCount: Long = 5L, // Every N sends triggers the Lucky Reward
    val currentHitCount: Long = 0L, // Current server counter for this gift
    val rewardCoins: Long = 200L, // Base Lucky Reward Coins
    val cashbackCoins: Long = 30L, // Instant Cashback Coins on Lucky trigger (or per send if configured)
    val expBonus: Long = 50L, // EXP awarded on send / trigger
    val maxRewardCoins: Long = 1_000L, // Maximum reward cap per trigger
    val activationLimit: Long = 100L, // Max times this Lucky Gift can trigger before reset (0 = unlimited)
    val currentActivations: Long = 0L, // Current activation count
    val startTimeMillis: Long = 0L, // Scheduled window start epoch ms (0 = immediate)
    val endTimeMillis: Long = 0L, // Scheduled window end epoch ms (0 = no expiry)
    val cooldownSeconds: Int = 5, // Cooldown seconds between reward triggers for this gift
    val repeatTimingSeconds: Int = 3600, // Repeat / auto-cycle reset interval in seconds (0 = manual)
    val isPaused: Boolean = false, // Pause/Resume Lucky trigger evaluation without deleting gift
    val lastTriggeredAt: Long = 0L, // Last reward trigger epoch ms
    val lastCycleResetAt: Long = 0L, // Last repeat-cycle reset epoch ms
    val backupTimingNotes: String = "Auto-Backup & Repeat Timing Active" // Owner configurable backup/timing profile
)

@Entity(
    tableName = "gift_transactions",
    indices = [Index(value = ["idempotencyKey"], unique = true)]
)
data class GiftTransactionEntity(
    @PrimaryKey val giftTxId: String,
    val idempotencyKey: String,
    val senderUserId: String,
    val senderUsername: String,
    val receiverUserId: String,
    val receiverUsername: String,
    val roomId: String,
    val giftId: String,
    val giftName: String,
    val giftCategory: String,
    val quantity: Int,
    val unitCoinPrice: Long,
    val totalCoinsSpent: Long,
    val receiverDiamondsEarned: Long,
    val timestamp: Long,
    val luckyRewardCoins: Long = 0L,
    val cashbackCoins: Long = 0L,
    val expGranted: Long = 0L,
    val giftCounterAfter: Long = 0L,
    val globalCounterAfter: Long = 0L
)

@Entity(tableName = "lucky_gift_state")
data class LuckyGiftStateEntity(
    @PrimaryKey val counterId: String = "GLOBAL_LUCKY_COUNTER",
    val globalPoolCoins: Long,
    val globalHitCount: Long,
    val milestone1Threshold: Long,
    val milestone1RewardCoins: Long,
    val milestone2Threshold: Long,
    val milestone2RewardCoins: Long,
    val jackpotThreshold: Long,
    val jackpotRewardCoins: Long,
    val updatedAt: Long,
    val isGlobalLuckyPaused: Boolean = false,
    val totalLuckyRewardsDistributed: Long = 0L,
    val totalCashbackDistributed: Long = 0L,
    val totalExpDistributed: Long = 0L
)

@Entity(
    tableName = "lucky_gift_audit_logs",
    indices = [Index(value = ["milestoneTriggerKey"], unique = true)]
)
data class LuckyGiftAuditEntity(
    @PrimaryKey val auditId: String,
    val milestoneTriggerKey: String, // Prevents duplicate reward for same counter milestone hit
    val userId: String,
    val username: String,
    val receiverUserId: String = "",
    val receiverUsername: String = "",
    val giftId: String,
    val giftName: String = "",
    val giftCounterAtHit: Long = 0L,
    val globalCounterValueAtHit: Long,
    val milestoneReached: String,
    val rewardCoinsGranted: Long,
    val cashbackCoinsGranted: Long = 0L,
    val expGranted: Long = 0L,
    val timestamp: Long
)

@Entity(tableName = "vip_configs")
data class VipConfigEntity(
    @PrimaryKey val vipLevel: Int, // 1..6
    val vipTitle: String,
    val requiredCoins: Long,
    val badgeName: String,
    val frameStyle: String,
    val entryAnimation: String,
    val dailyBonusCoins: Long
)

@Entity(tableName = "vip_history")
data class VipHistoryEntity(
    @PrimaryKey val historyId: String,
    val userId: String,
    val vipLevel: Int,
    val vipTitle: String,
    val coinsQualifiedWith: Long,
    val actionType: String, // "AUTO_QUALIFIED", "PURCHASED", "OWNER_GRANTED"
    val timestamp: Long
)

@Entity(tableName = "agencies")
data class AgencyEntity(
    @PrimaryKey val agencyId: String,
    val agencyName: String,
    val agencyCode: String,
    val ownerUserId: String,
    val ownerUsername: String,
    val countryFlag: String,
    val description: String,
    val memberCount: Int,
    val hostCount: Int,
    val dailyDiamondTarget: Long,
    val currentDailyDiamonds: Long,
    val monthlyDiamondTarget: Long,
    val currentMonthlyDiamonds: Long,
    val commissionPercent: Int,
    val createdAt: Long
)

@Entity(tableName = "agency_members")
data class AgencyMemberEntity(
    @PrimaryKey val membershipId: String,
    val agencyId: String,
    val userId: String,
    val username: String,
    val memberRole: String, // "AGENCY_OWNER", "HOST", "MEMBER"
    val dailyTargetDiamonds: Long,
    val dailyEarnedDiamonds: Long,
    val monthlyTargetDiamonds: Long,
    val monthlyEarnedDiamonds: Long,
    val joinedAt: Long
)

@Entity(tableName = "agency_invitations")
data class AgencyInvitationEntity(
    @PrimaryKey val inviteId: String,
    val agencyId: String,
    val agencyName: String,
    val invitedUserId: String,
    val invitedByUserId: String,
    val roleOffered: String, // "HOST" or "MEMBER"
    val status: String, // "PENDING", "ACCEPTED", "DECLINED"
    val createdAt: Long
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val messageId: String,
    val channelType: String, // "GLOBAL", "ROOM", "DIRECT"
    val roomOrThreadId: String, // "GLOBAL", roomId, or sorted pair "TS-100001_TS-100002"
    val senderUserId: String,
    val senderUsername: String,
    val senderVipLevel: Int,
    val senderCountryFlag: String,
    val messageType: String, // "TEXT", "VOICE_NOTE", "GIFT_ALERT", "SYSTEM"
    val contentText: String,
    val voiceDurationSec: Int,
    val createdAt: Long
)

@Entity(tableName = "call_sessions")
data class CallSessionEntity(
    @PrimaryKey val callId: String,
    val callerUserId: String,
    val callerUsername: String,
    val receiverUserId: String,
    val receiverUsername: String,
    val callType: String, // "VOICE_CALL", "VIDEO_CALL"
    val status: String, // "RINGING", "CONNECTED", "ENDED"
    val durationSeconds: Int,
    val startedAt: Long
)

@Entity(tableName = "game_matches")
data class GameMatchEntity(
    @PrimaryKey val matchId: String,
    val gameType: String, // "LUDO_STAR", "TREASURE_DICE", "ROYAL_ROULETTE"
    val roomId: String?,
    val entryFeeCoins: Long,
    val rewardPoolCoins: Long,
    val player1Id: String,
    val player1Name: String,
    val player1Pos: Int, // Ludo board track 0..56
    val player2Id: String,
    val player2Name: String,
    val player2Pos: Int, // Ludo board track 0..56
    val currentTurnUserId: String,
    val lastDiceRoll: Int,
    val turnNumber: Int,
    val status: String, // "ACTIVE", "COMPLETED"
    val winnerUserId: String?,
    val winnerUsername: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "global_events")
data class GlobalEventEntity(
    @PrimaryKey val eventId: String,
    val title: String,
    val subtitle: String,
    val contentType: String, // "EVENT", "BANNER", "OFFICIAL_MESSAGE", "GLOBAL_ANNOUNCEMENT", "PROMO"
    val targetScope: String, // "ALL", "COUNTRY", "AGENCY", "ROOM", "VIP", "USER"
    val targetValue: String, // e.g. "*", "MA", "AG-101", "RM-101", "VIP3", "TS-100001"
    val rewardInfo: String,
    val createdByOwnerId: String,
    val isActive: Boolean,
    val createdAt: Long,
    val expiresAt: Long
)

@Entity(
    tableName = "ranking_reward_claims",
    indices = [Index(value = ["rewardClaimKey"], unique = true)]
)
data class RankingRewardClaimEntity(
    @PrimaryKey val claimId: String,
    val rewardClaimKey: String, // Duplicate protection key: category + period + userId
    val category: String,
    val rankPosition: Int,
    val userId: String,
    val username: String,
    val rewardCoins: Long,
    val timestamp: Long
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val logId: String,
    val actorUserId: String,
    val actorRole: String,
    val actionCategory: String, // "FINANCE", "ADMIN_RBAC", "GIFT", "LUCKY_GIFT", "ROOM_BANNER", "MODERATION", "GAME_SERVER", "DIAMOND_CYCLE", "SOCIAL", "LIVE", "BACKUP", "CLOUD_MEDIA"
    val actionName: String,
    val targetEntityId: String,
    val detailsJson: String,
    val ipOrDevice: String,
    val timestamp: Long
)

@Entity(tableName = "social_posts")
data class SocialPostEntity(
    @PrimaryKey val postId: String,
    val authorUserId: String,
    val authorUsername: String,
    val authorCountryFlag: String,
    val authorVipLevel: Int,
    val caption: String,
    val mediaUri: String,
    val mediaType: String, // "IMAGE", "VIDEO", "AUDIO_CLIP", "TEXT_STATUS"
    val locationTag: String,
    val likesCount: Int,
    val commentsCount: Int,
    val sharesCount: Int,
    val giftCoinsReceived: Long,
    val isPinned: Boolean,
    val createdAt: Long
)

@Entity(tableName = "post_comments")
data class PostCommentEntity(
    @PrimaryKey val commentId: String,
    val postId: String,
    val authorUserId: String,
    val authorUsername: String,
    val authorCountryFlag: String,
    val commentText: String,
    val createdAt: Long
)

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val storyId: String,
    val authorUserId: String,
    val authorUsername: String,
    val authorCountryFlag: String,
    val authorVipLevel: Int,
    val captionText: String,
    val mediaUri: String,
    val backgroundTheme: String, // "EMERALD_GLOW", "GOLD_ROYAL", "TURQUOISE_WAVE", "RUBY_STAR"
    val viewsCount: Int,
    val likesCount: Int,
    val giftCoinsReceived: Long,
    val createdAt: Long,
    val expiresAt: Long,
    val isActive: Boolean
)

@Entity(tableName = "reels")
data class ReelEntity(
    @PrimaryKey val reelId: String,
    val creatorUserId: String,
    val creatorUsername: String,
    val creatorCountryFlag: String,
    val creatorVipLevel: Int,
    val title: String,
    val audioTrackName: String,
    val mediaUri: String,
    val durationSec: Int,
    val likesCount: Int,
    val commentsCount: Int,
    val sharesCount: Int,
    val giftCoinsReceived: Long,
    val createdAt: Long
)

@Entity(tableName = "live_streams")
data class LiveStreamEntity(
    @PrimaryKey val streamId: String,
    val hostUserId: String,
    val hostUsername: String,
    val hostCountryFlag: String,
    val hostVipLevel: Int,
    val streamTitle: String,
    val category: String, // "LIVE_VIDEO", "PK_BATTLE", "TALENT_SHOW", "VIP_CONCERT"
    val isPkBattle: Boolean,
    val pkOpponentUserId: String?,
    val pkOpponentUsername: String?,
    val hostPkScore: Long,
    val opponentPkScore: Long,
    val viewersCount: Int,
    val totalDiamondsEarned: Long,
    val isLive: Boolean,
    val startedAt: Long
)

@Entity(tableName = "cloud_media_assets")
data class CloudMediaAssetEntity(
    @PrimaryKey val assetId: String,
    val ownerUserId: String,
    val ownerUsername: String,
    val assetType: String, // "AVATAR", "POST_MEDIA", "STORY_MEDIA", "REEL_VIDEO", "ROOM_BANNER"
    val localContentUri: String,
    val cloudCdnUrl: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256Checksum: String,
    val syncStatus: String, // "SYNCED_PRODUCTION_CDN", "UPLOADING", "VERIFIED"
    val uploadedAt: Long
)

@Entity(tableName = "database_backups")
data class DatabaseBackupSnapshotEntity(
    @PrimaryKey val backupId: String,
    val actorUserId: String,
    val schemaVersion: Int,
    val usersCount: Int,
    val roomsCount: Int,
    val transactionsCount: Int,
    val postsAndReelsCount: Int,
    val giftsCount: Int,
    val payloadChecksum: String,
    val backupSummaryJson: String,
    val createdAt: Long
)
