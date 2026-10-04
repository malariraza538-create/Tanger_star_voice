package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.*
import com.example.ui.TangerViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WalletAndDiamondsScreen(
    viewModel: TangerViewModel,
    currentUser: UserEntity?,
    transactions: List<WalletTransactionEntity>,
    diamondCycles: List<DiamondMonthlyCycleEntity>,
    vipConfigs: List<VipConfigEntity>,
    vipHistory: List<VipHistoryEntity>,
    luckyState: LuckyGiftStateEntity?,
    luckyAudits: List<LuckyGiftAuditEntity>
) {
    val activeCycle = diamondCycles.firstOrNull { !it.isArchived } ?: diamondCycles.firstOrNull()
    val archivedCycles = diamondCycles.filter { it.isArchived }

    val filteredTransactions by viewModel.filteredUserTransactions.collectAsStateWithLifecycle()
    val txTypeFilter by viewModel.walletTxTypeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.walletSearchQuery.collectAsStateWithLifecycle()
    val verificationReport by viewModel.walletVerificationReport.collectAsStateWithLifecycle()

    var selectedDiamondExchangeAmount by remember { mutableLongStateOf(500L) }
    var customDiamondExchangeInput by remember { mutableStateOf("500") }
    var customIdempotencyKeyInput by remember { mutableStateOf("") }
    var selectedTransactionDetail by remember { mutableStateOf<WalletTransactionEntity?>(null) }

    val txCategories = listOf(
        "ALL" to "All Ledger",
        "GIFT_SENT" to "🎁 Gifts Sent",
        "GIFT_RECEIVED_DIAMOND" to "💎 Diamonds Earned",
        "DIAMOND_EXCHANGE" to "🔄 Exchanges",
        "VIP_DAILY_BONUS" to "👑 VIP Bonus",
        "OWNER_ISSUE_COINS" to "🏦 Treasury",
        "GAME_ENTRY" to "🎲 Game Entry",
        "GAME_REWARD" to "🏆 Game Wins",
        "RANKING_REWARD" to "🥇 Rankings"
    )

    val dateFormatter = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Server-Controlled Dual Wallet Card (Coins & Diamonds + VIP Daily Bonus Claim)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_summary_card"),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TangerPrimaryGradient)
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "🏦 TANGERAM Server-Validated Atomic Wallet",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Owner: ${currentUser?.fullName ?: "User"} (@${currentUser?.username ?: "star"} • ID #${currentUser?.tangerUserId ?: "199967"})",
                                    color = TangerMintSoft,
                                    fontSize = 11.sp
                                )
                            }
                            Surface(color = TangerGold, shape = RoundedCornerShape(50)) {
                                Text(
                                    text = "👑 VIP ${currentUser?.vipLevel ?: 1}",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("wallet_coins_balance_box"),
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("🪙 Gold Coins", color = TangerGoldLight, fontSize = 12.sp)
                                    Text(
                                        text = "${currentUser?.coinsBalance ?: 0}",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 22.sp
                                    )
                                    Text(
                                        text = "Total Spent: ${currentUser?.totalCoinsSpent ?: 0}",
                                        color = TangerMintSoft,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("wallet_diamonds_balance_box"),
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("💎 Host Diamonds", color = TangerDiamondBlue, fontSize = 12.sp)
                                    Text(
                                        text = "${currentUser?.diamondsBalance ?: 0}",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 22.sp
                                    )
                                    Text(
                                        text = "Total Earned: ${currentUser?.totalDiamondsEarned ?: 0}",
                                        color = TangerMintSoft,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.claimVipDailyBonus() },
                                colors = ButtonDefaults.buttonColors(containerColor = TangerGold),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("claim_vip_daily_bonus_button")
                            ) {
                                Icon(
                                    Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Claim Daily VIP Bonus",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.verifyWalletLedgerHistory() },
                                border = androidx.compose.foundation.BorderStroke(1.dp, TangerMint),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("verify_wallet_ledger_button")
                            ) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Verify Ledger Integrity",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Atomic Diamond-to-Coin Exchange Panel (With Idempotency Key Protection)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("diamond_to_coin_exchange_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🔄 Atomic Diamond → Gold Coin Exchange",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Server-validated 70% conversion rate • Protected by Room @Transaction & unique Idempotency Key.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(color = TangerMintSoft, shape = RoundedCornerShape(50)) {
                            Text(
                                text = "100 💎 = 70 🪙",
                                color = TangerEmeraldDark,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(100L, 500L, 1_000L, 5_000L)) { amt ->
                            FilterChip(
                                selected = selectedDiamondExchangeAmount == amt,
                                onClick = {
                                    selectedDiamondExchangeAmount = amt
                                    customDiamondExchangeInput = amt.toString()
                                },
                                label = {
                                    Text(
                                        text = "💎 $amt → 🪙 ${(amt * 70L) / 100L}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                modifier = Modifier.testTag("exchange_amount_chip_$amt")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customDiamondExchangeInput,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            customDiamondExchangeInput = digits
                            digits.toLongOrNull()?.let { selectedDiamondExchangeAmount = it }
                        },
                        label = { Text("Diamonds to Exchange (Min 100 💎 → +${(selectedDiamondExchangeAmount * 70L) / 100L} 🪙 Coins)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exchange_diamond_amount_input")
                    )

                    OutlinedTextField(
                        value = customIdempotencyKeyInput,
                        onValueChange = { customIdempotencyKeyInput = it },
                        label = { Text("Optional Custom Idempotency Key (Test Duplicate Protection)") },
                        placeholder = { Text("Leave blank for auto UUID key or enter fixed key to test replay block") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exchange_idempotency_key_input")
                    )

                    Button(
                        onClick = {
                            viewModel.exchangeDiamondsToCoins(
                                diamondsAmount = selectedDiamondExchangeAmount,
                                customIdempotencyKey = customIdempotencyKeyInput
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TangerEmeraldLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("execute_diamond_exchange_button")
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Convert 💎 $selectedDiamondExchangeAmount Diamonds to 🪙 ${(selectedDiamondExchangeAmount * 70L) / 100L} Coins Atomically",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 3. Relational Ledger Audit & Balance-After Verification Card
        if (verificationReport != null) {
            item {
                val rep = verificationReport!!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_verification_report_card"),
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
                            Text(
                                text = "🛡️ Relational Ledger Verification & Inflow/Outflow Summary",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            Surface(
                                color = if (rep.ledgerConsistencyVerified) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = if (rep.ledgerConsistencyVerified) "✔ SNAPSHOT VERIFIED" else "⚠ MISMATCH",
                                    color = if (rep.ledgerConsistencyVerified) TangerEmerald else TangerDangerRed,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🪙 Inflow: +${rep.totalCoinsInflow} | Outflow: -${rep.totalCoinsOutflow}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TangerEmerald
                            )
                            Text(
                                text = "💎 Inflow: +${rep.totalDiamondsInflow} | Outflow: -${rep.totalDiamondsOutflow}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TangerTurquoise
                            )
                        }
                        Text(
                            text = "Session Auth: ${if (rep.sessionVerified) "ACTIVE" else "INACTIVE"} • Total Relational Rows: ${rep.totalLedgerEntries} • Verified at ${dateFormatter.format(Date(rep.verifiedAtMillis))}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4. Monthly Diamond Cycle Engine (1st to Last Day: 28/29/30/31-day support + Auto-Archive)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("monthly_diamond_cycle_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "📅 Monthly Diamond Target & Cycle Engine",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Starts on the 1st of every month and ends on the last day (28/29/30/31 days). Resets automatically on new month while archiving prior history.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (activeCycle != null) {
                        val progress = (activeCycle.earnedDiamondsInCycle.toFloat() / activeCycle.monthlyTargetDiamonds.coerceAtLeast(1L)).coerceIn(0f, 1f)
                        Surface(
                            color = TangerMintSoft,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Active Cycle: ${activeCycle.yearMonth} (1st – ${activeCycle.daysInMonth}th)",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TangerEmeraldDark,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TangerEmerald
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = TangerEmeraldLight
                                )
                                Text(
                                    text = "💎 ${activeCycle.earnedDiamondsInCycle} / ${activeCycle.monthlyTargetDiamonds} Target • ${activeCycle.daysInMonth}-Day Month",
                                    fontSize = 12.sp,
                                    color = TangerEmeraldDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Text("Verify Calendar Month Lengths (28 / 29 / 30 / 31 Days):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            AssistChip(
                                onClick = { viewModel.testMonthBoundaryResetSimulation(2026, 1) },
                                label = { Text("Feb 2026 (28d)", fontSize = 11.sp) }
                            )
                        }
                        item {
                            AssistChip(
                                onClick = { viewModel.testMonthBoundaryResetSimulation(2028, 1) },
                                label = { Text("Feb 2028 Leap (29d)", fontSize = 11.sp) }
                            )
                        }
                        item {
                            AssistChip(
                                onClick = { viewModel.testMonthBoundaryResetSimulation(2026, 8) },
                                label = { Text("Sep 2026 (30d)", fontSize = 11.sp) }
                            )
                        }
                        item {
                            AssistChip(
                                onClick = { viewModel.testMonthBoundaryResetSimulation(2026, 9) },
                                label = { Text("Oct 2026 (31d)", fontSize = 11.sp) }
                            )
                        }
                    }

                    if (archivedCycles.isNotEmpty()) {
                        Text("Archived Monthly Diamond History:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        archivedCycles.forEach { arch ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("📦 ${arch.yearMonth} (${arch.daysInMonth} days) [ARCHIVED]", fontSize = 12.sp)
                                Text("💎 ${arch.earnedDiamondsInCycle} / ${arch.monthlyTargetDiamonds}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 5. Authoritative Global Lucky Gift Counter & Audit Trail
        item {
            if (luckyState != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🍀 Authoritative Global Lucky Gift Counter", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        Text(
                            text = "Global Pool: 🪙 ${luckyState.globalPoolCoins} • Total Hits: ${luckyState.globalHitCount}",
                            fontWeight = FontWeight.Bold,
                            color = TangerEmerald
                        )
                        Text(
                            text = "Milestones: Every ${luckyState.milestone1Threshold}x → +${luckyState.milestone1RewardCoins} Coins | Every ${luckyState.milestone2Threshold}x → +${luckyState.milestone2RewardCoins} Coins | Jackpot ${luckyState.jackpotThreshold}x → +${luckyState.jackpotRewardCoins} Coins",
                            fontSize = 11.sp
                        )
                        luckyAudits.take(3).forEach { aud ->
                            Text(
                                text = "✅ ${aud.username} hit #${aud.globalCounterValueAtHit} (${aud.milestoneReached}) → +${aud.rewardCoinsGranted} Coins",
                                fontSize = 11.sp,
                                color = TangerGold
                            )
                        }
                    }
                }
            }
        }

        // 6. Configurable VIP Tiers & Qualification
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("👑 VIP Qualification Tiers & Frames", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    vipConfigs.forEach { vip ->
                        val unlocked = (currentUser?.vipLevel ?: 0) >= vip.vipLevel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(vip.vipTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${vip.badgeName} • ${vip.frameStyle} • Daily Bonus: +${vip.dailyBonusCoins} 🪙", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = if (unlocked) "✅ UNLOCKED" else "🪙 ${vip.requiredCoins}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (unlocked) TangerEmerald else TangerGold
                            )
                        }
                    }
                }
            }
        }

        // 7. Secure Relational Transaction History Filter & Search Controls
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_history_filter_card"),
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
                            text = "📜 Secure Relational Transaction History (${filteredTransactions.size})",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        if (txTypeFilter != "ALL" || searchQuery.isNotBlank()) {
                            TextButton(onClick = {
                                viewModel.setWalletTxTypeFilter("ALL")
                                viewModel.setWalletSearchQuery("")
                            }) {
                                Text("Reset Filters", fontSize = 11.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setWalletSearchQuery(it) },
                        placeholder = { Text("Search TX ID, Idempotency Key, or description...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Ledger") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setWalletSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wallet_history_search_input")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(txCategories) { (typeCode, label) ->
                            FilterChip(
                                selected = txTypeFilter == typeCode,
                                onClick = { viewModel.setWalletTxTypeFilter(typeCode) },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.testTag("wallet_filter_chip_$typeCode")
                            )
                        }
                    }
                }
            }
        }

        if (filteredTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TangerTurquoise, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matching ledger transactions found", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Try clearing your search query or switching transaction category filters.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(filteredTransactions, key = { it.txId }) { tx ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTransactionDetail = tx }
                        .testTag("wallet_tx_item_${tx.txId}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = TangerMintSoft,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = tx.txType,
                                    color = TangerEmeraldDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = dateFormatter.format(Date(tx.timestamp)),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.description,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "TX: ${tx.txId} • Idempotency: ${tx.idempotencyKey.take(22)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                if (tx.coinsDelta != 0L) {
                                    Text(
                                        text = "${if (tx.coinsDelta > 0) "+" else ""}${tx.coinsDelta} 🪙",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (tx.coinsDelta > 0) TangerEmerald else TangerDangerRed,
                                        fontSize = 13.sp
                                    )
                                }
                                if (tx.diamondsDelta != 0L) {
                                    Text(
                                        text = "${if (tx.diamondsDelta > 0) "+" else ""}${tx.diamondsDelta} 💎",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (tx.diamondsDelta > 0) TangerTurquoise else TangerDangerRed,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Counterparty: ${tx.counterpartyUserId ?: "SYSTEM"}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Balance After → 🪙 ${tx.coinsBalanceAfter} | 💎 ${tx.diamondsBalanceAfter}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TangerEmeraldDark
                            )
                        }
                    }
                }
            }
        }
    }

    // Cryptographic & Relational Transaction Receipt Inspector Dialog
    if (selectedTransactionDetail != null) {
        val tx = selectedTransactionDetail!!
        AlertDialog(
            onDismissRequest = { selectedTransactionDetail = null },
            title = {
                Text("🧾 Verified Relational Ledger Receipt", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Transaction ID: ${tx.txId}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Idempotency Key: ${tx.idempotencyKey}", fontSize = 12.sp, color = TangerEmerald)
                    Text("User ID: ${tx.userId}", fontSize = 12.sp)
                    Text("Counterparty: ${tx.counterpartyUserId ?: "SYSTEM"}", fontSize = 12.sp)
                    Text("Operation Type: ${tx.txType}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Coins Delta: ${if (tx.coinsDelta > 0) "+" else ""}${tx.coinsDelta} 🪙", fontSize = 12.sp)
                    Text("Diamonds Delta: ${if (tx.diamondsDelta > 0) "+" else ""}${tx.diamondsDelta} 💎", fontSize = 12.sp)
                    Text("Snapshot Coins Balance After: ${tx.coinsBalanceAfter} 🪙", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Snapshot Diamonds Balance After: ${tx.diamondsBalanceAfter} 💎", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Description: ${tx.description}", fontSize = 12.sp)
                    Text("Timestamp: ${dateFormatter.format(Date(tx.timestamp))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = { selectedTransactionDetail = null }) {
                    Text("Close Receipt")
                }
            }
        )
    }
}

@Composable
fun EventsAndAgenciesScreen(
    viewModel: TangerViewModel,
    currentUser: UserEntity?,
    events: List<GlobalEventEntity>,
    agencies: List<AgencyEntity>,
    agencyMembers: List<AgencyMemberEntity>,
    invitations: List<AgencyInvitationEntity>
) {
    var selectedScopeFilter by remember { mutableStateOf("ALL") }
    var inviteUserIdInput by remember { mutableStateOf("199971") }

    val filteredEvents = remember(events, selectedScopeFilter, currentUser) {
        events.filter { ev ->
            selectedScopeFilter == "ALL" || ev.targetScope == selectedScopeFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Global Events, Banners & Official Announcements
        item {
            Text("🎉 Global Events, Banners & Official Announcements", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("ALL", "COUNTRY", "AGENCY", "ROOM", "VIP", "USER")) { scope ->
                    FilterChip(
                        selected = selectedScopeFilter == scope,
                        onClick = { selectedScopeFilter = scope },
                        label = { Text("Target: $scope", fontSize = 11.sp) }
                    )
                }
            }
        }

        items(filteredEvents, key = { it.eventId }) { ev ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("global_event_card_${ev.eventId}"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(color = TangerMintSoft, shape = RoundedCornerShape(50)) {
                            Text(
                                text = "${ev.contentType} • Scope: ${ev.targetScope} (${ev.targetValue})",
                                color = TangerEmeraldDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(ev.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(ev.subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("🎁 Reward: ${ev.rewardInfo}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TangerGold)
                }
            }
        }

        // 2. Pending Agency Invitations for Current User
        if (invitations.isNotEmpty()) {
            item {
                Text("💌 Your Agency & Host Invitations", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
            items(invitations, key = { it.inviteId }) { inv ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TangerMintSoft)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${inv.agencyName} invited you as ${inv.roleOffered}", fontWeight = FontWeight.Bold, color = TangerEmeraldDark)
                            Text("Status: ${inv.status}", fontSize = 11.sp, color = TangerEmerald)
                        }
                        if (inv.status == "PENDING") {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { viewModel.respondToInvitation(inv, true) }) {
                                    Text("Accept", fontSize = 11.sp)
                                }
                                OutlinedButton(onClick = { viewModel.respondToInvitation(inv, false) }) {
                                    Text("Decline", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Agencies & Hosts Management (Feature #10)
        item {
            Text("🏢 Global Agencies, Hosts & Diamond Targets", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
        }

        items(agencies, key = { it.agencyId }) { ag ->
            val dailyProgress = (ag.currentDailyDiamonds.toFloat() / ag.dailyDiamondTarget.coerceAtLeast(1L)).coerceIn(0f, 1f)
            val monthlyProgress = (ag.currentMonthlyDiamonds.toFloat() / ag.monthlyDiamondTarget.coerceAtLeast(1L)).coerceIn(0f, 1f)
            val membersOfAgency = agencyMembers.filter { it.agencyId == ag.agencyId }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("agency_card_${ag.agencyId}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${ag.countryFlag} ${ag.agencyName} (${ag.agencyCode})", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text("Agency Owner: ${ag.ownerUsername} • ${ag.hostCount} Hosts • ${ag.memberCount} Members", fontSize = 11.sp)
                        }
                        Surface(color = TangerGoldLight, shape = RoundedCornerShape(50)) {
                            Text(
                                text = "${ag.commissionPercent}% Share",
                                color = Color(0xFF78350F),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text("Daily Diamond Target: 💎 ${ag.currentDailyDiamonds} / ${ag.dailyDiamondTarget}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(
                        progress = { dailyProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = TangerTurquoiseBright
                    )

                    Text("Monthly Diamond Target: 💎 ${ag.currentMonthlyDiamonds} / ${ag.monthlyDiamondTarget}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(
                        progress = { monthlyProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = TangerEmeraldLight
                    )

                    Text("Active Agency Hosts (${membersOfAgency.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    membersOfAgency.forEach { mem ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🎙️ ${mem.username} (${mem.memberRole})", fontSize = 12.sp)
                            Text("💎 ${mem.monthlyEarnedDiamonds} / ${mem.monthlyTargetDiamonds}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inviteUserIdInput,
                            onValueChange = { inviteUserIdInput = it },
                            label = { Text("Invite User ID (e.g. TS-100005)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(onClick = { viewModel.inviteUserToAgency(ag.agencyId, inviteUserIdInput, "HOST") }) {
                            Text("Invite Host", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
