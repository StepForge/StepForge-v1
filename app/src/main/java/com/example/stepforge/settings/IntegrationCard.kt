package com.example.stepforge.settings

import com.example.stepforge.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun IntegrationCard(
    logoRes: Int,
    title: String,
    description: String,
    onClick: () -> Unit,
    darkTheme: Boolean? = null,
    modifier: Modifier = Modifier
) {
    val isDark = darkTheme ?: isSystemInDarkTheme()

    // ✅ Light palet (soft)
    val gradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF101114), Color(0xFF181A1E)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF0F3F7)))
    }

    val titleColor = if (isDark) Color(0xFFF7F7F7) else Color(0xFF1A202C)
    val descColor = if (isDark) Color(0xFFB9BEC6) else Color(0xFF5B6472)

    val shadow = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.08f)

    // ✅ Light’ta kart “koyu ada” olmasın diye containerColor transparent, içte gradient
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = shadow, spotColor = shadow)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .background(gradient)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = logoRes),
                    contentDescription = stringResource(R.string.hc_logo_format, title),
                    modifier = Modifier.fillMaxSize(0.9f),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp,
                    color = descColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
