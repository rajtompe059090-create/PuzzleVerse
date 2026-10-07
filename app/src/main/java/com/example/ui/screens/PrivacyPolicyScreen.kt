package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.Constants
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBgPrimary
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
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
                    text = "PRIVACY POLICY",
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
            ) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "Last Updated: October 2026",
                                color = ElectricBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Welcome to ${Constants.APP_NAME}, operated by ${Constants.DEVELOPER_NAME}. This Privacy Policy explains our practices regarding data privacy, game progress storage, and third-party advertising partners.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                item {
                    PolicySection(
                        number = "1",
                        title = "Information We Collect",
                        content = "We collect limited technical and gameplay data necessary to maintain level progress, high scores, and user preferences. In local mode, game progress and wallet transactions are stored securely on your device using local SQLite database (Room). No personal identification records (e.g. passwords, biometric data) are accessed."
                    )
                }

                item {
                    PolicySection(
                        number = "2",
                        title = "How Information Is Used",
                        content = "Collected data is used strictly for:\n• Tracking level completion across Arrow Flow, Block Merge, Heart Maze, and Tile Match.\n• Recording reward ledger entries for first-time level clears.\n• Saving user sound, music, and vibration preferences."
                    )
                }

                item {
                    PolicySection(
                        number = "3",
                        title = "Advertising",
                        content = "We serve contextual ads via Google Mobile Ads SDK (AdMob) to support continuous game development. Ads comply with Google Play Families and Advertising Program policies."
                    )
                }

                item {
                    PolicySection(
                        number = "4",
                        title = "Google AdMob",
                        content = "We integrate Google AdMob to deliver banner ads, interstitial transitions, and opt-in rewarded video ads. Google AdMob may process device identifiers and ad interaction signals pursuant to Google's Privacy Policy at https://policies.google.com/privacy."
                    )
                }

                item {
                    PolicySection(
                        number = "5",
                        title = "Game Progress",
                        content = "All level unlocking, move counters, and star ratings are validated through local deterministic game logic. Progress remains preserved on device even across app restarts."
                    )
                }

                item {
                    PolicySection(
                        number = "6",
                        title = "Rewards and Wallet",
                        content = "Rewards (₹1 per unique level cleared, ₹10 joining bonus) are tracked in an internal transaction ledger. Rewards are non-transferable virtual credits inside this application. Anti-cheat rules prevent duplicate payouts on replay."
                    )
                }

                item {
                    PolicySection(
                        number = "7",
                        title = "Withdrawal Information",
                        content = "In the current release, all withdrawal flows operate under simulated TEST/DEMO mode. No real fiat money is debited or credited. Real payment gateway integrations will require explicit user KYC and compliant merchant agreements in accordance with regional regulations."
                    )
                }

                item {
                    PolicySection(
                        number = "8",
                        title = "Data Storage",
                        content = "Game data is stored on-device in Android app-private internal storage using Room SQLite and DataStore. Uninstalling the app or clearing application storage will reset local data."
                    )
                }

                item {
                    PolicySection(
                        number = "9",
                        title = "Third-Party Services",
                        content = "This app relies on Google Play Services and Google Mobile Ads. We do not sell, rent, or lease personal telemetry data to unauthorized brokers."
                    )
                }

                item {
                    PolicySection(
                        number = "10",
                        title = "Children's Privacy",
                        content = "PuzzleVerse does not knowingly collect personal identifiable information from children under the age of 13. Our gameplay is suitable for general audiences."
                    )
                }

                item {
                    PolicySection(
                        number = "11",
                        title = "Security",
                        content = "We implement industry standard encryption and tamper-evident local data storage practices. Sensitive network calls employ TLS 1.3 encryption."
                    )
                }

                item {
                    PolicySection(
                        number = "12",
                        title = "Changes to Privacy Policy",
                        content = "We reserve the right to revise this policy periodically. Revisions will be published inside the application settings and on the public privacy policy web page."
                    )
                }

                item {
                    PolicySection(
                        number = "13",
                        title = "Contact Information",
                        content = "For questions, concerns, or data deletion requests, contact us at:\n\nDeveloper: ${Constants.DEVELOPER_NAME}\nEmail: ${Constants.SUPPORT_EMAIL}\nWebsite: ${Constants.PRIVACY_POLICY_URL}"
                    )
                }
            }
        }
    }
}

@Composable
private fun PolicySection(
    number: String,
    title: String,
    content: String
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        backgroundColor = Color(0xFF111726)
    ) {
        Column {
            Text(
                text = "$number. $title",
                color = ElectricBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
