package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

var currentThemeMode by mutableStateOf(ThemeMode.SYSTEM)

@Composable
fun Settings() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.25f)
                    .padding(15.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally)
                {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    )
                    {
                        ThemeButton(
                            selected = currentThemeMode == ThemeMode.SYSTEM,
                             onClick = { currentThemeMode = ThemeMode.SYSTEM },
                             color = MaterialTheme.colorScheme.outline,
                             label = "A"
                        )
                        ThemeButton(
                            selected = currentThemeMode == ThemeMode.LIGHT,
                            onClick = { currentThemeMode = ThemeMode.LIGHT },
                            color = Color.White,
                            border = Color.LightGray
                        )
                        ThemeButton(
                            selected = currentThemeMode == ThemeMode.DARK,
                            onClick = { currentThemeMode = ThemeMode.DARK },
                            color = Color.Black,
                            border = Color.Gray
                        )
                    }
                }

            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.3f)
                    .padding(15.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Put something else here, idk I just work here",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ThemeButton(
    selected: Boolean,
    onClick: () -> Unit,
    color: Color,
    border: Color = MaterialTheme.colorScheme.primary,
    label: String? = null
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 4.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else border,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (label != null) {
            Text(
                text = label,
                fontSize = 24.sp,
                color = if (color == Color.Black) Color.White else Color.Black,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}