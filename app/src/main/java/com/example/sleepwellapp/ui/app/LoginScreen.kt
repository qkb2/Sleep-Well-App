package com.example.sleepwellapp.ui.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sleepwellapp.R
import com.example.sleepwellapp.datalayer.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: MainViewModel = viewModel()) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(value = username, onValueChange = { username = it }, label = { Text("Username") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            scope.launch {
                viewModel.saveCredentials(username, password)
            }
        }) {
            Text("Login")
        }
        Spacer(modifier = Modifier.height(32.dp))
        SheepJumpingAnimation()
    }
}

@Composable
fun SheepJumpingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "transition")
    val sheepJump by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "sheep jumps"
    )

    val bitmap = ImageBitmap.imageResource(R.drawable.sheep)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f), // Adjust aspect ratio to match the design
        contentAlignment = Alignment.BottomCenter
    ) {
        val sheepHeight = 0.3f // 30% of the Box height
        val jumpHeight = 0.4f // 40% of the Box height

        Image(
            painter = painterResource(id = R.drawable.fence),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxWidth(0.2F) // 20% of the Box height
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val sheepWidth = bitmap.width.toFloat()

            val sheepX = width * sheepJump - sheepWidth / 2
            val normalizedJump = if (sheepJump < 0.5f) {
                sheepJump * 2
            } else {
                (1 - sheepJump) * 2
            }
            val sheepY = height * (1 - sheepHeight - (normalizedJump * jumpHeight))

            translate(left = sheepX, top = sheepY) {
                drawImage(bitmap)
            }
        }
    }
}