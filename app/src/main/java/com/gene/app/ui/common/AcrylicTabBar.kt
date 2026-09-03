package com.gene.app.ui.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gene.app.ui.theme.IosBlue
import com.gene.app.ui.theme.IosDarkBlue

@Composable
fun AcrylicTabBar(
    selectedTab: Int, // 0 = Messages, 1 = Contacts
    onTabSelected: (Int) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    // Acrylic glassmorphism color palette
    val acrylicBg = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xD02C2C2E), Color(0xBF1C1C1E)))
    } else {
        Brush.verticalGradient(listOf(Color(0xF0FFFFFF), Color(0xD8F2F2F7)))
    }

    val specularBorder = if (isDark) {
        Color.White.copy(alpha = 0.22f)
    } else {
        Color.White.copy(alpha = 0.75f)
    }

    val activePillBg = if (isDark) {
        Color.White.copy(alpha = 0.25f)
    } else {
        Color.White
    }

    val activeColor = if (isDark) IosDarkBlue else IosBlue
    val inactiveColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF8E8E93)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Centered, Shrunk Floating Acrylic Pill Container
        Box(
            modifier = Modifier
                .shadow(
                    elevation = if (isDark) 12.dp else 8.dp,
                    shape = CircleShape,
                    ambientColor = if (isDark) Color.Black else Color(0x30000000),
                    spotColor = if (isDark) Color.Black else Color(0x30000000)
                )
                .clip(CircleShape)
                .background(acrylicBg)
                .border(0.75.dp, specularBorder, CircleShape)
                .width(220.dp)
                .height(50.dp)
                .padding(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 0: Messages
                AcrylicTabItem(
                    label = "Messages",
                    isSelected = selectedTab == 0,
                    activeIcon = { Icon(Icons.Filled.ChatBubble, contentDescription = null, tint = activeColor, modifier = Modifier.size(17.dp)) },
                    inactiveIcon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = inactiveColor, modifier = Modifier.size(17.dp)) },
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    activeBg = activePillBg,
                    modifier = Modifier.weight(1f),
                    onClick = { onTabSelected(0) }
                )

                // Tab 1: Contacts
                AcrylicTabItem(
                    label = "Contacts",
                    isSelected = selectedTab == 1,
                    activeIcon = { Icon(Icons.Filled.Contacts, contentDescription = null, tint = activeColor, modifier = Modifier.size(17.dp)) },
                    inactiveIcon = { Icon(Icons.Outlined.Contacts, contentDescription = null, tint = inactiveColor, modifier = Modifier.size(17.dp)) },
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    activeBg = activePillBg,
                    modifier = Modifier.weight(1f),
                    onClick = { onTabSelected(1) }
                )
            }
        }
    }
}

@Composable
private fun AcrylicTabItem(
    label: String,
    isSelected: Boolean,
    activeIcon: @Composable () -> Unit,
    inactiveIcon: @Composable () -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    activeBg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .then(
                if (isSelected) Modifier
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .background(activeBg)
                else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) activeIcon() else inactiveIcon()
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isSelected) activeColor else inactiveColor,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
