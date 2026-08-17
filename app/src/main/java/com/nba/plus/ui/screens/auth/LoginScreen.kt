package com.nba.plus.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.nba.plus.BuildConfig
import com.nba.plus.R
import com.nba.plus.domain.repository.AuthRepository
import com.nba.plus.ui.components.AppTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isSignUp: Boolean = false,
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val googleLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun setEmail(email: String) = _state.update { it.copy(email = email, errorMessage = null) }

    fun setPassword(password: String) = _state.update { it.copy(password = password, errorMessage = null) }

    fun toggleMode() = _state.update { it.copy(isSignUp = !it.isSignUp, errorMessage = null) }

    val emailValid: Boolean
        get() = android.util.Patterns.EMAIL_ADDRESS.matcher(_state.value.email.trim()).matches()

    val passwordValid: Boolean
        get() = _state.value.password.length >= 6

    fun submit(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.loading || s.googleLoading) return
        _state.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = runCatching {
                if (s.isSignUp) {
                    authRepository.signUpWithEmail(s.email.trim(), s.password)
                } else {
                    authRepository.signInWithEmail(s.email.trim(), s.password)
                }
            }
            result.onSuccess {
                _state.update { it.copy(loading = false) }
                onSuccess()
            }.onFailure { error ->
                _state.update { st ->
                    st.copy(
                        loading = false,
                        errorMessage = error.localizedMessage ?: "auth_error"
                    )
                }
            }
        }
    }

    fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>, onSuccess: () -> Unit) {
        _state.update { it.copy(googleLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                val email = account.email
                val displayName = account.displayName
                val photoUrl = account.photoUrl?.toString()

                authRepository.signInWithGoogle(
                    idToken = idToken,
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl,
                )
                _state.update { it.copy(googleLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        googleLoading = false,
                        errorMessage = e.localizedMessage ?: "google_auth_error"
                    )
                }
            }
        }
    }
}

/**
 * شاشة تسجيل الدخول / إنشاء حساب عبر البريد الإلكتروني أو المتابعة عبر Google.
 */
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    val googleSignInClient: GoogleSignInClient = remember {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.ifBlank {
            context.getString(R.string.default_web_client_id)
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .requestProfile()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            viewModel.handleGoogleSignInResult(task, onSuccess = { navController.popBackStack() })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        AppTopBar(
            title = stringResource(
                if (state.isSignUp) R.string.signup_title else R.string.login_title
            ),
            showBack = true,
            onBack = { navController.popBackStack() },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // حقل البريد الإلكتروني
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::setEmail,
                label = { Text(stringResource(R.string.email)) },
                placeholder = { Text("example@domain.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                singleLine = true,
                isError = state.email.isNotBlank() && !viewModel.emailValid,
                supportingText = {
                    if (state.email.isNotBlank() && !viewModel.emailValid) {
                        Text(stringResource(R.string.invalid_email))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            // حقل كلمة المرور
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::setPassword,
                label = { Text(stringResource(R.string.password)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = state.password.isNotBlank() && !viewModel.passwordValid,
                supportingText = {
                    if (state.password.isNotBlank() && !viewModel.passwordValid) {
                        Text(stringResource(R.string.invalid_password))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (viewModel.emailValid && viewModel.passwordValid) {
                            viewModel.submit { navController.popBackStack() }
                        }
                    }
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            // رسالة الخطأ إن وجدت
            if (state.errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (state.errorMessage == "auth_error") {
                            stringResource(R.string.auth_error)
                        } else if (state.errorMessage == "google_auth_error") {
                            stringResource(R.string.google_auth_error)
                        } else {
                            state.errorMessage ?: stringResource(R.string.auth_error)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // زر الدخول / التسجيل بالبريد
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.submit { navController.popBackStack() }
                },
                enabled = viewModel.emailValid && viewModel.passwordValid && !state.loading && !state.googleLoading,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(
                            if (state.isSignUp) R.string.signup_button else R.string.login_button
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // الفاصل (أو / OR)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Text(
                    text = stringResource(R.string.or_divider),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }

            // زر المتابعة عبر Google
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    // إعادة تسجيل الخروج من الكاش المحلي لـ Google لتسهيل اختيار الحساب
                    googleSignInClient.signOut().addOnCompleteListener {
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                },
                enabled = !state.loading && !state.googleLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.googleLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        GoogleLogoIcon(modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.continue_with_google),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // التبديل بين تسجيل الدخول وإنشاء الحساب
            TextButton(onClick = viewModel::toggleMode) {
                Text(
                    text = stringResource(
                        if (state.isSignUp) R.string.switch_to_login else R.string.switch_to_signup
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * أيقونة شعار Google الرسمية بالألوان الأربعة.
 */
@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height / 2f
        val radius = width / 2f

        // Blue, Red, Yellow, Green Google Colors
        val blue = Color(0xFF4285F4)
        val red = Color(0xFFEA4335)
        val yellow = Color(0xFFFBBC05)
        val green = Color(0xFF34A853)

        // Draw Blue bar & arc
        drawArc(
            color = blue,
            startAngle = 0f,
            sweepAngle = 45f,
            useCenter = true,
            size = size,
        )
        drawArc(
            color = green,
            startAngle = 45f,
            sweepAngle = 135f,
            useCenter = true,
            size = size,
        )
        drawArc(
            color = yellow,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = true,
            size = size,
        )
        drawArc(
            color = red,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = true,
            size = size,
        )

        // Center cutout
        drawCircle(
            color = Color(0xFF131420),
            radius = radius * 0.58f,
            center = Offset(cx, cy),
        )

        // Middle blue horizontal bar
        drawRect(
            color = blue,
            topLeft = Offset(cx, cy - (height * 0.13f)),
            size = androidx.compose.ui.geometry.Size(width * 0.5f, height * 0.26f),
        )
    }
}
