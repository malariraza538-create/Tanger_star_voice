package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.*
import com.example.ui.MainNavTab
import com.example.ui.TangerViewModel
import com.example.ui.ads.TangerAdMobFeedBannerCard
import com.example.ui.theme.*

@Composable
fun HomeAndDiscoverScreen(
    isDiscoverMode: Boolean,
    viewModel: TangerViewModel,
    currentUser: UserEntity?,
    allUsers: List<UserEntity>,
    allRooms: List<VoiceRoomEntity>,
    allAgencies: List<AgencyEntity>,
    globalEvents: List<GlobalEventEntity>,
    luckyState: LuckyGiftStateEntity?,
    followingList: List<FollowEntity>,
    allGifts: List<GiftEntity> = emptyList()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRankingCategory by remember { mutableStateOf("Global") }
    var selectedCountryFilter by remember { mutableStateOf("ALL") }
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var giftTargetUserId by remember { mutableStateOf<String?>(null) }
    var showGlobalGiftCatalog by remember { mutableStateOf(false) }

    val rankingCategories = listOf(
        "Global", "Country", "User", "Gifter", "Receiver",
        "Room", "Agency", "Host", "Coin", "Diamond", "VIP", "Event"
    )

    val filteredUsers = remember(allUsers, searchQuery, selectedCountryFilter) {
        allUsers.filter { user ->
            val matchesQuery = searchQuery.isBlank() ||
                user.tangerUserId.contains(searchQuery, ignoreCase = true) ||
                user.username.contains(searchQuery, ignoreCase = true)
            val matchesCountry = selectedCountryFilter == "ALL" || user.countryCode == selectedCountryFilter
            matchesQuery && matchesCountry
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Banner Card with Tagline & Quick Actions
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_hero_card"),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_room_banner),
                        contentDescription = "Tanger Star Voice Global Stage Banner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        TangerEmeraldDark.copy(alpha = 0.55f),
                                        TangerEmeraldDark.copy(alpha = 0.92f)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = TangerGold.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(50),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, TangerGold)
                                ) {
                                    Text(
                                        text = "🌟 TANGER STAR VOICE GLOBAL",
                                        color = TangerGoldLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                if (luckyState != null) {
                                    Surface(
                                        color = TangerTurquoiseBright.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "🍀 Pool: ${luckyState.globalPoolCoins} Coins (${luckyState.globalHitCount} Hits)",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Column {
                                Text(
                                    text = "Voice Chat, Play Games, Find More Friends",
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Real-time Global Voice Rooms • Atomic Lucky Gifts • Ludo Tournaments",
                                    color = TangerMintSoft,
                                    fontSize = 12.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        giftTargetUserId = allUsers.firstOrNull { it.tangerUserId != currentUser?.tangerUserId }?.tangerUserId
                                        showGlobalGiftCatalog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TangerGold),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("hero_send_lucky_gift_button")
                                ) {
                                    Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Lucky Gift", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Button(
                                    onClick = {
                                        allRooms.firstOrNull()?.let { viewModel.selectAndEnterRoom(it.roomId) }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TangerTurquoiseBright),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("enter_top_room_button")
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Join Stage", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { showCreateRoomDialog = true },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, TangerMint),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("create_room_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Create Room", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Official Google AdMob Feed Banner (Safe Home/Discover Feed Placement Only)
        item {
            TangerAdMobFeedBannerCard(
                placementZone = if (isDiscoverMode) "DISCOVER_FEED" else "HOME_FEED"
            )
        }

        // 2. Global Search by Tanger Star User ID / Username & Country Filter
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("global_user_search_input"),
                        placeholder = { Text("Search TANGERAM User ID (e.g. 199967) or @username...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val countries = listOf("ALL" to "🌍 All Countries", "MA" to "🇲🇦 Morocco", "AE" to "🇦🇪 UAE", "SA" to "🇸🇦 Saudi", "TR" to "🇹🇷 Türkiye")
                        items(countries) { (code, label) ->
                            FilterChip(
                                selected = selectedCountryFilter == code,
                                onClick = { selectedCountryFilter = code },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        }

        // 3. Active Voice Rooms Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎙️ Live Global Voice Rooms (${allRooms.size})",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
                TextButton(onClick = { viewModel.selectTab(MainNavTab.ROOMS) }) {
                    Text("Open Stage →", fontWeight = FontWeight.Bold)
                }
            }
        }

        items(allRooms, key = { it.roomId }) { room ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectAndEnterRoom(room.roomId) }
                    .testTag("room_card_${room.roomId}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(TangerPrimaryGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = room.countryFlag, fontSize = 26.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = room.roomTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (room.isPrivate) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Private Room",
                                        tint = TangerGold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${room.roomTopic} • Host: ${room.ownerUsername}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = TangerMintSoft,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "🔥 ${room.totalRoomHeat} Heat",
                                        color = TangerEmeraldDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = Color(0xFFE0F2FE),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "👥 ${room.onlineCount} Online • ${room.themeName}",
                                        color = Color(0xFF0369A1),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    FilledTonalButton(
                        onClick = { viewModel.selectAndEnterRoom(room.roomId) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Join", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // 4. Server-Side 12-Category Rankings & Automatic Rewards
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rankings_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🏆 Server-Side Global Rankings",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "12 Authoritative Leaderboards • Duplicate-Protected Rewards",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { viewModel.claimRankingReward(selectedRankingCategory, 1) },
                            colors = ButtonDefaults.buttonColors(containerColor = TangerGold),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("claim_ranking_reward_button")
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Claim Reward", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(rankingCategories) { category ->
                            FilterChip(
                                selected = selectedRankingCategory == category,
                                onClick = { selectedRankingCategory = category },
                                label = { Text(category, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }

                    // Render Leaderboard Rows dynamically based on selected category
                    when (selectedRankingCategory) {
                        "Room" -> {
                            allRooms.sortedByDescending { it.totalRoomHeat }.forEachIndexed { idx, rm ->
                                RankingRowItem(
                                    rank = idx + 1,
                                    flag = rm.countryFlag,
                                    title = rm.roomTitle,
                                    subtitle = "Owner: ${rm.ownerUsername} (${rm.roomId})",
                                    metricLabel = "🔥 ${rm.totalRoomHeat} Heat"
                                )
                            }
                        }
                        "Agency" -> {
                            allAgencies.sortedByDescending { it.currentMonthlyDiamonds }.forEachIndexed { idx, ag ->
                                RankingRowItem(
                                    rank = idx + 1,
                                    flag = ag.countryFlag,
                                    title = ag.agencyName,
                                    subtitle = "Owner: ${ag.ownerUsername} • ${ag.hostCount} Hosts",
                                    metricLabel = "💎 ${ag.currentMonthlyDiamonds} / ${ag.monthlyDiamondTarget}"
                                )
                            }
                        }
                        "Event" -> {
                            globalEvents.forEachIndexed { idx, ev ->
                                RankingRowItem(
                                    rank = idx + 1,
                                    flag = "🎉",
                                    title = ev.title,
                                    subtitle = "Target: ${ev.targetScope} (${ev.targetValue})",
                                    metricLabel = ev.rewardInfo
                                )
                            }
                        }
                        else -> {
                            val sortedUsers = when (selectedRankingCategory) {
                                "Gifter", "Coin" -> filteredUsers.sortedByDescending { it.totalCoinsSpent }
                                "Receiver", "Diamond", "Host" -> filteredUsers.sortedByDescending { it.totalDiamondsEarned }
                                "VIP" -> filteredUsers.sortedByDescending { it.vipLevel * 1_000_000L + it.totalCoinsSpent }
                                "Country" -> filteredUsers.sortedWith(compareBy({ it.countryCode }, { -it.totalCoinsSpent }))
                                else -> filteredUsers.sortedByDescending { it.xpPoints + it.totalCoinsSpent }
                            }
                            sortedUsers.forEachIndexed { idx, usr ->
                                val metric = when (selectedRankingCategory) {
                                    "Gifter", "Coin" -> "🪙 ${usr.totalCoinsSpent} Spent"
                                    "Receiver", "Diamond", "Host" -> "💎 ${usr.totalDiamondsEarned} Earned"
                                    "VIP" -> "👑 VIP ${usr.vipLevel} • Lv.${usr.userLevel}"
                                    else -> "⭐ Lv.${usr.userLevel} • 🪙 ${usr.coinsBalance}"
                                }
                                RankingRowItem(
                                    rank = idx + 1,
                                    flag = usr.countryFlag,
                                    title = "${usr.username} (${usr.tangerUserId})",
                                    subtitle = "${usr.role} • ${usr.countryName} • ${usr.followersCount} Followers",
                                    metricLabel = metric
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Global Users Directory with Follow / Block / Report
        item {
            Text(
                text = if (isDiscoverMode) "🌍 Discover Global Users & Hosts (${filteredUsers.size})"
                else "👥 Global Community Directory",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp
            )
        }

        items(filteredUsers, key = { it.tangerUserId }) { usr ->
            val isFollowing = followingList.any { it.targetUserId == usr.tangerUserId }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_directory_card_${usr.tangerUserId}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(TangerBrightGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${usr.countryFlag}${usr.username.take(1)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(usr.username, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = if (usr.isOnline) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = if (usr.isOnline) "● ONLINE" else "○ OFFLINE",
                                            color = if (usr.isOnline) TangerEmerald else TextMutedSlate,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "ID: ${usr.tangerUserId} • VIP ${usr.vipLevel} • Lv.${usr.userLevel} • ${usr.role}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = usr.bio,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🪙 ${usr.coinsBalance} • 💎 ${usr.diamondsBalance} • 👥 ${usr.followersCount}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TangerEmerald
                        )
                        if (usr.tangerUserId != currentUser?.tangerUserId) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        giftTargetUserId = usr.tangerUserId
                                        showGlobalGiftCatalog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TangerGold),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("send_gift_to_user_${usr.tangerUserId}")
                                ) {
                                    Text("🎁 Gift", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                                }
                                FilledTonalButton(
                                    onClick = { viewModel.toggleFollow(usr.tangerUserId) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(if (isFollowing) "Following" else "+ Follow", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.blockUser(usr.tangerUserId) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Block", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.reportUser(
                                            usr.tangerUserId,
                                            "Community Report",
                                            "Reported ${usr.username} from Global Directory"
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Report", fontSize = 11.sp, color = TangerDangerRed)
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    giftTargetUserId = usr.tangerUserId
                                    showGlobalGiftCatalog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TangerGold),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("🎁 Lucky Gift", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGlobalGiftCatalog) {
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
            onDismiss = { showGlobalGiftCatalog = false }
        )
    }

    if (showCreateRoomDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newTopic by remember { mutableStateOf("Live Voice Chat & Lucky Gifts") }
        var isPrivate by remember { mutableStateOf(false) }
        var pinCode by remember { mutableStateOf("") }
        var theme by remember { mutableStateOf("Emerald Palace") }

        AlertDialog(
            onDismissRequest = { showCreateRoomDialog = false },
            title = { Text("Create Global Voice Room", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Room Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newTopic,
                        onValueChange = { newTopic = it },
                        label = { Text("Room Topic") },
                        singleLine = true
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPrivate, onCheckedChange = { isPrivate = it })
                        Text("Private Room with PIN")
                    }
                    if (isPrivate) {
                        OutlinedTextField(
                            value = pinCode,
                            onValueChange = { pinCode = it },
                            label = { Text("4-Digit PIN") },
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createNewVoiceRoom(newTitle, newTopic, isPrivate, pinCode, theme)
                    showCreateRoomDialog = false
                }) {
                    Text("Create & Enter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRoomDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RankingRowItem(
    rank: Int,
    flag: String,
    title: String,
    subtitle: String,
    metricLabel: String
) {
    val badgeColor = when (rank) {
        1 -> TangerGold
        2 -> Color(0xFF94A3B8)
        3 -> Color(0xFFD97706)
        else -> TangerTurquoise
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Text("#$rank", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(flag, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        Text(
            text = metricLabel,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = TangerEmerald
        )
    }
}
