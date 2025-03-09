package com.example.myapplication.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import kotlinx.coroutines.delay

/**
 * Applies the custom theme to the application.
 *
 * @param darkTheme Whether the dark theme is enabled.
 * @param content The content to be displayed within the theme.
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    LaunchedEffect(key1 = true) {
        delay(2000) // 5 seconds delay
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B5E20)), // Material Green
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Split the Bill",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,

            )
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = painterResource(id = R.drawable.money_logo),
                contentDescription = "Money Logo",
                modifier = Modifier.size(100.dp)
            )
        }
    }
}