package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.*
import com.example.data.realtime.RealtimeEvent
import com.example.ui.TangerViewModel
import com.example.ui.theme.*

@Composable
fun VoiceRoomScreen(
    viewModel: TangerViewModel,
    currentUser: UserEntity?,
    allRooms: List<VoiceRoomEntity>,
    activeRoom: VoiceRoomEntity?,
    seats: List<RoomSeatEntity>,
    roles: List<RoomRoleEntity>,
    activeTopBanners: List<RoomTopNotificationEntity>,
    roomMessages: List<ChatMessageEntity>,
    allGifts: List<GiftEntity>,
    luckyState: LuckyGiftStateEntity?,
    allUsers: List<UserEntity>,
    nowMillis: Long,
    isMicMuted: Boolean,
    hasAudioPermission: Boolean,
    micAmplitudeDb: Float,
    connectionState: String,
    pingMs: Int,
    recentGiftOverlay: RealtimeEvent.GiftBroadcast?
) {
    var showOwnerControlSheet by remember { mutableStateOf(false) }
    var showCreateTopBannerDialog by remember { mutableStateOf(false) }
    var showGiftTrayDialog by remember { mutableStateOf(false) }
    var showMusicDialog by remember { mutableStateOf(false) }
    var chatInput by remember { mutableStateOf("") }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.updateAudioPermissionGranted(granted)
    }

    val roomThemeGradient = remember(activeRoom?.themeName) {
        when (activeRoom?.themeName) {
            "Turquoise Oasis" -> Brush.verticalGradient(listOf(Color(0xFF0F766E), Color(0xFF042F2E)))
            "Royal Star" -> Brush.verticalGradient(listOf(Color(0xFF1E1B4B), Color(0xFF064E3B)))
            "Cyber Neon" -> Brush.verticalGradient(listOf(Color(0xFF065F46), Color(0xFF111827)))
            else -> Brush.verticalGradient(listOf(TangerEmeraldDark, Color(0xFF064E3B), Color(0xFF022C22)))
        }
    }

    val isRoomOwnerOrAdmin = remember(currentUser, activeRoom, roles) {
        if (currentUser == null || activeRoom == null) false
        else currentUser.role == "GLOBAL_OWNER" ||
            currentUser.role == "ADMIN" ||
            activeRoom.ownerUserId == currentUser.tangerUserId ||
            roles.any { it.userId == currentUser.tangerUserId && (it.roleType == "OWNER" || it.roleType == "ADMIN") }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(roomThemeGradient)
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Room Switcher Bar + Real-Time WebSocket & Audio Status
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allRooms, key = { it.roomId }) { rm ->
                        val isSelected = rm.roomId == activeRoom?.roomId
                        Surface(
                            modifier = Modifier
                                .clickable { viewModel.selectAndEnterRoom(rm.roomId) }
                                .testTag("switch_room_${rm.roomId}"),
                            color = if (isSelected) TangerTurquoiseBright else Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${rm.countryFlag} ${rm.roomId}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier
                        .clickable { viewModel.realtimeService.triggerReconnectHandshake() }
                        .testTag("realtime_status_badge"),
                    color = if (connectionState == "CONNECTED") Color(0xFF10B981).copy(alpha = 0.25f)
                    else TangerGold.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TangerMint)
                ) {
                    Text(
                        text = if (connectionState == "CONNECTED") "⚡ LIVE ${pingMs}ms" else "🔄 SYNCING...",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // 2. PROMINENT REAL-TIME ROOM TOP NOTIFICATION / ANNOUNCEMENT BANNER (Feature #5)
        item {
            val activeBanner = activeTopBanners.firstOrNull()
            if (activeBanner != null) {
                val remainingSec = ((activeBanner.expiresAt - nowMillis) / 1000L).coerceAtLeast(0L)
                val bannerBrush = when (activeBanner.bannerStyle) {
                    "RUBY_ALERT" -> Brush.horizontalGradient(listOf(Color(0xFFB91C1C), Color(0xFFD97706)))
                    "EMERALD_ROYAL" -> Brush.horizontalGradient(listOf(Color(0xFF047857), Color(0xFF0D9488)))
                    else -> Brush.horizontalGradient(listOf(Color(0xFFD97706), Color(0xFF0D9488)))
                }
                val iconEmoji = when (activeBanner.iconName) {
                    "CROWN" -> "👑"
                    "GIFT" -> "🎁"
                    "FIRE" -> "🔥"
                    "MEGAPHONE" -> "📢"
                    else -> "🌟"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, TangerGoldLight, RoundedCornerShape(18.dp))
                        .testTag("room_top_notification_banner"),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bannerBrush)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = iconEmoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = activeBanner.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                }
                                Surface(
                                    color = Color.Black.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "⏱ ${remainingSec}s • ${activeBanner.creatorRole}",
                                        color = TangerGoldLight,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = activeBanner.messageText,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_room_top_banner"),
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📢 No active Room Top Notification (Expired banners auto-removed)",
                            color = TangerMintSoft,
                            fontSize = 11.sp
                        )
                        if (isRoomOwnerOrAdmin) {
                            TextButton(onClick = { showCreateTopBannerDialog = true }) {
                                Text("+ Post Banner", color = TangerGoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Room Header Info + Owner Controls & Music Bar
        item {
            if (activeRoom != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${activeRoom.countryFlag} ${activeRoom.roomTitle}",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Owner: ${activeRoom.ownerUsername} • Theme: ${activeRoom.themeName} • 🔥 ${activeRoom.totalRoomHeat}",
                                    color = TangerMintSoft,
                                    fontSize = 11.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { showCreateTopBannerDialog = true },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(TangerGold.copy(alpha = 0.25f))
                                        .testTag("open_top_banner_creator_button")
                                ) {
                                    Icon(Icons.Default.Campaign, contentDescription = "Room Top Notification", tint = TangerGoldLight)
                                }
                                IconButton(
                                    onClick = { showMusicDialog = true },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(TangerTurquoiseBright.copy(alpha = 0.25f))
                                        .testTag("open_room_music_button")
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = "Room Music", tint = Color.White)
                                }
                                IconButton(
                                    onClick = { showOwnerControlSheet = true },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .testTag("open_room_owner_controls_button")
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = "Room Owner Controls", tint = Color.White)
                                }
                            }
                        }

                        // Synchronized Room Music Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.28f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (activeRoom.isMusicPlaying) Icons.Default.GraphicEq else Icons.Default.PauseCircle,
                                    contentDescription = null,
                                    tint = TangerMint,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${activeRoom.activeMusicTrackTitle ?: "No Track"} (${(activeRoom.musicVolume * 100).toInt()}% Vol)",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (luckyState != null) {
                                Text(
                                    text = "🍀 Lucky Counter: ${luckyState.globalHitCount}/${luckyState.milestone1Threshold}",
                                    color = TangerGoldLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Real-Time Gift Broadcast Overlay Banner
        item {
            AnimatedVisibility(
                visible = recentGiftOverlay != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                recentGiftOverlay?.let { giftEvent ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TangerGold.copy(alpha = 0.92f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${giftEvent.giftEmoji} ${giftEvent.senderName} sent ${giftEvent.quantity}x ${giftEvent.giftName} to ${giftEvent.receiverName}!",
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (giftEvent.luckyRewardGranted > 0) {
                                Surface(color = TangerEmeraldDark, shape = RoundedCornerShape(50)) {
                                    Text(
                                        text = "+${giftEvent.luckyRewardGranted} LUCKY!",
                                        color = TangerGoldLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. 8-Speaker Voice Stage Grid (2 rows x 4 seats)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("speaker_seats_grid"),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f)),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val chunkedSeats = seats.chunked(4)
                    chunkedSeats.forEach { rowSeats ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowSeats.forEach { seat ->
                                SpeakerSeatItem(
                                    seat = seat,
                                    isCurrentUser = seat.occupantUserId == currentUser?.tangerUserId,
                                    liveMicDb = if (seat.occupantUserId == currentUser?.tangerUserId && !isMicMuted) micAmplitudeDb else seat.speakingAudioDb,
                                    onClickSeat = { viewModel.joinOrLeaveSeat(seat.seatIndex) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Real Hardware Microphone Audio Bar + Gift Action Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (!hasAudioPermission) {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.toggleLocalMicrophoneMute()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!hasAudioPermission || isMicMuted) Color(0xFF475569) else TangerTurquoiseBright
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("voice_room_mic_button")
                ) {
                    Icon(
                        if (!hasAudioPermission || isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Microphone Toggle",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            !hasAudioPermission -> "Enable Real Mic"
                            isMicMuted -> "Mic Muted"
                            else -> "Live ${micAmplitudeDb.toInt()} dB"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { showGiftTrayDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TangerGold),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_gift_tray_button")
                ) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = "Send Gift", tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send Gift", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // 7. Live Room Chat Feed & Input
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.32f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💬 Live Room Chat (${roomMessages.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (activeRoom?.isChatLocked == true) {
                            Text("🔒 Chat Locked by Owner", color = TangerGoldLight, fontSize = 11.sp)
                        }
                    }

                    roomMessages.takeLast(8).forEach { msg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (msg.messageType == "GIFT_ALERT") TangerGold.copy(alpha = 0.22f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${msg.senderCountryFlag} [VIP${msg.senderVipLevel}] ${msg.senderUsername}: ${msg.contentText}",
                                color = if (msg.messageType == "GIFT_ALERT") TangerGoldLight else Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text("Say something in room...", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("room_chat_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = TangerMint,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                        IconButton(
                            onClick = {
                                if (chatInput.isNotBlank() && activeRoom != null) {
                                    viewModel.sendChatMessage("ROOM", activeRoom.roomId, chatInput)
                                    chatInput = ""
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(TangerTurquoiseBright)
                                .testTag("room_chat_send_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send Room Message", tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    // DIALOG 1: Create Room Top Notification / Announcement (Feature #5)
    if (showCreateTopBannerDialog && activeRoom != null) {
        var title by remember { mutableStateOf("👑 VIP Star Party Announcement") }
        var message by remember { mutableStateOf("Send Lucky Gifts now to trigger the Global Counter Jackpot!") }
        var selectedIcon by remember { mutableStateOf("CROWN") }
        var selectedStyle by remember { mutableStateOf("TURQUOISE_GOLD") }
        var durationSecText by remember { mutableStateOf("120") }

        AlertDialog(
            onDismissRequest = { showCreateTopBannerDialog = false },
            title = { Text("📢 Publish Room Top Notification", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Backend RBAC verifies Room Owner/Admin permission. Stored in database & broadcast in real-time to current and new users.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Banner Headline") },
                        singleLine = true,
                        modifier = Modifier.testTag("top_banner_title_input")
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Announcement Text") },
                        modifier = Modifier.testTag("top_banner_message_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("CROWN", "STAR", "GIFT", "FIRE", "MEGAPHONE").forEach { ic ->
                            FilterChip(
                                selected = selectedIcon == ic,
                                onClick = { selectedIcon = ic },
                                label = { Text(ic, fontSize = 10.sp) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("TURQUOISE_GOLD", "EMERALD_ROYAL", "RUBY_ALERT").forEach { st ->
                            FilterChip(
                                selected = selectedStyle == st,
                                onClick = { selectedStyle = st },
                                label = { Text(st.take(9), fontSize = 10.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = durationSecText,
                        onValueChange = { durationSecText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Expiry Duration (Seconds)") },
                        singleLine = true,
                        modifier = Modifier.testTag("top_banner_duration_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dur = durationSecText.toIntOrNull() ?: 120
                        viewModel.publishRoomTopBanner(title, message, selectedIcon, selectedStyle, dur)
                        showCreateTopBannerDialog = false
                    },
                    modifier = Modifier.testTag("publish_top_banner_confirm_button")
                ) {
                    Text("Publish Live Banner")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTopBannerDialog = false }) { Text("Cancel") }
            }
        )
    }

    // DIALOG 2: Room Owner Controls (Feature #4: Admins, Co-hosts, Seats, Themes, Chat Lock)
    if (showOwnerControlSheet && activeRoom != null) {
        var targetRoleUserId by remember { mutableStateOf("199970") }
        AlertDialog(
            onDismissRequest = { showOwnerControlSheet = false },
            title = { Text("👑 Room Owner & Admin Controls", fontWeight = FontWeight.ExtraBold) },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("1. Room Theme & Chat Moderation", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("Emerald Palace", "Turquoise Oasis", "Royal Star", "Cyber Neon")) { th ->
                                FilterChip(
                                    selected = activeRoom.themeName == th,
                                    onClick = { viewModel.updateRoomSettingsAndMusic(newTheme = th) },
                                    label = { Text(th, fontSize = 11.sp) }
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.updateRoomSettingsAndMusic(toggleChatLock = true) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (activeRoom.isChatLocked) "Unlock Room Chat" else "Lock Room Chat")
                        }
                    }

                    item {
                        HorizontalDivider()
                        Text("2. Add / Remove Room Admin & Co-Host", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        OutlinedTextField(
                            value = targetRoleUserId,
                            onValueChange = { targetRoleUserId = it },
                            label = { Text("Target Tanger User ID") },
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(onClick = { viewModel.assignRoomRole(targetRoleUserId, "ADMIN") }) {
                                Text("+ Admin", fontSize = 11.sp)
                            }
                            FilledTonalButton(onClick = { viewModel.assignRoomRole(targetRoleUserId, "CO_HOST") }) {
                                Text("+ Co-Host", fontSize = 11.sp)
                            }
                            OutlinedButton(onClick = { viewModel.assignRoomRole(targetRoleUserId, null) }) {
                                Text("Remove", fontSize = 11.sp)
                            }
                        }
                    }

                    item {
                        Text("3. Manage Speaker Seats (Mute / Lock)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    items(seats, key = { it.seatIndex }) { seat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Seat #${seat.seatIndex + 1}: ${seat.occupantUsername ?: "Empty"}",
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { viewModel.manageSeatByOwner(seat.seatIndex, toggleLock = false, toggleMute = true) }) {
                                    Text(if (seat.isMutedByOwner) "Unmute" else "Mute", fontSize = 11.sp)
                                }
                                TextButton(onClick = { viewModel.manageSeatByOwner(seat.seatIndex, toggleLock = true, toggleMute = false) }) {
                                    Text(if (seat.isSeatLocked) "Unlock" else "Lock", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showOwnerControlSheet = false }) { Text("Done") }
            }
        )
    }

    // DIALOG 3: Real-Time Room Music Synchronizer (Feature #14)
    if (showMusicDialog && activeRoom != null) {
        val tracks = listOf(
            "Tangier Sunset Acoustic Groove" to "Tanger Star Audio Lab",
            "Andalusian Starlight Oud" to "Emerald Ensemble",
            "Casablanca Midnight Synth" to "Atlas Beats (Permitted)",
            "Desert Rose Chillhop" to "Oasis Lounge"
        )
        var volumeSlider by remember { mutableFloatStateOf(activeRoom.musicVolume) }

        AlertDialog(
            onDismissRequest = { showMusicDialog = false },
            title = { Text("🎵 Room Music Queue & Sync", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Permitted local/royalty-free room audio. Synchronized in real time for all listeners.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    tracks.forEach { (title, artist) ->
                        val isCurrent = activeRoom.activeMusicTrackTitle == title
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateRoomSettingsAndMusic(
                                        musicTrack = title,
                                        musicArtist = artist,
                                        isMusicPlaying = true,
                                        musicVolume = volumeSlider
                                    )
                                },
                            color = if (isCurrent) TangerMintSoft else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TangerEmeraldDark)
                                    Text(artist, fontSize = 10.sp, color = TextMutedSlate)
                                }
                                Icon(
                                    if (isCurrent && activeRoom.isMusicPlaying) Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = TangerEmerald
                                )
                            }
                        }
                    }
                    Text("Master Volume: ${(volumeSlider * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Slider(
                        value = volumeSlider,
                        onValueChange = { volumeSlider = it },
                        onValueChangeFinished = {
                            viewModel.updateRoomSettingsAndMusic(musicVolume = volumeSlider)
                        }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateRoomSettingsAndMusic(isMusicPlaying = !(activeRoom.isMusicPlaying))
                }) {
                    Text(if (activeRoom.isMusicPlaying) "Pause Music" else "Play Music")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMusicDialog = false }) { Text("Close") }
            }
        )
    }

    // DIALOG 4: Server-Validated Atomic Gift Tray (Features #6, #7, #8)
    if (showGiftTrayDialog) {
        val categories = listOf("All", "Gift", "Event Gift", "Lucky Gift", "Lucky Draw", "CP", "Country/Flag Gift")
        var selectedCategory by remember { mutableStateOf("All") }
        var selectedReceiverId by remember {
            mutableStateOf(
                allUsers.firstOrNull { it.tangerUserId != currentUser?.tangerUserId }?.tangerUserId
                    ?: "199968"
            )
        }
        var selectedQty by remember { mutableIntStateOf(1) }

        val activeGifts = remember(allGifts, selectedCategory) {
            allGifts.filter { it.isActive && (selectedCategory == "All" || it.category == selectedCategory) }
        }

        AlertDialog(
            onDismissRequest = { showGiftTrayDialog = false },
            title = {
                Column {
                    Text("🎁 Send Atomic Server-Validated Gift", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(
                        text = "Your Balance: 🪙 ${currentUser?.coinsBalance ?: 0} Coins • Lucky Hits: ${luckyState?.globalHitCount ?: 0}",
                        fontSize = 12.sp,
                        color = TangerEmerald
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. Select Receiver:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allUsers) { usr ->
                            FilterChip(
                                selected = selectedReceiverId == usr.tangerUserId,
                                onClick = { selectedReceiverId = usr.tangerUserId },
                                label = { Text("${usr.countryFlag} ${usr.username.take(10)}", fontSize = 11.sp) }
                            )
                        }
                    }

                    Text("2. Category:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.height(210.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeGifts, key = { it.giftId }) { gift ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.sendGiftInRoom(selectedReceiverId, gift.giftId, selectedQty)
                                        showGiftTrayDialog = false
                                    }
                                    .testTag("send_gift_item_${gift.giftId}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(gift.iconEmoji, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(gift.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("${gift.category} • ${gift.diamondReturnRatePercent}% Diamond Return", fontSize = 10.sp)
                                        }
                                    }
                                    Surface(color = TangerGold, shape = RoundedCornerShape(50)) {
                                        Text(
                                            text = "🪙 ${gift.coinPrice * selectedQty}",
                                            color = Color.Black,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Combo Quantity:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1, 5, 10, 50).forEach { q ->
                                FilterChip(
                                    selected = selectedQty == q,
                                    onClick = { selectedQty = q },
                                    label = { Text("x$q", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGiftTrayDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SpeakerSeatItem(
    seat: RoomSeatEntity,
    isCurrentUser: Boolean,
    liveMicDb: Float,
    onClickSeat: () -> Unit
) {
    val isOccupied = seat.occupantUserId != null
    val ringColor = when {
        seat.isSeatLocked -> TangerDangerRed
        isOccupied && !seat.isMutedByOwner && !seat.isMutedBySelf && liveMicDb > 15f -> TangerMint
        isOccupied -> TangerGold
        else -> Color.White.copy(alpha = 0.25f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(74.dp)
            .clickable { onClickSeat() }
            .testTag("seat_item_${seat.seatIndex}")
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(
                    if (isOccupied) TangerBrightGradient else Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.06f))
                    )
                )
                .border(2.dp, ringColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when {
                seat.isSeatLocked && !isOccupied -> Icon(Icons.Default.Lock, contentDescription = "Locked Seat", tint = TangerDangerRed)
                isOccupied -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = seat.occupantUsername?.take(2)?.uppercase() ?: "ST",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "VIP${seat.occupantVipLevel}",
                            color = TangerGoldLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                else -> Icon(Icons.Default.EventSeat, contentDescription = "Open Seat", tint = Color.White.copy(alpha = 0.7f))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = seat.occupantUsername ?: "Seat #${seat.seatIndex + 1}",
            color = if (isCurrentUser) TangerMint else Color.White,
            fontSize = 11.sp,
            fontWeight = if (isOccupied) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = when {
                seat.isMutedByOwner -> "🔇 Owner Mute"
                isOccupied -> seat.occupantRole
                else -> "Tap to Sit"
            },
            color = TangerGoldLight,
            fontSize = 9.sp
        )
    }
}
