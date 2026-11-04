package com.wakemethere.app.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wakemethere.app.ui.config.AlarmConfigScreen
import com.wakemethere.app.ui.config.AlarmConfigViewModel
import com.wakemethere.app.ui.home.HomeScreen
import com.wakemethere.app.ui.home.HomeViewModel
import com.wakemethere.app.ui.map.MapPickerScreen
import com.wakemethere.app.ui.map.MapPickerViewModel
import com.wakemethere.app.ui.navigation.Screen
import com.wakemethere.app.ui.theme.WakeMeThereTheme
import com.wakemethere.app.util.PermissionUtils

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

    // Permission Launchers
    private val foregroundLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            homeViewModel.refreshSystemStatus()
            // If fine location was granted and we are on Android 10+, prompt background next
            if (PermissionUtils.hasFineLocationPermission(this) &&
                !PermissionUtils.hasBackgroundLocationPermission(this) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            ) {
                backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }

    private val backgroundLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            homeViewModel.refreshSystemStatus()
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            homeViewModel.refreshSystemStatus()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check and request permissions if needed on start
        checkInitialPermissions()

        setContent {
            WakeMeThereTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route
                    ) {
                        // 1. Home Screen (Alarm List)
                        composable(Screen.Home.route) {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onNavigateToMapPicker = {
                                    navController.navigate(Screen.MapPicker.route)
                                },
                                onNavigateToEditAlarm = { alarmId ->
                                    navController.navigate(Screen.EditAlarm.createRoute(alarmId))
                                },
                                onRequestForegroundLocation = {
                                    foregroundLocationPermissionLauncher.launch(
                                        PermissionUtils.getForegroundLocationPermissions()
                                    )
                                },
                                onRequestBackgroundLocation = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        backgroundLocationPermissionLauncher.launch(
                                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                        )
                                    }
                                },
                                onRequestNotification = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    }
                                }
                            )
                        }

                        // 2. Map Picker Screen
                        composable(Screen.MapPicker.route) {
                            val mapViewModel: MapPickerViewModel by viewModels()
                            MapPickerScreen(
                                viewModel = mapViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onConfirmLocation = { lat, lng, radius, areaName ->
                                    navController.navigate(
                                        Screen.AlarmConfig.createRoute(lat, lng, radius, areaName)
                                    )
                                }
                            )
                        }

                        // 3. Alarm Configuration Screen
                        composable(
                            route = Screen.AlarmConfig.route,
                            arguments = listOf(
                                navArgument("lat") { type = NavType.StringType },
                                navArgument("lng") { type = NavType.StringType },
                                navArgument("radius") { type = NavType.StringType },
                                navArgument("name") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                    nullable = true
                                }
                            )
                        ) { backStackEntry ->
                            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
                            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0
                            val radius = backStackEntry.arguments?.getString("radius")?.toFloatOrNull() ?: 500f
                            val encodedName = backStackEntry.arguments?.getString("name") ?: ""
                            val initialName = try {
                                java.net.URLDecoder.decode(encodedName, "UTF-8")
                            } catch (e: Exception) {
                                ""
                            }

                            val configViewModel: AlarmConfigViewModel by viewModels()
                            AlarmConfigScreen(
                                latitude = lat,
                                longitude = lng,
                                radiusMeters = radius,
                                initialName = initialName,
                                viewModel = configViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onSaveSuccess = {
                                    // Navigate back to Home and clear backstack
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Home.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 4. Edit Alarm Screen
                        composable(
                            route = Screen.EditAlarm.route,
                            arguments = listOf(
                                navArgument("alarmId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: -1L
                            val configViewModel: AlarmConfigViewModel by viewModels()
                            AlarmConfigScreen(
                                alarmIdToEdit = alarmId,
                                viewModel = configViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onSaveSuccess = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Home.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkInitialPermissions() {
        if (!PermissionUtils.hasFineLocationPermission(this)) {
            foregroundLocationPermissionLauncher.launch(
                PermissionUtils.getForegroundLocationPermissions()
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PermissionUtils.hasNotificationPermission(this)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.refreshSystemStatus()
    }
}

