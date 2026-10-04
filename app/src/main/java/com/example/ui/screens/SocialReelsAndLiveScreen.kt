package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.local.*
import com.example.ui.TangerViewModel
import com.example.ui.ads.TangerAdMobFeedBannerCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SocialReelsAndLiveScreen(
    viewModel: TangerViewModel,
    currentUser: UserEntity?,
    allUsers: List<UserEntity>,
    posts: List<SocialPostEntity>,
    comments: List<PostCommentEntity>,
    stories: List<StoryEntity>,
    reels: List<ReelEntity>,
    liveStreams: List<LiveStreamEntity>,
    cloudMediaAssets: List<CloudMediaAssetEntity>,
    allGifts: List<GiftEntity>,
    luckyState: LuckyGiftStateEntity?
) {
    var selectedMode by remember { mutableStateOf("POSTS_STORIES") } // "POSTS_STORIES", "REELS", "LIVE_PK", "CLOUD_MEDIA"
    var activeStoryDialog by remember { mutableStateOf<StoryEntity?>(null) }
    var showCreateStoryDialog by remember { mutableStateOf(false) }
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showCreateReelDialog by remember { mutableStateOf(false) }
    var showStartLiveDialog by remember { mutableStateOf(false) }
    var giftTargetUserId by remember { mutableStateOf<String?>(null) }
    var showGiftDialog by remember { mutableStateOf(false) }
    var selectedMediaUri by remember { mutableStateOf("") }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedMediaUri = uri.toString()
            viewModel.uploadMediaToCloudCdn("USER_GALLERY_UPLOAD", uri.toString())
        }
    }

    val timeFormat = remember { SimpleDateFormat("MMM dd • HH:mm", Locale.US) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Mode Switcher & Cloud Media Action Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("social_reels_live_header_card"),
                colors = CardDefaults.cardColors(containerColor = TangerEmeraldDark),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🌐 TANGERAM Global Social, Reels & Live PK",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "CDN Bucket: ${BuildConfig.TANGER_CLOUD_STORAGE_BUCKET} • Real-Time Sync Active",
                                color = TangerMintSoft,
                                fontSize = 10.sp
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("cloud_media_upload_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload CDN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val tabs = listOf(
                            "POSTS_STORIES" to "📸 Posts & 24h Stories (${posts.size})",
                            "REELS" to "🎬 Global Reels (${reels.size})",
                            "LIVE_PK" to "🔴 Live & PK Battles (${liveStreams.size})",
                            "CLOUD_MEDIA" to "☁️ Cloud Media CDN (${cloudMediaAssets.size})"
                        )
                        items(tabs) { (key, label) ->
                            FilterChip(
                                selected = selectedMode == key,
                                onClick = { selectedMode = key },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TangerTurquoiseBright,
                                    selectedLabelColor = Color.Black,
                                    labelColor = Color.White
                                ),
                                modifier = Modifier.testTag("social_subtab_${key.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // 2. 24-Hour Global Stories Ring (Always visible at top of Posts/Stories)
        if (selectedMode == "POSTS_STORIES") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✨ 24h Global Stories (${stories.size} Active)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { showCreateStoryDialog = true },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("open_create_story_button")
                                ) {
                                    Text("+ Add Story", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { showCreatePostDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = TangerEmerald),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("open_create_post_button")
                                ) {
                                    Text("+ New Post", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(stories, key = { it.storyId }) { story ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(82.dp)
                                        .clickable {
                                            viewModel.viewOrLikeStory(story.storyId, like = false)
                                            activeStoryDialog = story
                                        }
                                        .testTag("story_ring_${story.storyId}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(TangerGoldGradient)
                                            .padding(3.dp)
                                            .clip(CircleShape)
                                            .background(TangerEmeraldDark),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${story.authorCountryFlag}${story.authorUsername.take(1)}",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = story.authorUsername,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "👁 ${story.viewsCount} • ❤️ ${story.likesCount}",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Official Google AdMob Feed Banner in Social Posts Feed
            item {
                TangerAdMobFeedBannerCard(
                    placementZone = "SOCIAL_POSTS_FEED"
                )
            }

            // Social Posts Feed
            items(posts, key = { it.postId }) { post ->
                var commentInput by remember(post.postId) { mutableStateOf("") }
                val postComments = remember(comments, post.postId) {
                    comments.filter { it.postId == post.postId }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("social_post_card_${post.postId}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(TangerPrimaryGradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${post.authorCountryFlag}${post.authorUsername.take(1)}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${post.authorUsername} (#${post.authorUserId}) • VIP ${post.authorVipLevel}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "📍 ${post.locationTag} • ${timeFormat.format(Date(post.createdAt))}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    giftTargetUserId = post.authorUserId
                                    showGiftDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TangerGold),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("🎁 Gift", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }
                        }

                        Text(
                            text = post.caption,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        if (post.mediaUri.isNotBlank()) {
                            Surface(
                                color = TangerEmeraldDark.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "☁️ Media (${post.mediaType}): ${post.mediaUri.takeLast(36)}",
                                        fontSize = 10.sp,
                                        color = TangerEmeraldDark,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text("CDN Verified ✓", fontSize = 10.sp, color = TangerEmerald, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.likeOrCommentPost(post.postId, null) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("❤️ ${post.likesCount} Likes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {},
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("💬 ${post.commentsCount} Comments", fontSize = 11.sp)
                                }
                            }
                            Text(
                                text = "🪙 ${post.giftCoinsReceived} Gift Coins",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TangerEmerald
                            )
                        }

                        if (postComments.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                postComments.takeLast(3).forEach { c ->
                                    Text(
                                        text = "${c.authorCountryFlag} ${c.authorUsername}: ${c.commentText}",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = commentInput,
                                onValueChange = { commentInput = it },
                                placeholder = { Text("Write a real-time comment...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (commentInput.isNotBlank()) {
                                        viewModel.likeOrCommentPost(post.postId, commentInput)
                                        commentInput = ""
                                    }
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(TangerEmerald)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Post Comment", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // 3. Global Reels Stream
        if (selectedMode == "REELS") {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎬 TANGERAM Short Video & Audio Reels",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Button(
                        onClick = { showCreateReelDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TangerGold),
                        modifier = Modifier.testTag("open_create_reel_button")
                    ) {
                        Text("+ Publish Reel", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }

            items(reels, key = { it.reelId }) { reel ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reel_card_${reel.reelId}"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = TangerEmeraldDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = TangerTurquoiseBright,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "▶ REEL • ${reel.durationSec}s",
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${reel.creatorCountryFlag} @${reel.creatorUsername} (VIP ${reel.creatorVipLevel})",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                            Button(
                                onClick = {
                                    giftTargetUserId = reel.creatorUserId
                                    showGiftDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TangerGold),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("🎁 Lucky Gift", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }
                        }

                        Text(
                            text = reel.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "🎵 Audio: ${reel.audioTrackName} • CDN: ${reel.mediaUri.takeLast(32)}",
                            color = TangerMintSoft,
                            fontSize = 11.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { viewModel.likeOrShareReel(reel.reelId, share = false) }
                            ) {
                                Text("❤️ ${reel.likesCount} Likes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = { viewModel.likeOrShareReel(reel.reelId, share = true) }
                            ) {
                                Text("↗ ${reel.sharesCount} Shares", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 4. Live Streams & PK Battles
        if (selectedMode == "LIVE_PK") {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔴 Global Live Broadcasts & PK Battles",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Button(
                        onClick = { showStartLiveDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TangerDangerRed),
                        modifier = Modifier.testTag("start_live_stream_button")
                    ) {
                        Text("🔴 Go Live / PK", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }

            items(liveStreams, key = { it.streamId }) { stream ->
                val totalPk = (stream.hostPkScore + stream.opponentPkScore).coerceAtLeast(1L)
                val hostRatio = (stream.hostPkScore.toFloat() / totalPk.toFloat()).coerceIn(0.15f, 0.85f)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_stream_card_${stream.streamId}"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, TangerGold)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(color = TangerDangerRed, shape = RoundedCornerShape(50)) {
                                    Text(
                                        text = "● LIVE • ${stream.category}",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = "👁 ${stream.viewersCount} Watching • 💎 ${stream.totalDiamondsEarned}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TangerEmerald
                                )
                            }
                            Button(
                                onClick = {
                                    giftTargetUserId = stream.hostUserId
                                    showGiftDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TangerGold),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("🎁 Gift Host", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }
                        }

                        Text(
                            text = "${stream.hostCountryFlag} ${stream.streamTitle}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )

                        if (stream.isPkBattle) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "👑 ${stream.hostUsername}: ${stream.hostPkScore} PK",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = TangerEmeraldDark
                                    )
                                    Text(
                                        text = "⚔️ ${stream.pkOpponentUsername ?: "Challenger"}: ${stream.opponentPkScore} PK",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = TangerDangerRed
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(50))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(hostRatio)
                                            .fillMaxHeight()
                                            .background(TangerTurquoiseBright)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f - hostRatio)
                                            .fillMaxHeight()
                                            .background(TangerDangerRed)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.boostLivePkScore(stream.streamId, forHost = true, points = 300L) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("⚡ Boost ${stream.hostUsername} (+300 PK)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.boostLivePkScore(stream.streamId, forHost = false, points = 300L) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("⚡ Boost Opponent (+300 PK)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Cloud Media Storage CDN Registry
        if (selectedMode == "CLOUD_MEDIA") {
            item {
                Text(
                    text = "☁️ Production Cloud Media Storage & SHA-256 Registry",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
            items(cloudMediaAssets, key = { it.assetId }) { asset ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${asset.assetId} • ${asset.assetType} • Owner: @${asset.ownerUsername} (#${asset.ownerUserId})",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "CDN URL: ${asset.cloudCdnUrl}",
                            fontSize = 11.sp,
                            color = TangerEmerald
                        )
                        Text(
                            text = "SHA-256: ${asset.sha256Checksum.take(24)}... • Size: ${asset.sizeBytes / 1024} KB • ${asset.syncStatus}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Story Viewer Dialog
    activeStoryDialog?.let { story ->
        AlertDialog(
            onDismissRequest = { activeStoryDialog = null },
            title = {
                Text("${story.authorCountryFlag} ${story.authorUsername}'s 24h Story", fontWeight = FontWeight.ExtraBold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(story.captionText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("👁 ${story.viewsCount} Views • ❤️ ${story.likesCount} Likes • Theme: ${story.backgroundTheme}", fontSize = 12.sp)
                    Text("CDN Media: ${story.mediaUri}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.viewOrLikeStory(story.storyId, like = true)
                            activeStoryDialog = null
                        }
                    ) {
                        Text("❤️ Like Story")
                    }
                    Button(
                        onClick = {
                            giftTargetUserId = story.authorUserId
                            activeStoryDialog = null
                            showGiftDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TangerGold)
                    ) {
                        Text("🎁 Send Gift", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { activeStoryDialog = null }) { Text("Close") }
            }
        )
    }

    // Create Story Dialog
    if (showCreateStoryDialog) {
        var storyCaption by remember { mutableStateOf("🌟 Live in Tanger Star Voice! Send a Lucky Gift!") }
        var storyTheme by remember { mutableStateOf("EMERALD_GLOW") }
        AlertDialog(
            onDismissRequest = { showCreateStoryDialog = false },
            title = { Text("Publish 24-Hour Global Story", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = storyCaption,
                        onValueChange = { storyCaption = it },
                        label = { Text("Story Caption") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = selectedMediaUri.ifBlank { "https://cdn.tangerstarvoice.global/media/story_live.jpg" },
                        onValueChange = { selectedMediaUri = it },
                        label = { Text("Cloud Media URI") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.publishStory24h(
                        storyCaption,
                        selectedMediaUri.ifBlank { "https://cdn.tangerstarvoice.global/media/story_live.jpg" },
                        storyTheme
                    )
                    showCreateStoryDialog = false
                }) {
                    Text("Publish 24h Story")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateStoryDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Create Social Post Dialog
    if (showCreatePostDialog) {
        var postCaption by remember { mutableStateOf("Joining the Global Lucky Gift Gala tonight! 🌍🎙️💎") }
        var postLocation by remember { mutableStateOf(currentUser?.countryName ?: "Global TANGERAM") }
        AlertDialog(
            onDismissRequest = { showCreatePostDialog = false },
            title = { Text("Publish Global TANGERAM Post", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = postCaption,
                        onValueChange = { postCaption = it },
                        label = { Text("Post Caption") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = postLocation,
                        onValueChange = { postLocation = it },
                        label = { Text("Location Tag") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.publishSocialPost(
                        postCaption,
                        selectedMediaUri.ifBlank { "https://cdn.tangerstarvoice.global/media/post_stage.jpg" },
                        "IMAGE",
                        postLocation
                    )
                    showCreatePostDialog = false
                }) {
                    Text("Publish Post")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePostDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Create Reel Dialog
    if (showCreateReelDialog) {
        var reelTitle by remember { mutableStateOf("Live Voice Stage Highlights & Lucky Jackpot! 🎰🎶") }
        var reelAudio by remember { mutableStateOf("Tanger Star Original Mix") }
        AlertDialog(
            onDismissRequest = { showCreateReelDialog = false },
            title = { Text("Publish Global Reel", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reelTitle,
                        onValueChange = { reelTitle = it },
                        label = { Text("Reel Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reelAudio,
                        onValueChange = { reelAudio = it },
                        label = { Text("Audio Track") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.publishReel(
                        reelTitle,
                        reelAudio,
                        selectedMediaUri.ifBlank { "https://cdn.tangerstarvoice.global/media/reel_highlight.mp4" },
                        24
                    )
                    showCreateReelDialog = false
                }) {
                    Text("Publish Reel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateReelDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Start Live Stream / PK Battle Dialog
    if (showStartLiveDialog) {
        var liveTitle by remember { mutableStateOf("🔥 Global PK Battle & Lucky Gift Rain") }
        var isPk by remember { mutableStateOf(true) }
        val defaultOpponent = allUsers.firstOrNull { it.tangerUserId != currentUser?.tangerUserId }
        AlertDialog(
            onDismissRequest = { showStartLiveDialog = false },
            title = { Text("🔴 Start Global Live Stream / PK Battle", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = liveTitle,
                        onValueChange = { liveTitle = it },
                        label = { Text("Live Stream Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPk, onCheckedChange = { isPk = it })
                        Text("Enable Live PK Battle vs @${defaultOpponent?.username ?: "Global Host"}")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.startLiveStream(
                        streamTitle = liveTitle,
                        category = if (isPk) "PK_BATTLE" else "LIVE_VIDEO",
                        isPkBattle = isPk,
                        pkOpponentUserId = if (isPk) defaultOpponent?.tangerUserId else null
                    )
                    showStartLiveDialog = false
                }) {
                    Text("Go Live Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartLiveDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showGiftDialog) {
        GlobalGiftCatalogDialog(
            currentUser = currentUser,
            allUsers = allUsers,
            allGifts = allGifts,
            luckyState = luckyState,
            initialReceiverUserId = giftTargetUserId,
            contextRoomId = "GLOBAL",
            onSendGift = { receiverId, giftId, qty ->
                viewModel.sendGlobalGiftToUser(receiverId, giftId, qty, "GLOBAL")
            },
            onDismiss = { showGiftDialog = false }
        )
    }
}
