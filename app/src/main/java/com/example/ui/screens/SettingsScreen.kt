package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.PuzzleApplication
import com.example.ads.BannerAdCard
import com.example.core.Constants
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBgPrimary
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.RewardGoldLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as PuzzleApplication
    val soundHaptic = app.soundHapticManager
    val settings by viewModel.appSettings.collectAsState()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

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
                    text = "SETTINGS",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Audio & Haptics
                item {
                    Text(
                        text = "AUDIO & FEEDBACK",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            SettingToggleItem(
                                title = "Sound Effects",
                                icon = Icons.AutoMirrored.Filled.VolumeUp,
                                checked = settings?.soundEnabled ?: true,
                                onCheckedChange = {
                                    val current = settings ?: return@SettingToggleItem
                                    val updated = current.copy(soundEnabled = it)
                                    soundHaptic.isSoundEnabled = it
                                    viewModel.updateSettings(updated)
                                }
                            )

                            SettingToggleItem(
                                title = "Background Music",
                                icon = Icons.Default.MusicNote,
                                checked = settings?.musicEnabled ?: true,
                                onCheckedChange = {
                                    val current = settings ?: return@SettingToggleItem
                                    val updated = current.copy(musicEnabled = it)
                                    soundHaptic.isMusicEnabled = it
                                    if (it) {
                                        soundHaptic.startBackgroundMusic()
                                    } else {
                                        soundHaptic.stopBackgroundMusic()
                                    }
                                    viewModel.updateSettings(updated)
                                }
                            )

                            SettingToggleItem(
                                title = "Haptic Vibration",
                                icon = Icons.Default.Vibration,
                                checked = settings?.vibrationEnabled ?: true,
                                onCheckedChange = {
                                    val current = settings ?: return@SettingToggleItem
                                    val updated = current.copy(vibrationEnabled = it)
                                    soundHaptic.isVibrationEnabled = it
                                    viewModel.updateSettings(updated)
                                }
                            )

                            SettingToggleItem(
                                title = "Notifications",
                                icon = Icons.Default.Notifications,
                                checked = settings?.notificationsEnabled ?: true,
                                onCheckedChange = {
                                    val current = settings ?: return@SettingToggleItem
                                    viewModel.updateSettings(current.copy(notificationsEnabled = it))
                                }
                            )
                        }
                    }
                }

                // Legal & Support
                item {
                    Text(
                        text = "LEGAL & POLICIES",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            SettingNavRow(
                                title = "Privacy Policy",
                                icon = Icons.Default.Policy,
                                onClick = onOpenPrivacyPolicy
                            )
                            SettingNavRow(
                                title = "Terms & Conditions",
                                icon = Icons.Default.Description,
                                onClick = onOpenTerms
                            )
                            SettingNavRow(
                                title = "Help & Support",
                                icon = Icons.AutoMirrored.Filled.HelpOutline,
                                onClick = { showHelpDialog = true }
                            )
                            SettingNavRow(
                                title = "About PuzzleVerse",
                                icon = Icons.Default.Info,
                                onClick = { showAboutDialog = true }
                            )
                        }
                    }
                }

                // Debug / Demo Control
                item {
                    Text(
                        text = "DEBUG / TEST TOOLS",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = Color(0x40EF4444),
                        backgroundColor = Color(0xFF16111C)
                    ) {
                        Column {
                            Text(
                                text = "Reset Local Game Progress",
                                color = NeonCoral,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Restores all levels to level 1, clears test wallet ledger and reset achievements. For testing only.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { showResetConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCoral),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset All Progress", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Banner Ad
        BannerAdCard(modifier = Modifier.align(Alignment.BottomCenter))

        // About Dialog
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text(text = Constants.APP_NAME, color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Version 1.0.0 (Production Release Candidate)", color = ElectricBlue, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Developer: ${Constants.DEVELOPER_NAME}\nSupport: ${Constants.SUPPORT_EMAIL}\n\nA high-performance 3D animated puzzle game featuring Arrow Flow, Block Merge, Heart Maze, and Tile Match with verified completion logic and test reward ledger.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text("Close", color = ElectricBlue)
                    }
                },
                containerColor = Color(0xFF141C2E)
            )
        }

        // Help Dialog
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = { Text(text = "Help & Support", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "Frequently Asked Questions:\n\n" +
                                    "Q: How do I earn rewards?\n" +
                                    "A: Complete levels genuinely in any of the 4 puzzle games. Replaying completed levels does not duplicate rewards.\n\n" +
                                    "Q: When can I withdraw?\n" +
                                    "A: Minimum test withdrawal threshold is ₹50.\n\n" +
                                    "Need assistance? Contact:\n${Constants.SUPPORT_EMAIL}",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHelpDialog = false }) {
                        Text("Got it", color = ElectricBlue)
                    }
                },
                containerColor = Color(0xFF141C2E)
            )
        }

        // Reset Confirmation Dialog
        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                title = { Text("Reset Progress?", color = NeonCoral, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to reset all game levels, wallet ledger, and achievements? This action cannot be undone.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetDemoProgress()
                            showResetConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCoral)
                    ) {
                        Text("Confirm Reset")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = Color(0xFF141C2E)
            )
        }
    }
}

@Composable
private fun SettingToggleItem(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ElectricBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ElectricBlue,
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}

@Composable
private fun SettingNavRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonPurple,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
