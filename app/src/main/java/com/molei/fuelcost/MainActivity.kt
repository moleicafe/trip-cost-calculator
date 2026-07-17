package com.molei.fuelcost

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.molei.fuelcost.ui.detail.TripDetailScreen
import com.molei.fuelcost.ui.history.TripHistoryScreen
import com.molei.fuelcost.ui.settings.SettingsScreen
import com.molei.fuelcost.ui.theme.FuelCostTheme
import com.molei.fuelcost.ui.trip.NewTripScreen
import com.molei.fuelcost.ui.vehicle.VehicleListScreen
import com.molei.fuelcost.ui.vehicle.VehicleSetupScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FuelCostTheme {
                AppRoot()
            }
        }
    }
}

private data class TopLevelDestination(val route: String, val label: String, val icon: ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination("newTrip", "New Trip", Icons.Default.LocalGasStation),
    TopLevelDestination("history", "History", Icons.Default.History),
    TopLevelDestination("vehicles", "Vehicles", Icons.Default.DirectionsCar),
    TopLevelDestination("settings", "Settings", Icons.Default.Settings),
)

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (topLevelDestinations.any { it.route == currentRoute }) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "newTrip",
            modifier = Modifier.padding(padding),
        ) {
            composable("newTrip") {
                NewTripScreen(onAddVehicle = { navController.navigate("vehicleEdit") })
            }
            composable("history") {
                TripHistoryScreen(onTripClick = { id -> navController.navigate("trip/$id") })
            }
            composable("vehicles") {
                VehicleListScreen(
                    onAdd = { navController.navigate("vehicleEdit") },
                    onEdit = { id -> navController.navigate("vehicleEdit?vehicleId=$id") },
                )
            }
            composable("settings") { SettingsScreen() }
            composable(
                route = "trip/{tripId}",
                arguments = listOf(navArgument("tripId") { type = NavType.LongType }),
            ) { entry ->
                TripDetailScreen(
                    tripId = entry.arguments?.getLong("tripId") ?: 0L,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "vehicleEdit?vehicleId={vehicleId}",
                arguments = listOf(navArgument("vehicleId") { type = NavType.LongType; defaultValue = -1L }),
            ) { entry ->
                val id = entry.arguments?.getLong("vehicleId") ?: -1L
                VehicleSetupScreen(
                    vehicleId = id.takeIf { it >= 0 },
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}
