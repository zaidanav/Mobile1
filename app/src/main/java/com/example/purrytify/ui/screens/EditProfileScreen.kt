package com.example.purrytify.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.R
import com.example.purrytify.models.LocationResult
import com.example.purrytify.repository.UserRepository
import com.example.purrytify.ui.components.CountrySelectionDialog
import com.example.purrytify.ui.components.LocationSelectionDialog
import com.example.purrytify.ui.theme.BACKGROUND_COLOR
import com.example.purrytify.ui.theme.GREEN_COLOR
import com.example.purrytify.util.TokenManager
import com.example.purrytify.viewmodels.EditProfileViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokenManager = TokenManager(context)
    val userRepository = UserRepository(tokenManager)

    // ViewModel
    val viewModel: EditProfileViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return EditProfileViewModel(
                    context.applicationContext as android.app.Application,
                    userRepository
                ) as T
            }
        }
    )

    // State dari ViewModel
    val currentProfile by viewModel.currentProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val selectedPhotoUri by viewModel.selectedPhotoUri.collectAsState()
    val isLoadingLocation by viewModel.isLoadingLocation.collectAsState()
    val needLocationPermission by viewModel.needLocationPermission.collectAsState()
    val needCameraPermission by viewModel.needCameraPermission.collectAsState()

    // Dialog states
    var showLocationSelectionDialog by remember { mutableStateOf(false) }
    var showCountrySelectorDialog by remember { mutableStateOf(false) }
    var showCoordinateInputDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Activity result launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("EditProfileScreen", "Camera result received")
            viewModel.setPhotoFromCamera()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val photoUri = result.data?.data
            Log.d("EditProfileScreen", "Gallery result: $photoUri")
            viewModel.setPhotoFromGallery(photoUri)
        }
    }

    // Google Maps launcher
    val googleMapsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("EditProfileScreen", "=== GOOGLE MAPS RESULT ===")
        Log.d("EditProfileScreen", "Result code: ${result.resultCode}")
        Log.d("EditProfileScreen", "Data: ${result.data}")
        Log.d("EditProfileScreen", "URI: ${result.data?.data}")

        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val locationResult = viewModel.locationHelper.parseGoogleMapsResult(result.data)
                if (locationResult != null) {
                    Log.d("EditProfileScreen", "Location parsed: ${locationResult.countryName}")
                    viewModel.setManualLocation(locationResult)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Location selected: ${locationResult.countryName}",
                            duration = SnackbarDuration.Short
                        )
                    }
                } else {
                    Log.w("EditProfileScreen", "Failed to parse location")
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Could not get location from Maps. Try pasting coordinates or select from country list.",
                            duration = SnackbarDuration.Long
                        )
                    }
                    showCoordinateInputDialog = true
                }
            }
            Activity.RESULT_CANCELED -> {
                Log.d("EditProfileScreen", "Maps cancelled")
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "You can paste coordinates here or select from country list",
                        duration = SnackbarDuration.Long
                    )
                }
                showCoordinateInputDialog = true
            }
            else -> {
                Log.w("EditProfileScreen", "Unexpected result: ${result.resultCode}")
                showCoordinateInputDialog = true
            }
        }
    }

    // Permission launchers
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.onLocationPermissionGranted()
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onCameraPermissionGranted()
        } else {
            viewModel.onCameraPermissionDenied()
        }
    }

    // Handle permission requests
    LaunchedEffect(needLocationPermission) {
        if (needLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(needCameraPermission) {
        if (needCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Show messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccess()
        }
    }

    // Load profile
    LaunchedEffect(Unit) {
        viewModel.loadCurrentProfile()
    }

    // Dialog untuk memilih method location
    if (showLocationSelectionDialog) {
        LocationSelectionDialog(
            onGoogleMapsClick = {
                try {
                    // Launch Google Maps untuk pick location
                    val intent = viewModel.locationHelper.createGoogleMapsPickerIntent()
                    googleMapsLauncher.launch(intent)
                } catch (e: Exception) {
                    Log.e("EditProfileScreen", "Error launching Google Maps: ${e.message}")
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Error opening Google Maps: ${e.message}",
                            duration = SnackbarDuration.Long
                        )
                    }
                    showCountrySelectorDialog = true
                }
            },
            onDismiss = { showLocationSelectionDialog = false }
        )
    }

    // Dialog untuk input koordinat manual
    if (showCoordinateInputDialog) {
        CoordinateInputDialog(
            onCoordinateSubmit = { latitude, longitude ->
                val locationResult = viewModel.locationHelper.parseCoordinateString(latitude, longitude)
                if (locationResult != null) {
                    viewModel.setManualLocation(locationResult)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Location set: ${locationResult.countryName}",
                            duration = SnackbarDuration.Short
                        )
                    }
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Invalid coordinates. Please check format.",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            },
            onCountryListClick = {
                showCountrySelectorDialog = true
            },
            onDismiss = { showCoordinateInputDialog = false }
        )
    }

    // Dialog untuk pilih country
    if (showCountrySelectorDialog) {
        CountrySelectionDialog(
            onCountrySelected = { countryCode, countryName ->
                val locationResult = LocationResult(
                    countryCode = countryCode,
                    countryName = countryName,
                    address = countryName,
                    latitude = null,
                    longitude = null
                )
                viewModel.setManualLocation(locationResult)
            },
            onDismiss = { showCountrySelectorDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Profile",
                        fontFamily = FontFamily(Font(R.font.poppins_semi_bold))
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BACKGROUND_COLOR,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BACKGROUND_COLOR
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading && currentProfile == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GREEN_COLOR)
                }
            } else {
                // Profile Photo Section
                ProfilePhotoSection(
                    currentProfile = currentProfile,
                    selectedPhotoUri = selectedPhotoUri,
                    onCameraClick = {
                        if (viewModel.photoHelper.hasCameraPermission()) {
                            val cameraIntent = viewModel.photoHelper.createCameraIntent()
                            cameraIntent?.let { (intent, _) ->
                                cameraLauncher.launch(intent)
                            }
                        } else {
                            viewModel.requestCameraPermission()
                        }
                    },
                    onGalleryClick = {
                        val galleryIntent = viewModel.photoHelper.createGalleryIntent()
                        galleryLauncher.launch(galleryIntent)
                    },
                    onClearPhoto = { viewModel.clearSelectedPhoto() }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Location Section
                LocationSection(
                    currentProfile = currentProfile,
                    selectedLocation = selectedLocation,
                    isLoadingLocation = isLoadingLocation,
                    onAutoDetectClick = { viewModel.getCurrentLocation() },
                    onManualSelectClick = {
                        showLocationSelectionDialog = true
                    },
                    onClearLocation = { viewModel.clearSelectedLocation() }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Save Button
                Button(
                    onClick = { viewModel.saveProfileChanges() },
                    enabled = !isLoading && viewModel.hasUnsavedChanges(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GREEN_COLOR,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "Save Changes",
                            fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
                            fontSize = 16.sp
                        )
                    }
                }

                if (!viewModel.hasUnsavedChanges()) {
                    Text(
                        text = "No changes to save",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun CoordinateInputDialog(
    onCoordinateSubmit: (String, String) -> Unit,
    onCountryListClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var latitudeText by remember { mutableStateOf("") }
    var longitudeText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    // Try to parse clipboard for coordinates
    LaunchedEffect(Unit) {
        try {
            val clipboardText = clipboardManager.getText()?.text
            if (!clipboardText.isNullOrBlank()) {
                // Try to extract coordinates from clipboard
                val coordPattern = Regex("(-?\\d+\\.\\d+),\\s*(-?\\d+\\.\\d+)")
                coordPattern.find(clipboardText)?.let { match ->
                    latitudeText = match.groupValues[1]
                    longitudeText = match.groupValues[2]
                }
            }
        } catch (e: Exception) {
            Log.d("CoordinateDialog", "Could not parse clipboard: ${e.message}")
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Enter Coordinates",
                    color = Color.White,
                    fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Paste coordinates from Google Maps or enter manually:",
                    color = Color.Gray,
                    fontFamily = FontFamily(Font(R.font.poppins_regular)),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Paste button
                OutlinedButton(
                    onClick = {
                        try {
                            val clipboardText = clipboardManager.getText()?.text
                            if (!clipboardText.isNullOrBlank()) {
                                val coordPattern = Regex("(-?\\d+\\.\\d+),\\s*(-?\\d+\\.\\d+)")
                                coordPattern.find(clipboardText)?.let { match ->
                                    latitudeText = match.groupValues[1]
                                    longitudeText = match.groupValues[2]
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("CoordinateDialog", "Error pasting: ${e.message}")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Paste from Clipboard")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Latitude input
                androidx.compose.material3.OutlinedTextField(
                    value = latitudeText,
                    onValueChange = { latitudeText = it },
                    label = { Text("Latitude") },
                    placeholder = { Text("-6.2088") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GREEN_COLOR,
                        focusedLabelColor = GREEN_COLOR,
                        cursorColor = GREEN_COLOR
                    ),
                    textStyle = TextStyle(color = Color.White)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Longitude input
                androidx.compose.material3.OutlinedTextField(
                    value = longitudeText,
                    onValueChange = { longitudeText = it },
                    label = { Text("Longitude") },
                    placeholder = { Text("106.8456") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GREEN_COLOR,
                        focusedLabelColor = GREEN_COLOR,
                        cursorColor = GREEN_COLOR
                    ),
                    textStyle = TextStyle(color = Color.White)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCountryListClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Country List")
                    }

                    Button(
                        onClick = {
                            if (latitudeText.isNotBlank() && longitudeText.isNotBlank()) {
                                onCoordinateSubmit(latitudeText, longitudeText)
                                onDismiss()
                            }
                        },
                        enabled = latitudeText.isNotBlank() && longitudeText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GREEN_COLOR,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Set Location")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Cancel",
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// ProfilePhotoSection dan LocationSection tetap sama seperti implementasi sebelumnya
@Composable
fun ProfilePhotoSection(
    currentProfile: com.example.purrytify.models.UserProfile?,
    selectedPhotoUri: Uri?,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onClearPhoto: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Profile Photo",
            color = Color.White,
            fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.Gray)
                .border(2.dp, GREEN_COLOR, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selectedPhotoUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(selectedPhotoUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Selected Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (currentProfile != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("http://34.101.226.132:3000/uploads/profile-picture/${currentProfile.profilePhoto}")
                        .crossfade(true)
                        .build(),
                    contentDescription = "Current Profile Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.ic_person_placeholder),
                    fallback = painterResource(id = R.drawable.ic_person_placeholder)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Default Profile",
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCameraClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color.White)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Camera")
            }

            OutlinedButton(
                onClick = onGalleryClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color.White)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Gallery")
            }

            if (selectedPhotoUri != null) {
                TextButton(
                    onClick = onClearPhoto,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Text("Clear")
                }
            }
        }
    }
}

@Composable
fun LocationSection(
    currentProfile: com.example.purrytify.models.UserProfile?,
    selectedLocation: com.example.purrytify.models.LocationResult?,
    isLoadingLocation: Boolean,
    onAutoDetectClick: () -> Unit,
    onManualSelectClick: () -> Unit,
    onClearLocation: () -> Unit
) {
    Column {
        Text(
            text = "Location",
            color = Color.White,
            fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Current Location",
                    color = Color.Gray,
                    fontFamily = FontFamily(Font(R.font.poppins_regular)),
                    fontSize = 14.sp
                )
                Text(
                    text = if (currentProfile?.location != null) {
                        "${com.example.purrytify.util.CountryCodeHelper.getCountryName(currentProfile.location)} (${currentProfile.location})"
                    } else {
                        "Not set"
                    },
                    color = Color.White,
                    fontFamily = FontFamily(Font(R.font.poppins_regular)),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                if (selectedLocation != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "New Location",
                        color = GREEN_COLOR,
                        fontFamily = FontFamily(Font(R.font.poppins_regular)),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${selectedLocation.countryName} (${selectedLocation.countryCode})",
                        color = Color.White,
                        fontFamily = FontFamily(Font(R.font.poppins_regular)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    selectedLocation.address?.let { address ->
                        Text(
                            text = address,
                            color = Color.Gray,
                            fontFamily = FontFamily(Font(R.font.poppins_regular)),
                            fontSize = 12.sp
                        )
                    }
                    if (selectedLocation.latitude != null && selectedLocation.longitude != null) {
                        Text(
                            text = "Coordinates: ${String.format("%.4f", selectedLocation.latitude)}, ${String.format("%.4f", selectedLocation.longitude)}",
                            color = Color.Gray,
                            fontFamily = FontFamily(Font(R.font.poppins_regular)),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onAutoDetectClick,
                enabled = !isLoadingLocation,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color.White)
                ),
                modifier = Modifier.weight(1f)
            ) {
                if (isLoadingLocation) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Auto Detect",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Auto Detect")
            }

            OutlinedButton(
                onClick = onManualSelectClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color.White)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Select Manually",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Select on Map")
            }
        }

        if (selectedLocation != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onClearLocation,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Red
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear Selected Location")
            }
        }
    }
}