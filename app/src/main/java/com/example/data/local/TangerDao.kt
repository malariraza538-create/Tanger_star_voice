package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TangerDao {

    // ================= USERS & AUTH =================
    @Query("SELECT * FROM users ORDER BY totalCoinsSpent DESC, diamondsBalance DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE tangerUserId = :userId LIMIT 1")
    fun observeUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE tangerUserId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE phoneNumber = :phone AND countryCode = :countryCode LIMIT 1")
    suspend fun getUserByPhone(phone: String, countryCode: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) AND email != '' LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM user_id_sequence WHERE counterKey = 'TANGERAM_USER_ID_SEQ' LIMIT 1")
    fun observeUserSequenceCounter(): Flow<UserSequenceCounterEntity?>

    @Query("SELECT * FROM user_id_sequence WHERE counterKey = 'TANGERAM_USER_ID_SEQ' LIMIT 1")
    suspend fun getUserSequenceCounter(): UserSequenceCounterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserSequenceCounter(counter: UserSequenceCounterEntity)

    @Query("SELECT * FROM users WHERE tangerUserId LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' ORDER BY isOnline DESC, userLevel DESC")
    suspend fun searchUsers(query: String): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsers(users: List<UserEntity>)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    // ================= OTP & SESSIONS =================
    @Query("SELECT * FROM otp_challenges WHERE phoneKey = :phoneKey LIMIT 1")
    suspend fun getOtpChallenge(phoneKey: String): OtpChallengeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOtpChallenge(challenge: OtpChallengeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveSession(): SessionEntity?

    @Query("UPDATE sessions SET isActive = 0")
    suspend fun deactivateAllSessions()

    // ================= FOLLOWS, BLOCKS, REPORTS =================
    @Query("SELECT * FROM follows WHERE followerUserId = :userId")
    fun observeFollowing(userId: String): Flow<List<FollowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: FollowEntity)

    @Query("DELETE FROM follows WHERE followerUserId = :followerId AND targetUserId = :targetId")
    suspend fun deleteFollow(followerId: String, targetId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE followerUserId = :followerId AND targetUserId = :targetId)")
    suspend fun isFollowing(followerId: String, targetId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: BlockEntity)

    @Query("SELECT * FROM blocks WHERE blockerUserId = :userId")
    fun observeBlocks(userId: String): Flow<List<BlockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: UserReportEntity)

    @Query("SELECT * FROM user_reports ORDER BY createdAt DESC")
    fun observeAllReports(): Flow<List<UserReportEntity>>

    @Query("UPDATE user_reports SET status = :status, resolvedByAdminId = :adminId WHERE reportId = :reportId")
    suspend fun updateReportStatus(reportId: String, status: String, adminId: String)

    // ================= VOICE ROOMS, SEATS, ROLES, TOP BANNERS =================
    @Query("SELECT * FROM voice_rooms ORDER BY totalRoomHeat DESC, onlineCount DESC")
    fun observeAllRooms(): Flow<List<VoiceRoomEntity>>

    @Query("SELECT * FROM voice_rooms WHERE roomId = :roomId LIMIT 1")
    fun observeRoomById(roomId: String): Flow<VoiceRoomEntity?>

    @Query("SELECT * FROM voice_rooms WHERE roomId = :roomId LIMIT 1")
    suspend fun getRoomById(roomId: String): VoiceRoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoom(room: VoiceRoomEntity)

    @Query("SELECT * FROM room_seats WHERE roomId = :roomId ORDER BY seatIndex ASC")
    fun observeRoomSeats(roomId: String): Flow<List<RoomSeatEntity>>

    @Query("SELECT * FROM room_seats WHERE roomId = :roomId ORDER BY seatIndex ASC")
    suspend fun getRoomSeats(roomId: String): List<RoomSeatEntity>

    @Query("SELECT * FROM room_seats WHERE roomId = :roomId AND seatIndex = :seatIndex LIMIT 1")
    suspend fun getRoomSeat(roomId: String, seatIndex: Int): RoomSeatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoomSeat(seat: RoomSeatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoomSeats(seats: List<RoomSeatEntity>)

    @Query("SELECT * FROM room_roles WHERE roomId = :roomId")
    fun observeRoomRoles(roomId: String): Flow<List<RoomRoleEntity>>

    @Query("SELECT * FROM room_roles WHERE roomId = :roomId AND userId = :userId LIMIT 1")
    suspend fun getRoomRole(roomId: String, userId: String): RoomRoleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoomRole(role: RoomRoleEntity)

    @Query("DELETE FROM room_roles WHERE roomId = :roomId AND userId = :userId")
    suspend fun deleteRoomRole(roomId: String, userId: String)

    @Query("SELECT * FROM room_top_notifications WHERE roomId = :roomId AND isActive = 1 AND expiresAt > :now ORDER BY createdAt DESC")
    fun observeActiveRoomNotifications(roomId: String, now: Long): Flow<List<RoomTopNotificationEntity>>

    @Query("SELECT * FROM room_top_notifications ORDER BY createdAt DESC")
    fun observeAllRoomNotifications(): Flow<List<RoomTopNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoomNotification(notification: RoomTopNotificationEntity)

    @Query("UPDATE room_top_notifications SET isActive = 0 WHERE expiresAt <= :now AND isActive = 1")
    suspend fun expireOutdatedRoomNotifications(now: Long)

    @Query("UPDATE room_top_notifications SET isActive = 0 WHERE notificationId = :notificationId")
    suspend fun deactivateRoomNotification(notificationId: String)

    // ================= WALLETS & MONTHLY DIAMOND CYCLES =================
    @Query("SELECT * FROM wallet_transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun observeUserTransactions(userId: String): Flow<List<WalletTransactionEntity>>

    @Query("SELECT * FROM wallet_transactions WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getUserTransactionsSnapshot(userId: String): List<WalletTransactionEntity>

    @Query("""
        SELECT * FROM wallet_transactions 
        WHERE userId = :userId 
          AND (:txTypeFilter = 'ALL' OR txType = :txTypeFilter)
          AND (:searchQuery = '' OR description LIKE '%' || :searchQuery || '%' OR txId LIKE '%' || :searchQuery || '%' OR idempotencyKey LIKE '%' || :searchQuery || '%')
        ORDER BY timestamp DESC
    """)
    fun observeFilteredUserTransactions(
        userId: String,
        txTypeFilter: String,
        searchQuery: String
    ): Flow<List<WalletTransactionEntity>>

    @Query("SELECT * FROM wallet_transactions ORDER BY timestamp DESC LIMIT 200")
    fun observeAllTransactions(): Flow<List<WalletTransactionEntity>>

    @Query("SELECT * FROM wallet_transactions WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getTransactionByIdempotency(key: String): WalletTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWalletTransaction(tx: WalletTransactionEntity)

    @Query("SELECT * FROM diamond_monthly_cycles WHERE userId = :userId ORDER BY yearMonth DESC")
    fun observeDiamondCyclesForUser(userId: String): Flow<List<DiamondMonthlyCycleEntity>>

    @Query("SELECT * FROM diamond_monthly_cycles WHERE userId = :userId AND yearMonth = :yearMonth LIMIT 1")
    suspend fun getDiamondCycle(userId: String, yearMonth: String): DiamondMonthlyCycleEntity?

    @Query("UPDATE diamond_monthly_cycles SET isArchived = 1 WHERE userId = :userId AND yearMonth != :currentYearMonth")
    suspend fun archivePreviousDiamondCycles(userId: String, currentYearMonth: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDiamondCycle(cycle: DiamondMonthlyCycleEntity)

    // ================= GIFTS & LUCKY GIFT =================
    @Query("SELECT * FROM gifts ORDER BY coinPrice ASC")
    fun observeAllGifts(): Flow<List<GiftEntity>>

    @Query("SELECT * FROM gifts ORDER BY coinPrice ASC")
    suspend fun getAllGiftsSnapshot(): List<GiftEntity>

    @Query("SELECT * FROM gifts WHERE giftId = :giftId LIMIT 1")
    suspend fun getGiftById(giftId: String): GiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGift(gift: GiftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGifts(gifts: List<GiftEntity>)

    @Query("DELETE FROM gifts WHERE giftId = :giftId")
    suspend fun deleteGift(giftId: String)

    @Query("SELECT * FROM gift_transactions ORDER BY timestamp DESC LIMIT 100")
    fun observeGiftTransactions(): Flow<List<GiftTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGiftTransaction(tx: GiftTransactionEntity)

    @Query("SELECT * FROM lucky_gift_state WHERE counterId = 'GLOBAL_LUCKY_COUNTER' LIMIT 1")
    fun observeLuckyGiftState(): Flow<LuckyGiftStateEntity?>

    @Query("SELECT * FROM lucky_gift_state WHERE counterId = 'GLOBAL_LUCKY_COUNTER' LIMIT 1")
    suspend fun getLuckyGiftState(): LuckyGiftStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLuckyGiftState(state: LuckyGiftStateEntity)

    @Query("SELECT * FROM lucky_gift_audit_logs ORDER BY timestamp DESC")
    fun observeLuckyGiftAudits(): Flow<List<LuckyGiftAuditEntity>>

    @Query("SELECT * FROM lucky_gift_audit_logs WHERE milestoneTriggerKey = :key LIMIT 1")
    suspend fun getLuckyAuditByTriggerKey(key: String): LuckyGiftAuditEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLuckyGiftAudit(audit: LuckyGiftAuditEntity)

    // ================= VIP =================
    @Query("SELECT * FROM vip_configs ORDER BY vipLevel ASC")
    fun observeVipConfigs(): Flow<List<VipConfigEntity>>

    @Query("SELECT * FROM vip_configs ORDER BY vipLevel ASC")
    suspend fun getVipConfigs(): List<VipConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVipConfigs(configs: List<VipConfigEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVipConfig(config: VipConfigEntity)

    @Query("SELECT * FROM vip_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun observeVipHistory(userId: String): Flow<List<VipHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVipHistory(entry: VipHistoryEntity)

    // ================= AGENCIES & HOSTS =================
    @Query("SELECT * FROM agencies ORDER BY currentMonthlyDiamonds DESC")
    fun observeAllAgencies(): Flow<List<AgencyEntity>>

    @Query("SELECT * FROM agencies WHERE agencyId = :agencyId LIMIT 1")
    suspend fun getAgencyById(agencyId: String): AgencyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAgency(agency: AgencyEntity)

    @Query("SELECT * FROM agency_members WHERE agencyId = :agencyId ORDER BY monthlyEarnedDiamonds DESC")
    fun observeAgencyMembers(agencyId: String): Flow<List<AgencyMemberEntity>>

    @Query("SELECT * FROM agency_members ORDER BY monthlyEarnedDiamonds DESC")
    fun observeAllAgencyMembers(): Flow<List<AgencyMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAgencyMember(member: AgencyMemberEntity)

    @Query("SELECT * FROM agency_invitations WHERE invitedUserId = :userId ORDER BY createdAt DESC")
    fun observeInvitationsForUser(userId: String): Flow<List<AgencyInvitationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAgencyInvitation(invite: AgencyInvitationEntity)

    // ================= CHAT & CALLS =================
    @Query("SELECT * FROM chat_messages WHERE channelType = :channelType AND roomOrThreadId = :threadId ORDER BY createdAt ASC")
    fun observeMessages(channelType: String, threadId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY createdAt DESC LIMIT 150")
    fun observeRecentMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(msg: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE channelType = 'ROOM' AND roomOrThreadId = :roomId")
    suspend fun clearRoomChat(roomId: String)

    @Query("SELECT * FROM call_sessions ORDER BY startedAt DESC LIMIT 50")
    fun observeCallSessions(): Flow<List<CallSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCallSession(call: CallSessionEntity)

    // ================= GAMES =================
    @Query("SELECT * FROM game_matches ORDER BY updatedAt DESC")
    fun observeGameMatches(): Flow<List<GameMatchEntity>>

    @Query("SELECT * FROM game_matches WHERE matchId = :matchId LIMIT 1")
    suspend fun getGameMatchById(matchId: String): GameMatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGameMatch(match: GameMatchEntity)

    // ================= EVENTS, BANNERS & RANKING REWARDS =================
    @Query("SELECT * FROM global_events WHERE isActive = 1 ORDER BY createdAt DESC")
    fun observeActiveGlobalEvents(): Flow<List<GlobalEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGlobalEvent(event: GlobalEventEntity)

    @Query("UPDATE global_events SET isActive = :active WHERE eventId = :eventId")
    suspend fun setGlobalEventActive(eventId: String, active: Boolean)

    @Query("SELECT * FROM ranking_reward_claims ORDER BY timestamp DESC")
    fun observeRankingRewardClaims(): Flow<List<RankingRewardClaimEntity>>

    @Query("SELECT * FROM ranking_reward_claims WHERE rewardClaimKey = :claimKey LIMIT 1")
    suspend fun getRankingClaimByKey(claimKey: String): RankingRewardClaimEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRankingRewardClaim(claim: RankingRewardClaimEntity)

    // ================= AUDIT LOGS =================
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 250")
    fun observeAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    // ================= SOCIAL POSTS, COMMENTS, STORIES, REELS & LIVE STREAMS =================
    @Query("SELECT * FROM social_posts ORDER BY isPinned DESC, createdAt DESC")
    fun observeAllSocialPosts(): Flow<List<SocialPostEntity>>

    @Query("SELECT * FROM social_posts WHERE postId = :postId LIMIT 1")
    suspend fun getSocialPostById(postId: String): SocialPostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSocialPost(post: SocialPostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSocialPosts(posts: List<SocialPostEntity>)

    @Query("SELECT COUNT(*) FROM social_posts")
    suspend fun countSocialPosts(): Int

    @Query("SELECT * FROM post_comments WHERE postId = :postId ORDER BY createdAt ASC")
    fun observeCommentsForPost(postId: String): Flow<List<PostCommentEntity>>

    @Query("SELECT * FROM post_comments ORDER BY createdAt DESC LIMIT 200")
    fun observeAllPostComments(): Flow<List<PostCommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPostComment(comment: PostCommentEntity)

    @Query("SELECT * FROM stories WHERE isActive = 1 AND expiresAt > :now ORDER BY createdAt DESC")
    fun observeActiveStories(now: Long): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE storyId = :storyId LIMIT 1")
    suspend fun getStoryById(storyId: String): StoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStory(story: StoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStories(stories: List<StoryEntity>)

    @Query("UPDATE stories SET isActive = 0 WHERE expiresAt <= :now AND isActive = 1")
    suspend fun expireOutdatedStories(now: Long)

    @Query("SELECT * FROM reels ORDER BY createdAt DESC")
    fun observeAllReels(): Flow<List<ReelEntity>>

    @Query("SELECT * FROM reels WHERE reelId = :reelId LIMIT 1")
    suspend fun getReelById(reelId: String): ReelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReel(reel: ReelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReels(reels: List<ReelEntity>)

    @Query("SELECT COUNT(*) FROM reels")
    suspend fun countReels(): Int

    @Query("SELECT * FROM live_streams ORDER BY isLive DESC, viewersCount DESC, startedAt DESC")
    fun observeAllLiveStreams(): Flow<List<LiveStreamEntity>>

    @Query("SELECT * FROM live_streams WHERE streamId = :streamId LIMIT 1")
    suspend fun getLiveStreamById(streamId: String): LiveStreamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLiveStream(stream: LiveStreamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLiveStreams(streams: List<LiveStreamEntity>)

    // ================= CLOUD MEDIA STORAGE & DATABASE BACKUPS =================
    @Query("SELECT * FROM cloud_media_assets ORDER BY uploadedAt DESC")
    fun observeAllCloudMediaAssets(): Flow<List<CloudMediaAssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCloudMediaAsset(asset: CloudMediaAssetEntity)

    @Query("SELECT * FROM database_backups ORDER BY createdAt DESC")
    fun observeDatabaseBackups(): Flow<List<DatabaseBackupSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDatabaseBackup(backup: DatabaseBackupSnapshotEntity)

    @Query("SELECT COUNT(*) FROM voice_rooms")
    suspend fun countRooms(): Int

    @Query("SELECT COUNT(*) FROM wallet_transactions")
    suspend fun countWalletTransactions(): Int

    @Query("SELECT COUNT(*) FROM gifts")
    suspend fun countGifts(): Int
}
