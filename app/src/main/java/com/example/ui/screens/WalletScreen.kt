package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ads.BannerAdCard
import com.example.core.Constants
import com.example.data.entity.WalletTransaction
import com.example.data.entity.Withdrawal
import com.example.ui.components.GlassCard
import com.example.ui.components.Neon3DButton
import com.example.ui.theme.DarkBgCard
import com.example.ui.theme.DarkBgPrimary
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderGold
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.RewardGold
import com.example.ui.theme.RewardGoldGradient
import com.example.ui.theme.RewardGoldLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WalletScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val walletBalance by viewModel.walletBalance.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val withdrawals by viewModel.withdrawals.collectAsState()

    var showWithdrawalDialog by remember { mutableStateOf(false) }
    var withdrawalFeedbackMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 75.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "WALLET & REWARDS",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Test/Demo Disclaimer Card
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFF141926),
                        borderColor = Color(0xFF26334D)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "TEST / DEMO REWARD MODE: Simulated wallet ledger for preview. No real monetary transactions are processed in demo mode.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Balance Card
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = GlassBorderGold,
                        backgroundColor = Color(0xFF131B2E),
                        cornerRadius = 20.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "AVAILABLE BALANCE",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹${walletBalance.availableBalance.toInt()}",
                                color = RewardGoldLight,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Color(0xFF1F2B42))
                            Spacer(modifier = Modifier.height(14.dp))

                            // Breakdown Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total Earned", color = TextSecondary, fontSize = 11.sp)
                                    Text("₹${walletBalance.totalEarned.toInt()}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Joining Bonus", color = TextSecondary, fontSize = 11.sp)
                                    Text("₹${walletBalance.joiningBonus.toInt()}", color = ElectricBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Level Rewards", color = TextSecondary, fontSize = 11.sp)
                                    Text("₹${walletBalance.levelRewards.toInt()}", color = NeonEmerald, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Withdrawable", color = TextSecondary, fontSize = 11.sp)
                                    Text("₹${walletBalance.withdrawableBalance.toInt()}", color = RewardGoldLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Withdrawal Button
                            val canWithdraw = walletBalance.availableBalance >= Constants.MINIMUM_WITHDRAWAL_AMOUNT
                            Neon3DButton(
                                text = if (canWithdraw) "Withdraw Funds" else "Minimum Withdrawal: ₹50",
                                onClick = {
                                    if (canWithdraw) {
                                        showWithdrawalDialog = true
                                    }
                                },
                                enabled = canWithdraw,
                                gradient = if (canWithdraw) RewardGoldGradient else Brush.horizontalGradient(listOf(Color(0xFF334155), Color(0xFF1E293B))),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (!canWithdraw) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val needed = (Constants.MINIMUM_WITHDRAWAL_AMOUNT - walletBalance.availableBalance).toInt()
                                Text(
                                    text = "Complete $needed more levels to unlock withdrawal!",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Withdrawals History (if any)
                if (withdrawals.isNotEmpty()) {
                    item {
                        Text(
                            text = "WITHDRAWAL REQUESTS",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(withdrawals) { wth ->
                        WithdrawalItemCard(wth)
                    }
                }

                // Transaction Ledger Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRANSACTION LEDGER",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${transactions.size} Records",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                if (transactions.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No transactions yet. Complete levels to earn rewards!",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(transactions) { tx ->
                        TransactionItemCard(tx)
                    }
                }
            }
        }

        // Bottom Banner Ad
        BannerAdCard(modifier = Modifier.align(Alignment.BottomCenter))

        // Withdrawal Dialog
        if (showWithdrawalDialog) {
            WithdrawalModal(
                availableBalance = walletBalance.availableBalance,
                onDismiss = { showWithdrawalDialog = false },
                onSubmit = { amount, method, details ->
                    viewModel.submitWithdrawal(amount, method, details) { res ->
                        showWithdrawalDialog = false
                        withdrawalFeedbackMessage = if (res.isSuccess) {
                            "Withdrawal request created! ID: ${res.getOrNull()}"
                        } else {
                            res.exceptionOrNull()?.message ?: "Withdrawal failed"
                        }
                    }
                }
            )
        }

        // Feedback Notice
        if (withdrawalFeedbackMessage != null) {
            Dialog(onDismissRequest = { withdrawalFeedbackMessage = null }) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    cornerRadius = 20.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "STATUS",
                            color = ElectricBlue,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = withdrawalFeedbackMessage ?: "",
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Neon3DButton(
                            text = "OK",
                            onClick = { withdrawalFeedbackMessage = null },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionItemCard(tx: WalletTransaction) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
    val isCredit = tx.type in listOf("JOINING_BONUS", "LEVEL_REWARD", "REFUND")

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        backgroundColor = Color(0xFF101726)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isCredit) Color(0x2210B981) else Color(0x22EF4444)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCredit) Icons.Default.MonetizationOn else Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = if (isCredit) NeonEmerald else NeonCoral,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = tx.referenceNote.ifEmpty { tx.type },
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ID: ${tx.transactionId.take(16)} • $dateStr",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Text(
                text = "${if (isCredit) "+" else "-"}₹${tx.amount.toInt()}",
                color = if (isCredit) NeonEmerald else NeonCoral,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun WithdrawalItemCard(wth: Withdrawal) {
    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(wth.requestTimestamp))
    val statusColor = when (wth.status) {
        "APPROVED", "COMPLETED" -> NeonEmerald
        "REJECTED" -> NeonCoral
        else -> RewardGold
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        backgroundColor = Color(0xFF101726)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "${wth.payoutMethod}: ${wth.payoutDetails}",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$dateStr • ID: ${wth.id.take(12)}",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
                Text(
                    text = wth.remarks,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${wth.amount.toInt()}",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = wth.status,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun WithdrawalModal(
    availableBalance: Double,
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, method: String, details: String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("UPI") }
    var details by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("50") }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "REQUEST WITHDRAWAL (TEST)",
                    color = RewardGoldLight,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Available: ₹${availableBalance.toInt()} • Min: ₹50",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Method selection
                Text("Select Method", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("UPI", "PAYTM", "BANK").forEach { method ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedMethod = method }
                        ) {
                            RadioButton(
                                selected = (selectedMethod == method),
                                onClick = { selectedMethod = method },
                                colors = RadioButtonDefaults.colors(selectedColor = ElectricBlue)
                            )
                            Text(text = method, color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = Color(0xFF2E3D5C)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Details (e.g. UPI ID or Account Number)
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text(if (selectedMethod == "UPI") "UPI ID (e.g. user@okhdfc)" else "Account / Wallet Number") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = Color(0xFF2E3D5C)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Neon3DButton(
                    text = "Submit Request",
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 50.0
                        if (details.isNotBlank()) {
                            onSubmit(amount, selectedMethod, details)
                        }
                    },
                    gradient = RewardGoldGradient,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Cancel",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable(onClick = onDismiss)
                        .padding(8.dp)
                )
            }
        }
    }
}
