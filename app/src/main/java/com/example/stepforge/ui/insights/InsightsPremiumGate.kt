package com.example.stepforge.ui.insights

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
            .clip(RoundedCornerShape(24.dp))
    ) {

        // MAIN CONTENT (blurred)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (!premiumEnabled) Modifier.blur(22.dp) else Modifier)
        ) {
            content()
        }

        // PREMIUM LOCK OVERLAY
        if (!premiumEnabled) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize()
                    // this blocks ALL clicks from going through
                    .pointerInput(Unit) {}
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.70f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onUnlockClick()
                    }
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = Color(0xFF00FFA3),
                        modifier = Modifier.size(22.dp)
                    )

                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )

                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.72f),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = onUnlockClick,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00FFA3),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Unlock Premium",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        text = "Tap anywhere to unlock",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}
