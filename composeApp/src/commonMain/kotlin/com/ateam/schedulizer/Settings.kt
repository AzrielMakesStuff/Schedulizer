package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Settings() {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.2f)
                    .padding(15.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterHorizontally),
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                    {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color.White.copy(alpha = 0.5f), shape = CircleShape)
                                .border(1.dp, Color.Black, shape = CircleShape)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        )
                        {
                            Text(text = "A", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color.White, shape = CircleShape)
                                .border(1.dp, Color.Black, shape = CircleShape)
                                .clip(CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color.Black, shape = CircleShape)
                                .border(1.dp, Color.Black, shape = CircleShape)
                                .clip(CircleShape)
                        )
                    }
                }

            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.3f)
                    .padding(15.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.DarkGray),
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