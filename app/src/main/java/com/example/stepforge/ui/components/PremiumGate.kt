package com.example.stepforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PremiumGate(
    premiumEnabled: Boolean,
    title: String,
    subtitle: String,
    onUnlockClick: () -> Unit,
    content: @Composable () -> Unit
) {

    val cs = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight() // Sadece içeriği kadar yer kaplasın
    ) {

        // CONTENT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (!premiumEnabled) Modifier.height(260.dp).blur(16.dp)
                    else Modifier.wrapContentHeight()
                )
        ) {
            content()
        }

        // OVERLAY
        if (!premiumEnabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                cs.background.copy(alpha = 0.85f),
                                cs.background.copy(alpha = 0.98f)
                            )
                        )
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onUnlockClick() },
                contentAlignment = Alignment.Center
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = Color(0xFF00FFA3),
                        modifier = Modifier.size(26.dp)
                    )

                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = cs.onBackground
                    )

                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = cs.onBackground.copy(alpha = 0.75f),
                        lineHeight = 18.sp
                    )

                    Spacer(Modifier.height(6.dp))

                    Button(
                        onClick = onUnlockClick,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00FFA3),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text(
                            text = "Unlock Premium",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Text(
                        text = "Tap anywhere to unlock",
                        fontSize = 11.sp,
                        color = cs.onBackground.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}