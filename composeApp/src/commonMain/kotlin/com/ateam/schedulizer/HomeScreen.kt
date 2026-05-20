package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
    )
    {
        Column(
             modifier = Modifier
                 .fillMaxSize()
        )
        {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.25f)
                    .padding(15.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            )
            {
                Text(text = "Part 1", fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier
                    .fillMaxHeight(0.25f)
                    .fillMaxWidth()
            )
            {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .fillMaxHeight()
                        .padding(15.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                )
                {
                    Text(text = "Part 2", fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(15.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                )
                {
                    Text(text = "Part 3", fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .padding(15.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            )
            {
                Text(text = "Part 4", fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .padding(15.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            )
            {
                Text(text = "Part 5", fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(15.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            )
            {
                Text(text = "Part 6", fontWeight = FontWeight.Bold)
            }
        }


    }
}
