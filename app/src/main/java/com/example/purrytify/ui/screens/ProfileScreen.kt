package com.example.purrytify.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.MainActivity
import com.example.purrytify.R
import com.example.purrytify.models.UserProfile
import com.example.purrytify.ui.theme.BACKGROUND_COLOR
import com.example.purrytify.viewmodels.ProfileViewModel
import com.example.purrytify.viewmodels.ViewModelFactory

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = viewModel(
        factory = ViewModelFactory(LocalContext.current)
    )
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // State untuk menyimpan data
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Observe LiveData dari ViewModel
    DisposableEffect(lifecycleOwner) {
        // Observer untuk profile data
        val profileObserver = androidx.lifecycle.Observer<UserProfile> { profile ->
            userProfile = profile
        }

        // Observer untuk loading state
        val loadingObserver = androidx.lifecycle.Observer<Boolean> { loading ->
            isLoading = loading
        }

        // Observer untuk error message
        val errorObserver = androidx.lifecycle.Observer<String> { errorMsg ->
            error = errorMsg
        }

        // Register observers
        profileViewModel.profileData.observe(lifecycleOwner, profileObserver)
        profileViewModel.isLoading.observe(lifecycleOwner, loadingObserver)
        profileViewModel.error.observe(lifecycleOwner, errorObserver)

        // Load profile data when screen is shown
        profileViewModel.loadUserProfile()

        // Clean up observers when leaving the screen
        onDispose {
            profileViewModel.profileData.removeObserver(profileObserver)
            profileViewModel.isLoading.removeObserver(loadingObserver)
            profileViewModel.error.removeObserver(errorObserver)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BACKGROUND_COLOR)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Profile",
            color = Color.White,
            fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
            fontSize = 24.sp,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (isLoading) {
            // Show loading indicator
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp),
                color = Color.White
            )
        } else if (error != null) {
            // Show error message
            Text(
                text = "Error: $error",
                color = Color.Red,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        } else if (userProfile != null) {
            // Show profile information
            ProfileContent(userProfile = userProfile!!, viewModel = profileViewModel)
        }
    }
}

@Composable
fun ProfileContent(userProfile: UserProfile, viewModel: ProfileViewModel) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Photo
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("http://34.101.226.132:3000/uploads/profile-picture/${userProfile.profilePhoto}")
                .crossfade(true)
                .build(),
            contentDescription = "Profile Photo",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.Gray), // Gunakan background sebagai placeholder
            contentScale = ContentScale.Crop
            // Hapus placeholder dan error image
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Username
        Text(
            text = userProfile.username,
            color = Color.White,
            fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Email
        Text(
            text = userProfile.email,
            color = Color.White,
            fontFamily = FontFamily(Font(R.font.poppins_regular)),
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Location
        Text(
            text = "Location: ${userProfile.location}",
            color = Color.White,
            fontFamily = FontFamily(Font(R.font.poppins_regular)),
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Stats section
        Text(
            text = "Your Music Stats",
            color = Color.White,
            fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stats cards in a row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Added Songs
            StatCard(count = "0", label = "Added")

            // Liked Songs
            StatCard(count = "0", label = "Liked")

            // Listened Songs
            StatCard(count = "0", label = "Listened")
        }

        // Spacer to push the logout button to the bottom
        Spacer(modifier = Modifier.height(48.dp))

        // Logout Button
        Button(
            onClick = {
                viewModel.logout()
                (context as? MainActivity)?.recreate()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }
}

@Composable
fun StatCard(count: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = count,
            color = Color.White,
            fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
            fontSize = 24.sp
        )

        Text(
            text = label,
            color = Color.White,
            fontFamily = FontFamily(Font(R.font.poppins_regular)),
            fontSize = 14.sp
        )
    }
}