package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        UserSequenceCounterEntity::class,
        OtpChallengeEntity::class,
        SessionEntity::class,
        FollowEntity::class,
        BlockEntity::class,
        UserReportEntity::class,
        VoiceRoomEntity::class,
        RoomSeatEntity::class,
        RoomRoleEntity::class,
        RoomTopNotificationEntity::class,
        WalletTransactionEntity::class,
        DiamondMonthlyCycleEntity::class,
        GiftEntity::class,
        GiftTransactionEntity::class,
        LuckyGiftStateEntity::class,
        LuckyGiftAuditEntity::class,
        VipConfigEntity::class,
        VipHistoryEntity::class,
        AgencyEntity::class,
        AgencyMemberEntity::class,
        AgencyInvitationEntity::class,
        ChatMessageEntity::class,
        CallSessionEntity::class,
        GameMatchEntity::class,
        GlobalEventEntity::class,
        RankingRewardClaimEntity::class,
        AuditLogEntity::class,
        SocialPostEntity::class,
        PostCommentEntity::class,
        StoryEntity::class,
        ReelEntity::class,
        LiveStreamEntity::class,
        CloudMediaAssetEntity::class,
        DatabaseBackupSnapshotEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class TangerDatabase : RoomDatabase() {
    abstract fun tangerDao(): TangerDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `social_posts` (
                        `postId` TEXT NOT NULL,
                        `authorUserId` TEXT NOT NULL,
                        `authorUsername` TEXT NOT NULL,
                        `authorCountryFlag` TEXT NOT NULL,
                        `authorVipLevel` INTEGER NOT NULL,
                        `caption` TEXT NOT NULL,
                        `mediaUri` TEXT NOT NULL,
                        `mediaType` TEXT NOT NULL,
                        `locationTag` TEXT NOT NULL,
                        `likesCount` INTEGER NOT NULL,
                        `commentsCount` INTEGER NOT NULL,
                        `sharesCount` INTEGER NOT NULL,
                        `giftCoinsReceived` INTEGER NOT NULL,
                        `isPinned` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`postId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `post_comments` (
                        `commentId` TEXT NOT NULL,
                        `postId` TEXT NOT NULL,
                        `authorUserId` TEXT NOT NULL,
                        `authorUsername` TEXT NOT NULL,
                        `authorCountryFlag` TEXT NOT NULL,
                        `commentText` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`commentId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `stories` (
                        `storyId` TEXT NOT NULL,
                        `authorUserId` TEXT NOT NULL,
                        `authorUsername` TEXT NOT NULL,
                        `authorCountryFlag` TEXT NOT NULL,
                        `authorVipLevel` INTEGER NOT NULL,
                        `captionText` TEXT NOT NULL,
                        `mediaUri` TEXT NOT NULL,
                        `backgroundTheme` TEXT NOT NULL,
                        `viewsCount` INTEGER NOT NULL,
                        `likesCount` INTEGER NOT NULL,
                        `giftCoinsReceived` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `expiresAt` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        PRIMARY KEY(`storyId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reels` (
                        `reelId` TEXT NOT NULL,
                        `creatorUserId` TEXT NOT NULL,
                        `creatorUsername` TEXT NOT NULL,
                        `creatorCountryFlag` TEXT NOT NULL,
                        `creatorVipLevel` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `audioTrackName` TEXT NOT NULL,
                        `mediaUri` TEXT NOT NULL,
                        `durationSec` INTEGER NOT NULL,
                        `likesCount` INTEGER NOT NULL,
                        `commentsCount` INTEGER NOT NULL,
                        `sharesCount` INTEGER NOT NULL,
                        `giftCoinsReceived` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`reelId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `live_streams` (
                        `streamId` TEXT NOT NULL,
                        `hostUserId` TEXT NOT NULL,
                        `hostUsername` TEXT NOT NULL,
                        `hostCountryFlag` TEXT NOT NULL,
                        `hostVipLevel` INTEGER NOT NULL,
                        `streamTitle` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `isPkBattle` INTEGER NOT NULL,
                        `pkOpponentUserId` TEXT,
                        `pkOpponentUsername` TEXT,
                        `hostPkScore` INTEGER NOT NULL,
                        `opponentPkScore` INTEGER NOT NULL,
                        `viewersCount` INTEGER NOT NULL,
                        `totalDiamondsEarned` INTEGER NOT NULL,
                        `isLive` INTEGER NOT NULL,
                        `startedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`streamId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cloud_media_assets` (
                        `assetId` TEXT NOT NULL,
                        `ownerUserId` TEXT NOT NULL,
                        `ownerUsername` TEXT NOT NULL,
                        `assetType` TEXT NOT NULL,
                        `localContentUri` TEXT NOT NULL,
                        `cloudCdnUrl` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `sizeBytes` INTEGER NOT NULL,
                        `sha256Checksum` TEXT NOT NULL,
                        `syncStatus` TEXT NOT NULL,
                        `uploadedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`assetId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `database_backups` (
                        `backupId` TEXT NOT NULL,
                        `actorUserId` TEXT NOT NULL,
                        `schemaVersion` INTEGER NOT NULL,
                        `usersCount` INTEGER NOT NULL,
                        `roomsCount` INTEGER NOT NULL,
                        `transactionsCount` INTEGER NOT NULL,
                        `postsAndReelsCount` INTEGER NOT NULL,
                        `giftsCount` INTEGER NOT NULL,
                        `payloadChecksum` TEXT NOT NULL,
                        `backupSummaryJson` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`backupId`)
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: TangerDatabase? = null

        fun getInstance(context: Context): TangerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TangerDatabase::class.java,
                    "tanger_star_voice_production.db"
                )
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
