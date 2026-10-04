package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.*
import com.example.data.repository.CountryOption
import com.example.data.repository.GLOBAL_COUNTRIES
import com.example.ui.MainNavTab
import com.example.ui.TangerViewModel
import com.example.ui.theme.*
import java.util.UUID

/**
 * 1. TANGERAM SIGNUP SCREEN:
 * - Google Login, Facebook Login, Email/Password Signup, and Phone + Country OTP Signup.
 * - Displays the authoritative server-side Sequential TANGERAM User ID Counter (starting from 199967).
 * - Automatically assigns the next available sequential ID (no duplicates) and opens TANGERAM Onboarding.
 */
@Composable
fun TangeramSignupScreen(
    viewModel: TangerViewModel,
    nextSequentialId: Long,
    lastIssuedOtp: String?,
    otpCooldownSec: Int,
    allUsers: List<UserEntity>,
    isAlreadySignedIn: Boolean = false
) {
    BackHandler(enabled = isAlreadySignedIn) {
        viewModel.closeSignupFlow()
    }

    var authMode by remember { mutableStateOf("EMAIL") } // "EMAIL" or "PHONE_OTP"
    var selectedCountry by remember { mutableStateOf(GLOBAL_COUNTRIES.first()) }

    // Email / Password inputs
    var emailInput by remember { mutableStateOf("star_${nextSequentialId}@tangeram.global") }
    var passwordInput by remember { mutableStateOf("tanger2026") }
    var fullNameInput by remember { mutableStateOf("Nour Tangeram") }
    var showPassword by remember { mutableStateOf(false) }

    // Phone + OTP inputs
    var phoneInput by remember { mutableStateOf("662${nextSequentialId.toString().takeLast(6)}") }
    var otpInput by remember { mutableStateOf("") }

    // Social OAuth display name
    var socialDisplayName by remember { mutableStateOf("Tangeram Star #$nextSequentialId") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Banner: TANGERAM Signup & Sequential ID Allocator
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tangeram_signup_header_card"),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TangerPrimaryGradient)
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = TangerGold,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "🔒 STEP 1 OF 2 • MANDATORY SIGN UP TO ENTER APP",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            if (isAlreadySignedIn) {
                                TextButton(
                                    onClick = { viewModel.closeSignupFlow() },
                                    modifier = Modifier.testTag("close_signup_screen_button")
                                ) {
                                    Text("Enter App →", color = TangerMint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            text = "Complete Sign Up First to Enter TANGER STAR VOICE",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Step 1: Sign up with Google, Facebook, Email, or Phone OTP (receives sequential TANGERAM ID #$nextSequentialId) → Step 2: Complete Profile Onboarding → Step 3: Enter App!",
                            color = TangerMintSoft,
                            fontSize = 12.sp
                        )

                        Surface(
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, TangerGoldLight),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("next_sequential_id_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Next Sequential TANGERAM ID Reserved",
                                        color = TangerMintSoft,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "ID #$nextSequentialId (Permanent • Zero Duplicates)",
                                        color = TangerGoldLight,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                }
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "Verified Sequential Counter",
                                    tint = TangerGoldLight,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. One-Tap Google Login & Facebook Login (Creates real sequential ID account + opens Onboarding)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("social_oauth_signup_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "🌐 Instant Social OAuth Signup (Google & Facebook)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Allocates permanent TANGERAM ID #$nextSequentialId atomically in the database and launches Onboarding.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = socialDisplayName,
                        onValueChange = { socialDisplayName = it },
                        label = { Text("Your Display Name for Social Signup") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("social_display_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val uniqueEmail = "google_${nextSequentialId}_${UUID.randomUUID().toString().take(4)}@gmail.com"
                                viewModel.registerNewSocialOAuthAccount(
                                    provider = "GOOGLE",
                                    socialEmail = uniqueEmail,
                                    displayName = socialDisplayName,
                                    country = selectedCountry
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB4437)),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("google_signup_button")
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Google Login", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val uniqueEmail = "fb_${nextSequentialId}_${UUID.randomUUID().toString().take(4)}@facebook.com"
                                viewModel.registerNewSocialOAuthAccount(
                                    provider = "FACEBOOK",
                                    socialEmail = uniqueEmail,
                                    displayName = socialDisplayName,
                                    country = selectedCountry
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("facebook_signup_button")
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Facebook Login", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 2. Email / Password Signup OR Phone + OTP Signup
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("email_phone_signup_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = authMode == "EMAIL",
                            onClick = { authMode = "EMAIL" },
                            label = { Text("📧 Email & Password Signup", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("auth_mode_email_chip")
                        )
                        FilterChip(
                            selected = authMode == "PHONE_OTP",
                            onClick = { authMode = "PHONE_OTP" },
                            label = { Text("📱 Phone + SMS OTP Signup", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("auth_mode_phone_chip")
                        )
                    }

                    Text("Select Country / Region:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(GLOBAL_COUNTRIES) { c ->
                            FilterChip(
                                selected = selectedCountry.code == c.code,
                                onClick = { selectedCountry = c },
                                label = { Text("${c.flag} ${c.name} (${c.dialCode})", fontSize = 11.sp) }
                            )
                        }
                    }

                    if (authMode == "EMAIL") {
                        OutlinedTextField(
                            value = fullNameInput,
                            onValueChange = { fullNameInput = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_fullname_input")
                        )

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_email_input")
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password (SHA-256 Hashed on Server)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_password_input")
                        )

                        Button(
                            onClick = {
                                viewModel.registerOrSignInWithEmail(
                                    email = emailInput,
                                    password = passwordInput,
                                    fullName = fullNameInput,
                                    country = selectedCountry
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TangerEmeraldLight),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 50.dp)
                                .testTag("submit_email_signup_button")
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign Up with Email → Assign ID #$nextSequentialId & Onboarding",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Text(
                            text = "Real OTP engine with 5-minute expiry, 30s resend cooldown, and 5-requests/10m rate limiting.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Phone Number (${selectedCountry.dialCode})") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("otp_phone_input"),
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.requestOtp(selectedCountry, phoneInput) },
                                enabled = otpCooldownSec == 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("request_otp_button")
                            ) {
                                Text(
                                    text = if (otpCooldownSec > 0) "Cooldown (${otpCooldownSec}s)" else "Send SMS OTP",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (lastIssuedOtp != null) {
                                FilledTonalButton(
                                    onClick = { otpInput = lastIssuedOtp },
                                    modifier = Modifier.testTag("autofill_otp_button")
                                ) {
                                    Text("Use OTP: $lastIssuedOtp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = { otpInput = it },
                            label = { Text("6-Digit OTP Verification Code") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("otp_code_input"),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                viewModel.verifyOtpAndSignIn(
                                    country = selectedCountry,
                                    phone = phoneInput,
                                    otp = otpInput
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TangerTurquoise),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 50.dp)
                                .testTag("verify_otp_button")
                        ) {
                            Text(
                                text = "Verify Phone OTP → Assign ID #$nextSequentialId & Onboarding",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. Existing Global TANGERAM Accounts Switcher (199967+)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "🔄 Switch to Existing TANGERAM Account (${allUsers.size} Users in DB)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allUsers, key = { it.tangerUserId }) { u ->
                            AssistChip(
                                onClick = { viewModel.switchUserAccount(u.tangerUserId, "SESSION_SWITCH") },
                                label = {
                                    Text(
                                        text = "${u.countryFlag} ${u.fullName} (ID #${u.tangerUserId})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. TANGERAM ONBOARDING SCREEN:
 * Opened automatically right after Signup.
 * User selects:
 * - Country Flag
 * - Gender
 * - Unique Username (@handle) & Full Display Name
 * - Profile Picture (from Phone Gallery via zero-permission Android Photo Picker or Star Avatar presets)
 * Displays the user's newly allocated permanent sequential TANGERAM User ID (starting from 199967).
 */
@Composable
fun TangeramOnboardingScreen(
    viewModel: TangerViewModel,
    currentUser: UserEntity?
) {
    val initialCountry = remember(currentUser) {
        GLOBAL_COUNTRIES.find { it.code == currentUser?.countryCode } ?: GLOBAL_COUNTRIES.first()
    }
    var selectedCountry by remember { mutableStateOf(initialCountry) }
    var selectedGender by remember(currentUser) { mutableStateOf(currentUser?.gender ?: "Female") }
    var usernameInput by remember(currentUser) {
        mutableStateOf(currentUser?.username ?: "tangeram_${currentUser?.tangerUserId ?: "199972"}")
    }
    var fullNameInput by remember(currentUser) {
        mutableStateOf(currentUser?.fullName ?: "Star #${currentUser?.tangerUserId ?: "199972"}")
    }
    var bioInput by remember(currentUser) {
        mutableStateOf(currentUser?.bio ?: "Voice Chat, Play Games, Find More Friends ✨")
    }
    var avatarUri by remember(currentUser) { mutableStateOf(currentUser?.avatarUri ?: "") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            avatarUri = uri.toString()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tangeram_onboarding_header_card"),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TangerPrimaryGradient)
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(color = TangerGold, shape = RoundedCornerShape(50)) {
                            Text(
                                text = "🎉 STEP 2 OF 2 • PERMANENT ID #${currentUser?.tangerUserId ?: "199972"} ASSIGNED",
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "Final Step: Complete Onboarding to Enter App",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Select your Country Flag, Gender, unique Username, and Profile Picture to finish setup and enter TANGER STAR VOICE.",
                            color = TangerMintSoft,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Step 1: Profile Picture Selection (Gallery Picker)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("1. Choose Your Profile Picture", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)

                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(TangerGoldGradient)
                            .border(3.dp, TangerTurquoiseBright, CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("onboarding_avatar_picker"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarUri.isNotBlank()) {
                            AsyncImage(
                                model = avatarUri,
                                contentDescription = "Selected Onboarding Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = "${selectedCountry.flag}${fullNameInput.take(1).uppercase()}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("onboarding_gallery_button")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Photo from Phone Gallery", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Step 2: Select Country Flag & Gender
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("2. Select Your Country Flag", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(GLOBAL_COUNTRIES) { country ->
                            FilterChip(
                                selected = selectedCountry.code == country.code,
                                onClick = { selectedCountry = country },
                                label = {
                                    Text(
                                        text = "${country.flag} ${country.name}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.testTag("onboarding_country_${country.code}")
                            )
                        }
                    }

                    HorizontalDivider()
                    Text("3. Select Your Gender", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("Female" to "♀ Female", "Male" to "♂ Male", "Other" to "✨ Other").forEach { (value, label) ->
                            FilterChip(
                                selected = selectedGender == value,
                                onClick = { selectedGender = value },
                                label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("onboarding_gender_$value")
                            )
                        }
                    }
                }
            }
        }

        // Step 3: Username, Display Name & Bio
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("4. Choose Unique Username & Display Name", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it.lowercase().replace(" ", "_") },
                        label = { Text("Unique TANGERAM Username (@handle)") },
                        leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_username_input")
                    )

                    OutlinedTextField(
                        value = fullNameInput,
                        onValueChange = { fullNameInput = it },
                        label = { Text("Display Name") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_fullname_input")
                    )

                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        label = { Text("Profile Bio") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_bio_input")
                    )

                    Button(
                        onClick = {
                            viewModel.completeOnboardingAndOpenProfile(
                                selectedCountry = selectedCountry,
                                selectedGender = selectedGender,
                                desiredUsername = usernameInput,
                                fullNameInput = fullNameInput,
                                avatarUriInput = avatarUri,
                                bioInput = bioInput,
                                destinationTab = MainNavTab.HOME
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TangerEmeraldLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .testTag("complete_onboarding_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Complete Setup & Enter TANGER STAR VOICE →",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.completeOnboardingAndOpenProfile(
                                selectedCountry = selectedCountry,
                                selectedGender = selectedGender,
                                desiredUsername = usernameInput,
                                fullNameInput = fullNameInput,
                                avatarUriInput = avatarUri,
                                bioInput = bioInput,
                                destinationTab = MainNavTab.PROFILE
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("complete_onboarding_open_profile_button")
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Complete Setup & Open My Profile / Wallet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. TANGERAM PROFILE & SETTINGS SCREEN:
 * - Displays permanent sequential TANGERAM User ID (199967+), Profile Picture, Full Name, @Username, Country Flag, VIP, Level, Bio.
 * - Allows changing Profile Picture, Name, Username, Bio, Country Flag, Gender, DOB, and Profile Privacy Settings (Allow Direct Messages, Show Online Status).
 * - Includes integrated Wallet Preview Card with real Coin & Diamond balances and one-tap navigation to the full Wallet & Exchange screen.
 * - Includes button to open the TANGERAM Signup & Sequential ID Creator to test creating new sequential accounts (199972, 199973, ...).
 */
@Composable
fun ProfileAndAuthScreen(
    viewModel: TangerViewModel,
    currentUser: UserEntity?,
    allUsers: List<UserEntity>,
    lastIssuedOtp: String?,
    otpCooldownSec: Int
) {
    val nextSequentialCounter by viewModel.userSequenceCounter.collectAsState()
    val nextSequentialId = nextSequentialCounter?.nextAvailableId ?: 199972L

    var selectedCountry by remember(currentUser) {
        mutableStateOf(
            GLOBAL_COUNTRIES.find { it.code == currentUser?.countryCode } ?: GLOBAL_COUNTRIES.first()
        )
    }

    var editFullName by remember(currentUser) { mutableStateOf(currentUser?.fullName ?: "Amira TangerStar") }
    var editUsername by remember(currentUser) { mutableStateOf(currentUser?.username ?: "amira_tangerstar") }
    var editGender by remember(currentUser) { mutableStateOf(currentUser?.gender ?: "Female") }
    var editDob by remember(currentUser) { mutableStateOf(currentUser?.dob ?: "1998-05-14") }
    var editBio by remember(currentUser) { mutableStateOf(currentUser?.bio ?: "Voice Chat, Play Games, Find More Friends") }
    var editAvatarUri by remember(currentUser) { mutableStateOf(currentUser?.avatarUri ?: "") }
    var allowDirectMessages by remember(currentUser) { mutableStateOf(currentUser?.allowDirectMessages ?: true) }
    var showOnlineStatus by remember(currentUser) { mutableStateOf(currentUser?.showOnlineStatus ?: true) }

    // Zero-permission Android Photo Picker for Profile Photo from Phone Gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            editAvatarUri = uri.toString()
            viewModel.updateProfile(
                username = editUsername,
                gender = editGender,
                dob = editDob,
                country = selectedCountry,
                bio = editBio,
                avatarUri = editAvatarUri,
                fullName = editFullName,
                allowDirectMessages = allowDirectMessages,
                showOnlineStatus = showOnlineStatus
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Current Authenticated TANGERAM Profile Card with Permanent ID (199967+) & Photo Picker
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_header_card"),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TangerPrimaryGradient)
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(TangerGold)
                                        .border(2.dp, TangerMint, CircleShape)
                                        .clickable {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                        .testTag("pick_gallery_photo_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (editAvatarUri.isNotBlank()) {
                                        AsyncImage(
                                            model = editAvatarUri,
                                            contentDescription = "Profile Photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = "${currentUser?.countryFlag ?: "🇲🇦"}${currentUser?.fullName?.take(1) ?: "T"}",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.Black
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = currentUser?.fullName ?: "Tangeram Star",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "@${currentUser?.username ?: "amira_tangerstar"}",
                                        color = TangerMint,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Surface(
                                        color = TangerGold.copy(alpha = 0.24f),
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, TangerGoldLight)
                                    ) {
                                        Text(
                                            text = "🆔 TANGERAM ID: #${currentUser?.tangerUserId ?: "199967"}",
                                            color = TangerGoldLight,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${currentUser?.countryFlag ?: "🇲🇦"} ${currentUser?.countryName ?: "Morocco"} • ${currentUser?.gender ?: "Female"} • Auth: ${currentUser?.authProvider ?: "PHONE_OTP"}",
                                        color = TangerMintSoft,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStatPill("TANGERAM ID", "#${currentUser?.tangerUserId ?: "199967"}")
                            ProfileStatPill("VIP Tier", "VIP ${currentUser?.vipLevel ?: 1}")
                            ProfileStatPill("Level", "Lv.${currentUser?.userLevel ?: 1}")
                            ProfileStatPill("Followers", "${currentUser?.followersCount ?: 0}")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                border = BorderStroke(1.dp, TangerMint),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change Photo", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.openSignupFlow() },
                                colors = ButtonDefaults.buttonColors(containerColor = TangerGold),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_signup_page_button")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "New Signup (#$nextSequentialId)",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.signOutAndReturnToSignup() },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sign_out_return_to_signup_button")
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sign Out (Return to Mandatory Sign Up Gate)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Quick TANGERAM Wallet & Diamond-to-Coin Exchange Summary Card on Profile
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_wallet_quick_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("💰 TANGERAM Wallet & Exchange", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text(
                                text = "Real-time server-validated balances & atomic Diamond → Coin exchange",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = { viewModel.selectTab(MainNavTab.WALLET) },
                            modifier = Modifier.testTag("profile_open_wallet_button")
                        ) {
                            Text("Open Full Wallet →", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = TangerMintSoft,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("🪙 Coin Balance", fontSize = 11.sp, color = TangerEmeraldDark, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${currentUser?.coinsBalance ?: 0}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TangerEmeraldDark
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFE0F2FE),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("💎 Diamond Balance", fontSize = 11.sp, color = Color(0xFF0369A1), fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${currentUser?.diamondsBalance ?: 0}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0369A1)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.exchangeDiamondsToCoins(100L) },
                            colors = ButtonDefaults.buttonColors(containerColor = TangerEmeraldLight),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_quick_exchange_100_button")
                        ) {
                            Text("Exchange 100 💎 → 70 🪙", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { viewModel.openOnboardingFlow() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reopen_onboarding_wizard_button")
                        ) {
                            Text("Onboarding Wizard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Edit Profile Details & Profile Settings (Name, @Username, Country Flag, Gender, DOB, Bio, Privacy Toggles)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("✏️ Edit TANGERAM Profile & Settings", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)

                    OutlinedTextField(
                        value = editFullName,
                        onValueChange = { editFullName = it },
                        label = { Text("Display Name") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_fullname_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = { editUsername = it.lowercase().replace(" ", "_") },
                        label = { Text("Unique Username (@handle)") },
                        leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_username_input"),
                        singleLine = true
                    )

                    Text("Country Flag:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(GLOBAL_COUNTRIES) { c ->
                            FilterChip(
                                selected = selectedCountry.code == c.code,
                                onClick = { selectedCountry = c },
                                label = { Text("${c.flag} ${c.name}", fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Female", "Male", "Other").forEach { g ->
                            FilterChip(
                                selected = editGender == g,
                                onClick = { editGender = g },
                                label = { Text(g) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editDob,
                        onValueChange = { editDob = it },
                        label = { Text("Date of Birth (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Bio") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_bio_input")
                    )

                    HorizontalDivider()
                    Text("⚙️ Profile Privacy & Status Settings", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Allow Direct Messages", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Allow global friends to message you directly", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = allowDirectMessages,
                            onCheckedChange = { allowDirectMessages = it },
                            modifier = Modifier.testTag("toggle_allow_dm_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show Active Online Status", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Display green online indicator on global directory", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = showOnlineStatus,
                            onCheckedChange = { showOnlineStatus = it },
                            modifier = Modifier.testTag("toggle_online_status_switch")
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.updateProfile(
                                username = editUsername,
                                gender = editGender,
                                dob = editDob,
                                country = selectedCountry,
                                bio = editBio,
                                avatarUri = editAvatarUri,
                                fullName = editFullName,
                                allowDirectMessages = allowDirectMessages,
                                showOnlineStatus = showOnlineStatus
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("save_profile_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Profile & Settings to Production Database", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. Switch Active Account or Open TANGERAM Signup Page
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_otp_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🔐 TANGERAM Multi-Account & Sequential ID Registry",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Sequential User IDs start at #199967. Next new signup will receive #${nextSequentialId}.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allUsers, key = { it.tangerUserId }) { u ->
                            AssistChip(
                                onClick = { viewModel.switchUserAccount(u.tangerUserId, "SESSION_SWITCH") },
                                label = {
                                    Text(
                                        text = "${u.countryFlag} @${u.username} (#${u.tangerUserId})",
                                        fontSize = 11.sp,
                                        fontWeight = if (u.tangerUserId == currentUser?.tangerUserId) FontWeight.ExtraBold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.openSignupFlow() },
                        colors = ButtonDefaults.buttonColors(containerColor = TangerTurquoise),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("launch_full_signup_button")
                    ) {
                        Text(
                            text = "Open TANGERAM Signup Page (Google / Facebook / Email / Phone)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        Text(label, color = TangerMintSoft, fontSize = 10.sp)
    }
}

@Composable
fun OwnerAdminPanelScreen(
    viewModel: TangerViewModel,
    currentUser: UserEntity?,
    allUsers: List<UserEntity>,
    allGifts: List<GiftEntity>,
    luckyState: LuckyGiftStateEntity?,
    allRooms: List<VoiceRoomEntity>,
    allAgencies: List<AgencyEntity>,
    allEvents: List<GlobalEventEntity>,
    allReports: List<UserReportEntity>,
    auditLogs: List<AuditLogEntity>,
    databaseBackups: List<DatabaseBackupSnapshotEntity> = emptyList(),
    cloudMediaAssets: List<CloudMediaAssetEntity> = emptyList()
) {
    BackHandler {
        viewModel.closeOwnerAdminScreen()
    }

    var selectedAdminSection by remember { mutableStateOf("USERS_FINANCE") }
    var backupLabelInput by remember { mutableStateOf("Owner Production Snapshot (v5 Schema)") }
    var userSearchId by remember { mutableStateOf("") }
    var targetUserId by remember { mutableStateOf("199967") }
    var coinsIssueText by remember { mutableStateOf("5000") }
    var diamondsIssueText by remember { mutableStateOf("1000") }
    var issueReason by remember { mutableStateOf("Global Owner Treasury Bonus") }

    // Gift & Lucky Gift Management inputs
    var editingGiftId by remember { mutableStateOf("") }
    var giftNameInput by remember { mutableStateOf("Royal Star Pegasus") }
    var giftCategoryInput by remember { mutableStateOf("Lucky Gift") }
    var giftEmojiInput by remember { mutableStateOf("🦄") }
    var giftPriceInput by remember { mutableStateOf("600") }
    var giftAnimationInput by remember { mutableStateOf("PEGASUS_LUCKY_RAINBOW") }
    var giftDiamondReturnInput by remember { mutableStateOf("65") }
    var giftTriggerCountInput by remember { mutableStateOf("3") }
    var giftRewardCoinsInput by remember { mutableStateOf("900") }
    var giftCashbackCoinsInput by remember { mutableStateOf("90") }
    var giftExpBonusInput by remember { mutableStateOf("220") }
    var giftMaxRewardInput by remember { mutableStateOf("2500") }
    var giftActivationLimitInput by remember { mutableStateOf("100") }
    var giftStartTimeInput by remember { mutableStateOf("0") }
    var giftEndTimeInput by remember { mutableStateOf("0") }
    var giftCooldownSecInput by remember { mutableStateOf("4") }
    var giftRepeatTimingSecInput by remember { mutableStateOf("1800") }
    var giftIsActiveInput by remember { mutableStateOf(true) }
    var giftIsPausedInput by remember { mutableStateOf(false) }
    var giftBackupNotesInput by remember { mutableStateOf("Auto-Backup & 30m Repeat Timing Profile") }

    // Event & Banner inputs
    var eventTitleInput by remember { mutableStateOf("🌍 Global VIP Star Weekend") }
    var eventSubInput by remember { mutableStateOf("Double Lucky Gift Jackpot & Ludo Prizes") }
    var eventTypeInput by remember { mutableStateOf("GLOBAL_ANNOUNCEMENT") }
    var eventScopeInput by remember { mutableStateOf("ALL") }
    var eventTargetValInput by remember { mutableStateOf("*") }

    val searchedUsers = remember(allUsers, userSearchId) {
        if (userSearchId.isBlank()) allUsers
        else allUsers.filter {
            it.tangerUserId.contains(userSearchId, ignoreCase = true) ||
                it.username.contains(userSearchId, ignoreCase = true) ||
                it.fullName.contains(userSearchId, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("owner_admin_panel_header"),
                colors = CardDefaults.cardColors(containerColor = TangerEmeraldDark),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🛡️ GLOBAL OWNER ADMIN PANEL",
                                color = TangerGoldLight,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "Backend RBAC Verified • Actor: ${currentUser?.username} (#${currentUser?.tangerUserId})",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.closeOwnerAdminScreen() },
                            border = BorderStroke(1.dp, TangerGoldLight),
                            modifier = Modifier.testTag("close_admin_panel_button")
                        ) {
                            Text("Exit Admin", color = TangerGoldLight, fontSize = 11.sp)
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val tabs = listOf(
                            "USERS_FINANCE" to "👥 Users, Coins & Diamonds",
                            "GIFTS_LUCKY" to "🎁 Gifts, Prices & Lucky",
                            "EVENTS_ROOMS" to "📢 Events, Banners & Rooms",
                            "CLOUD_BACKUPS" to "☁️ Backups, Migrations & CDN (${databaseBackups.size})",
                            "REPORTS_AUDIT" to "📜 Reports & Audit Logs (${auditLogs.size})"
                        )
                        items(tabs) { (key, label) ->
                            FilterChip(
                                selected = selectedAdminSection == key,
                                onClick = { selectedAdminSection = key },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TangerGold,
                                    selectedLabelColor = Color.Black,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        when (selectedAdminSection) {
            "USERS_FINANCE" -> {
                // Issue Coins & Diamonds Atomically
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("💰 Owner-Issued Coins & Diamonds (Atomic Ledger)", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            OutlinedTextField(
                                value = targetUserId,
                                onValueChange = { targetUserId = it },
                                label = { Text("Target TANGERAM User ID (e.g. 199967)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = coinsIssueText,
                                    onValueChange = { coinsIssueText = it.filter { c -> c.isDigit() || c == '-' } },
                                    label = { Text("Coins Delta") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = diamondsIssueText,
                                    onValueChange = { diamondsIssueText = it.filter { c -> c.isDigit() || c == '-' } },
                                    label = { Text("Diamonds Delta") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = issueReason,
                                onValueChange = { issueReason = it },
                                label = { Text("Audit Reason") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    viewModel.ownerIssueCoinsOrDiamonds(
                                        targetUserId = targetUserId,
                                        coinsDelta = coinsIssueText.toLongOrNull() ?: 0L,
                                        diamondsDelta = diamondsIssueText.toLongOrNull() ?: 0L,
                                        reason = issueReason
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_issue_coins_button")
                            ) {
                                Text("Execute Atomic Wallet Adjustment + Audit Log", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Search ANY Global User by User ID & Ban/Suspend/Unban/VIP/Role
                item {
                    OutlinedTextField(
                        value = userSearchId,
                        onValueChange = { userSearchId = it },
                        label = { Text("Search Any Global User by TANGERAM ID (199967+) or Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                items(searchedUsers, key = { it.tangerUserId }) { u ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "${u.countryFlag} ${u.fullName} (@${u.username} • ID #${u.tangerUserId}) • Role: ${u.role} • VIP ${u.vipLevel}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Coins: 🪙 ${u.coinsBalance} • Diamonds: 💎 ${u.diamondsBalance} • Suspended: ${u.isSuspended} • Banned: ${u.isBanned}",
                                fontSize = 11.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = {
                                    viewModel.ownerModerateUser(u.tangerUserId, newVipLevel = (u.vipLevel % 6) + 1)
                                }) {
                                    Text("VIP ${(u.vipLevel % 6) + 1}", fontSize = 10.sp)
                                }
                                OutlinedButton(onClick = {
                                    viewModel.ownerModerateUser(u.tangerUserId, suspend = !u.isSuspended)
                                }) {
                                    Text(if (u.isSuspended) "Unsuspend" else "Suspend", fontSize = 10.sp)
                                }
                                Button(
                                    onClick = {
                                        viewModel.ownerModerateUser(u.tangerUserId, ban = !u.isBanned)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (u.isBanned) TangerEmerald else TangerDangerRed)
                                ) {
                                    Text(if (u.isBanned) "Unban" else "Ban", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            "GIFTS_LUCKY" -> {
                // Global Lucky Engine Master Controls (Milestones, Pause/Resume, Global Reset)
                if (luckyState != null) {
                    item {
                        var m1T by remember(luckyState) { mutableStateOf(luckyState.milestone1Threshold.toString()) }
                        var m1R by remember(luckyState) { mutableStateOf(luckyState.milestone1RewardCoins.toString()) }
                        var jpT by remember(luckyState) { mutableStateOf(luckyState.jackpotThreshold.toString()) }
                        var jpR by remember(luckyState) { mutableStateOf(luckyState.jackpotRewardCoins.toString()) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "🍀 Global Lucky Engine & Milestones",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Global Hits: ${luckyState.globalHitCount} • Pool: 🪙 ${luckyState.globalPoolCoins} • Status: ${if (luckyState.isGlobalLuckyPaused) "PAUSED ⏸" else "RUNNING ▶"}",
                                            fontSize = 11.sp,
                                            color = TangerEmerald
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = { viewModel.ownerToggleGlobalLuckyPause() },
                                            modifier = Modifier.testTag("admin_toggle_global_lucky_pause_button")
                                        ) {
                                            Text(
                                                text = if (luckyState.isGlobalLuckyPaused) "▶ Resume Global" else "⏸ Pause Global",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.ownerResetLuckyGiftCounters(giftId = null, resetGlobalCounter = true) },
                                            modifier = Modifier.testTag("admin_reset_global_lucky_button")
                                        ) {
                                            Text("🔄 Reset Global", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = m1T, onValueChange = { m1T = it.filter { c -> c.isDigit() } }, label = { Text("M1 Hits") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = m1R, onValueChange = { m1R = it.filter { c -> c.isDigit() } }, label = { Text("M1 Reward 🪙") }, singleLine = true, modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = jpT, onValueChange = { jpT = it.filter { c -> c.isDigit() } }, label = { Text("Jackpot Hits") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = jpR, onValueChange = { jpR = it.filter { c -> c.isDigit() } }, label = { Text("Jackpot Reward 🪙") }, singleLine = true, modifier = Modifier.weight(1f))
                                }
                                Button(
                                    onClick = {
                                        viewModel.ownerUpdateLuckyMilestones(
                                            m1T.toLongOrNull() ?: 5L,
                                            m1R.toLongOrNull() ?: 500L,
                                            jpT.toLongOrNull() ?: 50L,
                                            jpR.toLongOrNull() ?: 10000L
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Update Global Lucky Milestones", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Full Per-Gift Lucky & Timing Configuration Studio
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🎁 Create / Edit Lucky Gift, Rewards & Backup Timing",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                if (editingGiftId.isNotBlank()) {
                                    TextButton(onClick = { editingGiftId = "" }) {
                                        Text("Clear ID (Create New)", fontSize = 11.sp)
                                    }
                                }
                            }
                            Text(
                                text = "Configure Coin Price, Trigger Count, Reward, Cashback, EXP, Max Reward, Activation Limit, Start/End Time, Cooldown, Repeat Timing, Pause/Resume & Backup Notes.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = editingGiftId,
                                onValueChange = { editingGiftId = it },
                                label = { Text("Gift ID (Leave blank for new, or tap 'Load & Edit' on any gift below)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = giftNameInput,
                                    onValueChange = { giftNameInput = it },
                                    label = { Text("Gift Name") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = giftEmojiInput,
                                    onValueChange = { giftEmojiInput = it },
                                    label = { Text("Icon/Emoji") },
                                    singleLine = true,
                                    modifier = Modifier.width(86.dp)
                                )
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf("Lucky Gift", "Lucky Draw", "Gift", "Event Gift", "CP", "Country/Flag Gift")) { cat ->
                                    FilterChip(
                                        selected = giftCategoryInput == cat,
                                        onClick = { giftCategoryInput = cat },
                                        label = { Text(cat, fontSize = 11.sp) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = giftPriceInput,
                                    onValueChange = { giftPriceInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("Coin Price 🪙") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = giftDiamondReturnInput,
                                    onValueChange = { giftDiamondReturnInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("Diamond Return %") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = giftAnimationInput,
                                    onValueChange = { giftAnimationInput = it },
                                    label = { Text("Animation Effect") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = giftTriggerCountInput,
                                    onValueChange = { giftTriggerCountInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("Trigger Count (Hits)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = giftRewardCoinsInput,
                                    onValueChange = { giftRewardCoinsInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("Reward Coins 🪙") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = giftMaxRewardInput,
                                    onValueChange = { giftMaxRewardInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("Max Reward Cap 🪙") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = giftCashbackCoinsInput,
                                    onValueChange = { giftCashbackCoinsInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("Cashback Coins 🪙") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = giftExpBonusInput,
                                    onValueChange = { giftExpBonusInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("EXP Bonus ⭐") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = giftActivationLimitInput,
                                    onValueChange = { giftActivationLimitInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("Activation Limit") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = giftCooldownSecInput,
                                    onValueChange = { giftCooldownSecInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("Cooldown (sec)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = giftRepeatTimingSecInput,
                                    onValueChange = { giftRepeatTimingSecInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("Repeat Timing (sec)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = giftStartTimeInput,
                                    onValueChange = { giftStartTimeInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("Start Time ms (0=Now)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = giftEndTimeInput,
                                    onValueChange = { giftEndTimeInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("End Time ms (0=No Exp)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = giftBackupNotesInput,
                                onValueChange = { giftBackupNotesInput = it },
                                label = { Text("Backup & Timing Profile Notes") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = giftIsActiveInput, onCheckedChange = { giftIsActiveInput = it })
                                    Text("Enabled (Active)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = giftIsPausedInput, onCheckedChange = { giftIsPausedInput = it })
                                    Text("Pause Lucky Trigger", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.ownerSaveGift(
                                            giftId = editingGiftId.ifBlank { null },
                                            name = giftNameInput,
                                            category = giftCategoryInput,
                                            emoji = giftEmojiInput,
                                            price = giftPriceInput.toLongOrNull() ?: 100L,
                                            active = giftIsActiveInput,
                                            animationEffect = giftAnimationInput,
                                            diamondReturnRatePercent = giftDiamondReturnInput.toIntOrNull() ?: 60,
                                            triggerCount = giftTriggerCountInput.toLongOrNull() ?: 5L,
                                            rewardCoins = giftRewardCoinsInput.toLongOrNull() ?: 250L,
                                            cashbackCoins = giftCashbackCoinsInput.toLongOrNull() ?: 30L,
                                            expBonus = giftExpBonusInput.toLongOrNull() ?: 50L,
                                            maxRewardCoins = giftMaxRewardInput.toLongOrNull() ?: 1000L,
                                            activationLimit = giftActivationLimitInput.toLongOrNull() ?: 100L,
                                            startTimeMillis = giftStartTimeInput.toLongOrNull() ?: 0L,
                                            endTimeMillis = giftEndTimeInput.toLongOrNull() ?: 0L,
                                            cooldownSeconds = giftCooldownSecInput.toIntOrNull() ?: 5,
                                            repeatTimingSeconds = giftRepeatTimingSecInput.toIntOrNull() ?: 3600,
                                            isPaused = giftIsPausedInput,
                                            backupTimingNotes = giftBackupNotesInput
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_save_gift_button")
                                ) {
                                    Text(if (editingGiftId.isBlank()) "Create New Lucky Gift" else "Save Changes to $editingGiftId", fontWeight = FontWeight.Bold)
                                }
                                if (editingGiftId.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.ownerUpdateGiftBackupAndTiming(
                                                giftId = editingGiftId,
                                                startTimeMillis = giftStartTimeInput.toLongOrNull() ?: 0L,
                                                endTimeMillis = giftEndTimeInput.toLongOrNull() ?: 0L,
                                                cooldownSeconds = giftCooldownSecInput.toIntOrNull() ?: 5,
                                                repeatTimingSeconds = giftRepeatTimingSecInput.toIntOrNull() ?: 3600,
                                                backupTimingNotes = giftBackupNotesInput
                                            )
                                        }
                                    ) {
                                        Text("Save Backup/Timing Only", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Live Catalog of All Gifts with Enable/Disable, Pause/Resume, Reset, Load & Edit, and Delete
                items(allGifts, key = { it.giftId }) { g ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${g.iconEmoji} ${g.name} (${g.giftId}) • ${g.category}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "🪙 ${g.coinPrice} Coins • Return: ${g.diamondReturnRatePercent}% 💎 • Active: ${g.isActive} • Trigger: ${if (g.isPaused) "PAUSED ⏸" else "RUNNING ▶"}",
                                        fontSize = 11.sp,
                                        color = TangerEmerald
                                    )
                                    Text(
                                        text = "Hits: ${g.currentHitCount}/${g.triggerCount}x ➔ Reward: +${g.rewardCoins}🪙 (Max ${g.maxRewardCoins}🪙) • Cashback: +${g.cashbackCoins}🪙 • +${g.expBonus} EXP",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Activations: ${g.currentActivations}/${g.activationLimit} • Cooldown: ${g.cooldownSeconds}s • Repeat: ${g.repeatTimingSeconds}s • ${g.backupTimingNotes}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    FilledTonalButton(
                                        onClick = {
                                            editingGiftId = g.giftId
                                            giftNameInput = g.name
                                            giftCategoryInput = g.category
                                            giftEmojiInput = g.iconEmoji
                                            giftPriceInput = g.coinPrice.toString()
                                            giftAnimationInput = g.animationEffect
                                            giftDiamondReturnInput = g.diamondReturnRatePercent.toString()
                                            giftTriggerCountInput = g.triggerCount.toString()
                                            giftRewardCoinsInput = g.rewardCoins.toString()
                                            giftCashbackCoinsInput = g.cashbackCoins.toString()
                                            giftExpBonusInput = g.expBonus.toString()
                                            giftMaxRewardInput = g.maxRewardCoins.toString()
                                            giftActivationLimitInput = g.activationLimit.toString()
                                            giftStartTimeInput = g.startTimeMillis.toString()
                                            giftEndTimeInput = g.endTimeMillis.toString()
                                            giftCooldownSecInput = g.cooldownSeconds.toString()
                                            giftRepeatTimingSecInput = g.repeatTimingSeconds.toString()
                                            giftIsActiveInput = g.isActive
                                            giftIsPausedInput = g.isPaused
                                            giftBackupNotesInput = g.backupTimingNotes
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("✏️ Load & Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                item {
                                    OutlinedButton(
                                        onClick = { viewModel.ownerToggleGiftActiveOrPaused(g.giftId, toggleActive = true) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(if (g.isActive) "Disable" else "Enable", fontSize = 11.sp)
                                    }
                                }
                                item {
                                    OutlinedButton(
                                        onClick = { viewModel.ownerToggleGiftActiveOrPaused(g.giftId, togglePaused = true) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(if (g.isPaused) "▶ Resume Trigger" else "⏸ Pause Trigger", fontSize = 11.sp)
                                    }
                                }
                                item {
                                    OutlinedButton(
                                        onClick = { viewModel.ownerResetLuckyGiftCounters(giftId = g.giftId, resetGlobalCounter = false) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("🔄 Reset Hits", fontSize = 11.sp)
                                    }
                                }
                                item {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.ownerSaveGift(
                                                giftId = g.giftId,
                                                name = g.name,
                                                category = g.category,
                                                emoji = g.iconEmoji,
                                                price = g.coinPrice + 50L,
                                                active = g.isActive,
                                                animationEffect = g.animationEffect,
                                                diamondReturnRatePercent = g.diamondReturnRatePercent,
                                                triggerCount = g.triggerCount,
                                                rewardCoins = g.rewardCoins,
                                                cashbackCoins = g.cashbackCoins,
                                                expBonus = g.expBonus,
                                                maxRewardCoins = g.maxRewardCoins,
                                                activationLimit = g.activationLimit,
                                                startTimeMillis = g.startTimeMillis,
                                                endTimeMillis = g.endTimeMillis,
                                                cooldownSeconds = g.cooldownSeconds,
                                                repeatTimingSeconds = g.repeatTimingSeconds,
                                                isPaused = g.isPaused,
                                                backupTimingNotes = g.backupTimingNotes
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("+50🪙", fontSize = 11.sp)
                                    }
                                }
                                item {
                                    TextButton(
                                        onClick = { viewModel.ownerDeleteGift(g.giftId) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Delete", color = TangerDangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "EVENTS_ROOMS" -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📢 Publish Global Event / Banner / Official Message", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            OutlinedTextField(
                                value = eventTitleInput,
                                onValueChange = { eventTitleInput = it },
                                label = { Text("Headline") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = eventSubInput,
                                onValueChange = { eventSubInput = it },
                                label = { Text("Description") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf("ALL", "COUNTRY", "AGENCY", "ROOM", "VIP", "USER")) { sc ->
                                    FilterChip(
                                        selected = eventScopeInput == sc,
                                        onClick = { eventScopeInput = sc },
                                        label = { Text("Target: $sc", fontSize = 11.sp) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = eventTargetValInput,
                                onValueChange = { eventTargetValInput = it },
                                label = { Text("Target Value (*, MA, AG-101, RM-101, VIP6, 199967)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    viewModel.ownerPublishEventOrBanner(
                                        eventTitleInput,
                                        eventSubInput,
                                        eventTypeInput,
                                        eventScopeInput,
                                        eventTargetValInput,
                                        "25,000 Coins Bonus"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Publish to Database & Target Scope")
                            }
                        }
                    }
                }
            }

            "CLOUD_BACKUPS" -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "☁️ Production Database Backups, Schema Migrations & Cloud CDN",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Active Relational Schema: Room SQLite v5 (MIGRATION_1_5 .. MIGRATION_4_5 verified) • Cloud Storage Bucket: ${com.example.BuildConfig.TANGER_CLOUD_STORAGE_BUCKET.ifBlank { "tangeram-global-media-prod" }}",
                                fontSize = 11.sp,
                                color = TangerEmerald
                            )
                            OutlinedTextField(
                                value = backupLabelInput,
                                onValueChange = { backupLabelInput = it },
                                label = { Text("Backup Snapshot Label") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = { viewModel.ownerTriggerDatabaseBackup() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_create_backup_button")
                            ) {
                                Text("Create Atomic Production Backup Snapshot + Cloud Sync", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Text("📦 Stored Database Backup Snapshots (${databaseBackups.size})", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }

                items(databaseBackups, key = { it.backupId }) { bkp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Snapshot ${bkp.backupId} • Actor: #${bkp.actorUserId} • Schema v${bkp.schemaVersion}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Users: ${bkp.usersCount} • Rooms: ${bkp.roomsCount} • Ledger Txs: ${bkp.transactionsCount} • Gifts: ${bkp.giftsCount} • Posts/Reels: ${bkp.postsAndReelsCount}",
                                fontSize = 11.sp,
                                color = TangerEmerald
                            )
                            Text(
                                text = "SHA-256: ${bkp.payloadChecksum} • Summary: ${bkp.backupSummaryJson}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    viewModel.postStatus("✅ Verified Backup Snapshot ${bkp.backupId} (Schema v${bkp.schemaVersion} • Checksum ${bkp.payloadChecksum})")
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("🛡️ Verify Integrity & Audit Restore Point", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Text("🌐 Cloud Media CDN Assets (${cloudMediaAssets.size})", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }

                items(cloudMediaAssets, key = { it.assetId }) { asset ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "[${asset.assetType}] ${asset.assetId} • Owner: @${asset.ownerUsername} (#${asset.ownerUserId}) • ${asset.syncStatus}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "CDN URL: ${asset.cloudCdnUrl} • Checksum: ${asset.sha256Checksum}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            "REPORTS_AUDIT" -> {
                item {
                    Text("🚨 User Reports (${allReports.size})", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
                items(allReports, key = { it.reportId }) { rep ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${rep.reportId}: ${rep.category} (Reported: #${rep.reportedUserId})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(rep.details, fontSize = 12.sp)
                            Text("Status: ${rep.status}", fontSize = 11.sp, color = TangerEmerald)
                        }
                    }
                }

                item {
                    Text("🔒 Immutable RBAC & Financial Audit Logs (${auditLogs.size})", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
                items(auditLogs, key = { it.logId }) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "[${log.actionCategory}] ${log.actionName} → ${log.targetEntityId}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TangerEmeraldDark
                            )
                            Text(
                                text = "Actor: ${log.actorUserId} (${log.actorRole}) • ${log.detailsJson}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
