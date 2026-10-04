package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.*
import com.example.ui.TangerViewModel
import com.example.ui.theme.*

@Composable
fun MessagesAndCallsScreen(
    viewModel: TangerViewModel,
    currentUser: UserEntity?,
    allUsers: List<UserEntity>,
    globalMessages: List<ChatMessageEntity>,
    callSessions: List<CallSessionEntity>,
    allGifts: List<GiftEntity> = emptyList(),
    luckyState: LuckyGiftStateEntity? = null
) {
    var selectedChatTab by remember { mutableStateOf("GLOBAL") } // "GLOBAL" or "DIRECT_CALLS"
    var selectedDirectPartner by remember {
        mutableStateOf(allUsers.firstOrNull { it.tangerUserId != currentUser?.tangerUserId })
    }
    var messageText by remember { mutableStateOf("") }
    var showChatGiftDialog by remember { mutableStateOf(false) }
    var giftReceiverId by remember { mutableStateOf<String?>(null) }

    val directThreadId = remember(currentUser?.tangerUserId, selectedDirectPartner?.tangerUserId) {
        val u1 = currentUser?.tangerUserId ?: "199967"
        val u2 = selectedDirectPartner?.tangerUserId ?: "199968"
        listOf(u1, u2).sorted().joinToString("_")
    }
    val directMessages by viewModel.repository.observeMessages("DIRECT", directThreadId)
        .collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedChatTab == "GLOBAL",
                    onClick = { selectedChatTab = "GLOBAL" },
                    label = { Text("🌍 Global Chat Feed", fontWeight = FontWeight.Bold) }
                )
                FilterChip(
                    selected = selectedChatTab == "DIRECT_CALLS",
                    onClick = { selectedChatTab = "DIRECT_CALLS" },
                    label = { Text("📞 Direct Chat, Voice & Video Calls", fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (selectedChatTab == "GLOBAL") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🌍 Global Tanger Star Stream", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        globalMessages.takeLast(12).forEach { msg ->
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "${msg.senderCountryFlag} ${msg.senderUsername} (VIP ${msg.senderVipLevel})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TangerEmerald
                                    )
                                    Text(text = msg.contentText, fontSize = 13.sp)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                placeholder = { Text("Send message to all global users...") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("global_chat_input"),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    giftReceiverId = allUsers.firstOrNull { it.tangerUserId != currentUser?.tangerUserId }?.tangerUserId
                                    showChatGiftDialog = true
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(TangerTurquoiseBright)
                                    .testTag("global_chat_send_gift_button")
                            ) {
                                Icon(Icons.Default.CardGiftcard, contentDescription = "Send Lucky Gift", tint = Color.Black)
                            }
                            IconButton(
                                onClick = {
                                    viewModel.sendChatMessage("GLOBAL", "GLOBAL", "", isVoiceNote = true, voiceSec = 7)
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(TangerGold)
                            ) {
                                Icon(Icons.Default.KeyboardVoice, contentDescription = "Send Voice Note", tint = Color.Black)
                            }
                            IconButton(
                                onClick = {
                                    if (messageText.isNotBlank()) {
                                        viewModel.sendChatMessage("GLOBAL", "GLOBAL", messageText)
                                        messageText = ""
                                    }
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(TangerEmeraldLight)
                                    .testTag("global_chat_send_button")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Select Global Friend to Message or Call:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(allUsers.filter { it.tangerUserId != currentUser?.tangerUserId }) { usr ->
                                FilterChip(
                                    selected = selectedDirectPartner?.tangerUserId == usr.tangerUserId,
                                    onClick = { selectedDirectPartner = usr },
                                    label = { Text("${usr.countryFlag} ${usr.username}") }
                                )
                            }
                        }

                        selectedDirectPartner?.let { partner ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${partner.countryFlag} ${partner.username}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                    Text("ID: ${partner.tangerUserId} • VIP ${partner.vipLevel}", fontSize = 11.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            giftReceiverId = partner.tangerUserId
                                            showChatGiftDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TangerGold),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("direct_chat_send_gift_button")
                                    ) {
                                        Text("🎁 Gift", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                                    }
                                    FilledTonalButton(
                                        onClick = { viewModel.startCallWithUser(partner, "VOICE_CALL") },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("start_voice_call_button")
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Voice", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = { viewModel.startCallWithUser(partner, "VIDEO_CALL") },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("start_video_call_button")
                                    ) {
                                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Video", fontSize = 11.sp)
                                    }
                                }
                            }

                            HorizontalDivider()
                            if (directMessages.isEmpty()) {
                                Text(
                                    text = "Start a private encrypted conversation or send a voice message to ${partner.username}.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                directMessages.forEach { dm ->
                                    Text(
                                        text = "${dm.senderUsername}: ${dm.contentText}",
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = messageText,
                                    onValueChange = { messageText = it },
                                    placeholder = { Text("Direct message ${partner.username}...") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = {
                                        viewModel.sendChatMessage("DIRECT", directThreadId, "", isVoiceNote = true, voiceSec = 5)
                                    },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(TangerGold)
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = "Voice Note", tint = Color.Black)
                                }
                                IconButton(
                                    onClick = {
                                        if (messageText.isNotBlank()) {
                                            viewModel.sendChatMessage("DIRECT", directThreadId, messageText)
                                            messageText = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(TangerEmeraldLight)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("📞 Recent Real-Time Call Logs", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            items(callSessions, key = { it.callId }) { call ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${call.callType}: ${call.callerUsername} → ${call.receiverUsername}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Session ID: ${call.callId} • Status: ${call.status}", fontSize = 11.sp)
                        }
                        Text("${call.durationSeconds}s", fontWeight = FontWeight.ExtraBold, color = TangerEmerald)
                    }
                }
            }
        }
    }

    if (showChatGiftDialog) {
        GlobalGiftCatalogDialog(
            currentUser = currentUser,
            allUsers = allUsers,
            allGifts = allGifts,
            luckyState = luckyState,
            initialReceiverUserId = giftReceiverId,
            contextRoomId = "GLOBAL",
            onSendGift = { receiverId, giftId, qty ->
                viewModel.sendGlobalGiftToUser(receiverId, giftId, qty, "GLOBAL")
            },
            onDismiss = { showChatGiftDialog = false }
        )
    }
}

@Composable
fun MultiplayerGamesScreen(
    viewModel: TangerViewModel,
    currentUser: UserEntity?,
    allUsers: List<UserEntity>,
    matches: List<GameMatchEntity>
) {
    var selectedGameType by remember { mutableStateOf("LUDO_STAR") }
    var selectedEntryFee by remember { mutableLongStateOf(500L) }
    val defaultOpponent = remember(allUsers, currentUser) {
        allUsers.firstOrNull { it.tangerUserId != currentUser?.tangerUserId }
    }

    val activeMatch = matches.firstOrNull { it.status == "ACTIVE" } ?: matches.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ludo_game_header_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🎲 Server-Controlled Multiplayer Games",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Server controls players, matches, turns, dice state, winners, and atomic Coin payouts. Client-side fake results are strictly blocked.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("LUDO_STAR", "TREASURE_DICE", "ROYAL_ROULETTE").forEach { gt ->
                            FilterChip(
                                selected = selectedGameType == gt,
                                onClick = { selectedGameType = gt },
                                label = { Text(gt.replace("_", " "), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(200L, 500L, 1_000L).forEach { fee ->
                                FilterChip(
                                    selected = selectedEntryFee == fee,
                                    onClick = { selectedEntryFee = fee },
                                    label = { Text("🪙 $fee", fontSize = 11.sp) }
                                )
                            }
                        }
                        Button(
                            onClick = {
                                if (defaultOpponent != null) {
                                    viewModel.startNewLudoMatch(defaultOpponent, selectedEntryFee, selectedGameType)
                                }
                            },
                            modifier = Modifier.testTag("start_new_ludo_match_button")
                        ) {
                            Text("New Match", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Interactive Ludo Star Board for Active Match
        if (activeMatch != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_ludo_board_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = TangerEmeraldDark)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "🎮 ${activeMatch.gameType} • Match ${activeMatch.matchId}",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Prize Pool: 🪙 ${activeMatch.rewardPoolCoins} Coins • Turn #${activeMatch.turnNumber}",
                                    color = TangerGoldLight,
                                    fontSize = 12.sp
                                )
                            }
                            Surface(
                                color = if (activeMatch.status == "ACTIVE") TangerTurquoiseBright else TangerGold,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = activeMatch.status,
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Visual Ludo Track Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF064E3B))
                                .padding(12.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                // Draw 4 Ludo Home Bases
                                drawRoundRect(
                                    color = Color(0xFF10B981).copy(alpha = 0.35f),
                                    topLeft = Offset(0f, 0f),
                                    size = androidx.compose.ui.geometry.Size(w * 0.28f, h * 0.42f)
                                )
                                drawRoundRect(
                                    color = Color(0xFFF59E0B).copy(alpha = 0.35f),
                                    topLeft = Offset(w * 0.72f, h * 0.58f),
                                    size = androidx.compose.ui.geometry.Size(w * 0.28f, h * 0.42f)
                                )
                                // Draw 56-step track progress line
                                val trackY = h * 0.5f
                                drawLine(
                                    color = Color.White.copy(alpha = 0.3f),
                                    start = Offset(24f, trackY),
                                    end = Offset(w - 24f, trackY),
                                    strokeWidth = 14f
                                )
                                val p1X = 24f + ((w - 48f) * (activeMatch.player1Pos / 56f))
                                val p2X = 24f + ((w - 48f) * (activeMatch.player2Pos / 56f))

                                drawCircle(
                                    color = Color(0xFF34D399),
                                    radius = 18f,
                                    center = Offset(p1X, trackY - 10f)
                                )
                                drawCircle(
                                    color = Color(0xFFFBBF24),
                                    radius = 18f,
                                    center = Offset(p2X, trackY + 10f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🟢 ${activeMatch.player1Name}: Tile ${activeMatch.player1Pos}/56",
                                color = TangerMint,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "🟡 ${activeMatch.player2Name}: Tile ${activeMatch.player2Pos}/56",
                                color = TangerGoldLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        if (activeMatch.status == "ACTIVE") {
                            Button(
                                onClick = { viewModel.rollServerLudoDice(activeMatch.matchId) },
                                colors = ButtonDefaults.buttonColors(containerColor = TangerGold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("roll_server_ludo_dice_button")
                            ) {
                                Icon(Icons.Default.Casino, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Roll Server-Validated Dice (Last Roll: ${activeMatch.lastDiceRoll})",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        } else {
                            Surface(
                                color = TangerGold.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "🏆 Winner: ${activeMatch.winnerUsername} (+${activeMatch.rewardPoolCoins} Coins Credited)",
                                    color = TangerGoldLight,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("📜 Server Match History & Audit", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
        items(matches, key = { it.matchId }) { m ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${m.gameType} (${m.matchId})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${m.player1Name} (${m.player1Pos}/56) vs ${m.player2Name} (${m.player2Pos}/56)", fontSize = 11.sp)
                    }
                    Text(
                        text = if (m.winnerUsername != null) "🏆 ${m.winnerUsername}" else "ACTIVE",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = TangerEmerald
                    )
                }
            }
        }
    }
}
