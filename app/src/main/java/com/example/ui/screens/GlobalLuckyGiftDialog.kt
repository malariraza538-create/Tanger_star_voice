package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.GiftEntity
import com.example.data.local.LuckyGiftStateEntity
import com.example.data.local.UserEntity
import com.example.data.realtime.RealtimeEvent
import com.example.ui.TangerViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GlobalLuckyGiftRealTimeBannerOverlay(
    giftOverlay: RealtimeEvent.GiftBroadcast?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = giftOverlay != null,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.85f),
        modifier = modifier
    ) {
        giftOverlay?.let { gift ->
            val infiniteTransition = rememberInfiniteTransition(label = "global_gift_anim")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.96f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(480, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_scale"
            )

            val isLuckyTriggered = gift.luckyRewardGranted > 0 || gift.cashbackGranted > 0
            val containerBrush = if (isLuckyTriggered) {
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF022C22),
                        Color(0xFF047857),
                        Color(0xFFB45309)
                    )
                )
            } else {
                Brush.horizontalGradient(
                    listOf(
                        TangerEmeraldDark,
                        TangerEmerald,
                        TangerTurquoise
                    )
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .scale(pulseScale)
                    .clickable { onDismiss() }
                    .testTag("global_lucky_gift_realtime_overlay"),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(
                    width = 2.dp,
                    color = if (isLuckyTriggered) TangerGoldLight else TangerTurquoiseBright
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(containerBrush)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                    .clip(CircleShape)
                                    .background(TangerGoldGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = gift.giftEmoji,
                                    fontSize = 30.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = if (isLuckyTriggered) TangerGold else TangerTurquoiseBright,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = if (isLuckyTriggered) "🎰 LUCKY TRIGGER: ${gift.milestoneReached}" else "✨ ${gift.animationEffect}",
                                            color = Color.Black,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = if (gift.roomId == "GLOBAL") "🌍 GLOBAL" else "🎙️ ${gift.roomId}",
                                        color = TangerMintSoft,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${gift.senderName} ➔ ${gift.receiverName}",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Sent ${gift.quantity}x ${gift.giftName} (🪙 ${gift.totalCoins} Coins)",
                                    color = TangerGoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                if (gift.luckyRewardGranted > 0 || gift.cashbackGranted > 0 || gift.expGranted > 0) {
                                    Text(
                                        text = "🎉 Reward: +${gift.luckyRewardGranted} 🪙 • Cashback: +${gift.cashbackGranted} 🪙 • +${gift.expGranted} EXP",
                                        color = Color(0xFFFEF08A),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss Gift Animation",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalGiftCatalogDialog(
    currentUser: UserEntity?,
    allUsers: List<UserEntity>,
    allGifts: List<GiftEntity>,
    luckyState: LuckyGiftStateEntity?,
    initialReceiverUserId: String? = null,
    contextRoomId: String = "GLOBAL",
    onSendGift: (receiverUserId: String, giftId: String, quantity: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val eligibleReceivers = remember(allUsers) {
        allUsers
    }
    var selectedReceiverId by remember(initialReceiverUserId, eligibleReceivers, currentUser) {
        mutableStateOf(
            initialReceiverUserId
                ?: eligibleReceivers.firstOrNull { it.tangerUserId != currentUser?.tangerUserId }?.tangerUserId
                ?: currentUser?.tangerUserId
                ?: "199967"
        )
    }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var selectedQuantity by remember { mutableIntStateOf(1) }

    val availableGifts = remember(allGifts, selectedCategoryFilter) {
        allGifts.filter { g ->
            g.isActive && (selectedCategoryFilter == "ALL" || g.category.equals(selectedCategoryFilter, ignoreCase = true))
        }
    }

    var selectedGift by remember(availableGifts) {
        mutableStateOf(
            availableGifts.firstOrNull { it.category == "Lucky Gift" } ?: availableGifts.firstOrNull()
        )
    }

    val categories = listOf(
        "ALL",
        "Lucky Gift",
        "Lucky Draw",
        "Gift",
        "Event Gift",
        "CP",
        "Country/Flag Gift"
    )

    val dateFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .testTag("global_gift_catalog_dialog"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🎁 TANGERAM Global Gift & Lucky Engine",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = TangerEmeraldDark
                        )
                        Text(
                            text = "Your Wallet: 🪙 ${currentUser?.coinsBalance ?: 0} Coins • 💎 ${currentUser?.diamondsBalance ?: 0} Diamonds • EXP: ${currentUser?.xpPoints ?: 0}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TangerEmerald
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_global_gift_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Global Lucky Pool & Counter Status Bar
                if (luckyState != null) {
                    Surface(
                        color = TangerEmeraldDark,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "🍀 Global Lucky Pool: 🪙 ${luckyState.globalPoolCoins} Coins • Total Hits: ${luckyState.globalHitCount}",
                                    color = TangerGoldLight,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Milestones: Every ${luckyState.milestone1Threshold}x (+${luckyState.milestone1RewardCoins}🪙) • Jackpot ${luckyState.jackpotThreshold}x (+${luckyState.jackpotRewardCoins}🪙)",
                                    color = TangerMintSoft,
                                    fontSize = 10.sp
                                )
                            }
                            Surface(
                                color = if (luckyState.isGlobalLuckyPaused) TangerDangerRed else TangerTurquoiseBright,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = if (luckyState.isGlobalLuckyPaused) "PAUSED" else "LIVE",
                                    color = if (luckyState.isGlobalLuckyPaused) Color.White else Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Receiver Selector (Any Global User)
                Text(
                    text = "1. Select Any Global User Receiver:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(eligibleReceivers, key = { it.tangerUserId }) { user ->
                        val isSelected = selectedReceiverId == user.tangerUserId
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedReceiverId = user.tangerUserId },
                            label = {
                                Text(
                                    text = "${user.countryFlag} ${user.username} (#${user.tangerUserId})",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            },
                            modifier = Modifier.testTag("gift_receiver_chip_${user.tangerUserId}")
                        )
                    }
                }

                // Category Filter
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = {
                                selectedCategoryFilter = cat
                                val nextFirst = allGifts.firstOrNull {
                                    it.isActive && (cat == "ALL" || it.category.equals(cat, ignoreCase = true))
                                }
                                if (nextFirst != null) selectedGift = nextFirst
                            },
                            label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                // Gifts List with Lucky Specs
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 180.dp, max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableGifts, key = { it.giftId }) { gift ->
                        val isSelected = selectedGift?.giftId == gift.giftId
                        val isLuckyType = gift.category == "Lucky Gift" || gift.category == "Lucky Draw"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedGift = gift }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) TangerEmerald else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .testTag("global_gift_item_${gift.giftId}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) TangerMintSoft.copy(alpha = 0.65f)
                                else MaterialTheme.colorScheme.background
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(if (isLuckyType) TangerGoldGradient else TangerPrimaryGradient),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(gift.iconEmoji, fontSize = 22.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = gift.name,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp
                                            )
                                            Surface(
                                                color = if (isLuckyType) TangerGold else TangerTurquoise.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = gift.category,
                                                    color = if (isLuckyType) Color.Black else TangerEmeraldDark,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                            if (gift.isPaused) {
                                                Surface(
                                                    color = TangerDangerRed,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "PAUSED",
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "Trigger: ${gift.currentHitCount}/${gift.triggerCount}x ➔ +${gift.rewardCoins}🪙 Reward • Cashback: +${gift.cashbackCoins}🪙 • +${gift.expBonus} EXP",
                                            fontSize = 10.sp,
                                            color = TangerEmeraldDark,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Max Reward: ${gift.maxRewardCoins}🪙 • Activations: ${gift.currentActivations}/${if (gift.activationLimit > 0) gift.activationLimit else "∞"} • Cooldown: ${gift.cooldownSeconds}s • Repeat: ${gift.repeatTimingSeconds}s",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (gift.lastTriggeredAt > 0L) {
                                            Text(
                                                text = "Last Triggered: ${dateFormatter.format(Date(gift.lastTriggeredAt))} • ${gift.backupTimingNotes}",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "🪙 ${gift.coinPrice}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = TangerEmeraldDark
                                    )
                                    Text(
                                        text = "${gift.diamondReturnRatePercent}% 💎",
                                        fontSize = 10.sp,
                                        color = TangerTurquoise
                                    )
                                }
                            }
                        }
                    }
                }

                // Quantity Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quantity Multiplier:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 5, 10, 25, 50, 99).forEach { qty ->
                            FilterChip(
                                selected = selectedQuantity == qty,
                                onClick = { selectedQuantity = qty },
                                label = { Text("x$qty", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("global_gift_qty_$qty")
                            )
                        }
                    }
                }

                // Atomic Send Button
                val currentGift = selectedGift
                val totalCoins = (currentGift?.coinPrice ?: 0L) * selectedQuantity
                val expectedExp = (currentGift?.expBonus ?: 0L) * selectedQuantity
                val targetUser = eligibleReceivers.find { it.tangerUserId == selectedReceiverId }

                Button(
                    onClick = {
                        if (currentGift != null) {
                            onSendGift(selectedReceiverId, currentGift.giftId, selectedQuantity)
                            onDismiss()
                        }
                    },
                    enabled = currentGift != null,
                    colors = ButtonDefaults.buttonColors(containerColor = TangerEmerald),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 50.dp)
                        .testTag("confirm_send_global_gift_button")
                ) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentGift != null) {
                            "Send ${selectedQuantity}x ${currentGift.iconEmoji} ${currentGift.name} to @${targetUser?.username ?: selectedReceiverId} (🪙 $totalCoins • +$expectedExp EXP)"
                        } else {
                            "Select a Gift"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
