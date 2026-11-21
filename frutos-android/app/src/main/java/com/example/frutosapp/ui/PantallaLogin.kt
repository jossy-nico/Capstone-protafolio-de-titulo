package com.example.frutosapp.ui

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.frutosapp.LoginState
import com.example.frutosapp.LoginViewModel
import com.example.frutosapp.model.Usuario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLogin(
    vm: LoginViewModel = viewModel(),
    onLoginSuccess: (usuario: Usuario) -> Unit,
    onNavigateToRegistro: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginState by vm.loginState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ANIMACIÓN "PULSE" DEL BOTÓN INGRESAR (distinta al botón Agregar)
    val infiniteTransition = rememberInfiniteTransition(label = "login_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "login_pulse_scale"
    )

    LaunchedEffect(loginState) {
        when (val estado = loginState) {
            is LoginState.Success -> {
                onLoginSuccess(estado.usuario)
                vm.resetLoginState()
            }
            is LoginState.Error -> {
                Toast.makeText(context, estado.mensaje, Toast.LENGTH_SHORT).show()
                vm.resetLoginState()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Iniciar Sesión") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Correo Electrónico") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                isError = loginState is LoginState.Error
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                isError = loginState is LoginState.Error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { vm.iniciarSesion(correo, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    // si está cargando, no pulsea; si no, aplica la animación
                    .scale(if (loginState is LoginState.Loading) 1f else pulseScale),
                enabled = loginState !is LoginState.Loading,
            ) {
                if (loginState is LoginState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Ingresar")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "¿No tienes cuenta? Regístrate",
                modifier = Modifier.clickable { onNavigateToRegistro() },
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}