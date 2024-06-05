package com.example.sleepwellapp.ui.app

import android.content.Context
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sleepwellapp.R
import com.example.sleepwellapp.datalayer.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: MainViewModel = viewModel()) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val loginUiState = viewModel.loginUiState
    val isError = loginUiState.loginError != null
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        if (!loginUiState.wantToRegister) {
            LogInForUser(viewModel, isError, scope, context)
        } else {
            SignUp(viewModel, isError, scope, context)
        }

        if (loginUiState.isLoading) {
            CircularProgressIndicator()
        }

        LaunchedEffect(key1 = viewModel.hasUser) {
            if (viewModel.hasUser) {
                viewModel.saveCredentials()
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        SheepJumpingAnimation()

    }
}

@Composable
fun LogInForUser(
    viewModel: MainViewModel,
    isError: Boolean,
    scope: CoroutineScope,
    context: Context
) {
    val loginUiState = viewModel.loginUiState
    Text(text = "Login")

    if (isError) {
        Text(
            text = loginUiState.loginError ?: "unknown error",
            color = Color.Red,
        )
    }

    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        value = loginUiState.userName ?: "",
        onValueChange = { viewModel.onUserNameChange(it) },
        label = {
            Text(text = "Email")
        },
        isError = isError
    )
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        value = loginUiState.password ?: "",
        onValueChange = { viewModel.onPasswordNameChange(it) },
        label = {
            Text(text = "Password")
        },
        visualTransformation = PasswordVisualTransformation(),
        isError = isError
    )

    Button(onClick = { viewModel.loginUser(context) }) {
        Text(text = "Sign In")
    }
    Spacer(modifier = Modifier.size(16.dp))

    Button(onClick = { viewModel.toggleWantToRegister() }) {
        Text(text = "Don't have an Account?")
    }
}

@Composable
fun SignUp(
    viewModel: MainViewModel,
    isError: Boolean,
    scope: CoroutineScope,
    context: Context
) {
    val loginUiState = viewModel.loginUiState

    Text(text = "Register")

    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        value = loginUiState.userNameSignUp ?: "",
        onValueChange = { viewModel.onUserNameChangeSignup(it) },
        label = {
            Text(text = "Email")
        },
        isError = isError
    )
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        value = loginUiState.passwordSignUp ?: "",
        onValueChange = { viewModel.onPasswordChangeSignup(it) },
        label = {
            Text(text = "Password")
        },
        visualTransformation = PasswordVisualTransformation(),
        isError = isError
    )
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        value = loginUiState.confirmPasswordSignUp ?: "",
        onValueChange = { viewModel.onConfirmPasswordChange(it) },
        label = {
            Text(text = "Confirm Password")
        },
        visualTransformation = PasswordVisualTransformation(),
        isError = isError
    )

    Button(onClick = {
        scope.launch {
            viewModel.createUser(context)
        }
    }) {
        Text(text = "Sign Up")
    }

    Button(onClick = { viewModel.toggleWantToRegister() }) {
        Text(text = "Already have an account?")
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