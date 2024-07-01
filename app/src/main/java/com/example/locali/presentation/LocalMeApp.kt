package com.example.locali.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun LocalMeApp(
    isTrainingMode: MutableState<Boolean>,
    onModeChange: (Boolean) -> Unit,
    lastUpdateTime: String,
    gpsStatus: MutableState<Boolean>,
    internetStatus: MutableState<Boolean>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "GPS",
                color = if (gpsStatus.value) Color.Green else Color.Red,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                text = "int",
                color = if (internetStatus.value) Color.Green else Color.Red,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        Text(
            text = "Last update: $lastUpdateTime",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
        )
        Switch(
            checked = isTrainingMode.value,
            onCheckedChange = { onModeChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Green,
                uncheckedThumbColor = Color.Red
            ),
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Text(
            text = if (isTrainingMode.value) "Training Mode" else "Normal Mode",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLocalMeApp() {
    val isTrainingMode = remember { mutableStateOf(false) }
    val gpsStatus = remember { mutableStateOf(true) }
    val internetStatus = remember { mutableStateOf(true) }
    LocalMeApp(
        isTrainingMode = isTrainingMode,
        onModeChange = {},
        lastUpdateTime = "N/A",
        gpsStatus = gpsStatus,
        internetStatus = internetStatus
    )
}
