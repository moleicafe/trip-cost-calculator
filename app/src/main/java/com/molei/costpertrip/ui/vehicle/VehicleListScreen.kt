package com.molei.costpertrip.ui.vehicle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.molei.costpertrip.data.Vehicle
import com.molei.costpertrip.domain.FuelPriceMode
import com.molei.costpertrip.ui.display2dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    vm: VehicleListViewModel = viewModel(factory = VehicleListViewModel.Factory),
) {
    val vehicles by vm.vehicles.collectAsState()
    var pendingDelete by remember { mutableStateOf<Vehicle?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Vehicles") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = "Add vehicle") }
        },
    ) { padding ->
        if (vehicles.isEmpty()) {
            Text(
                "No vehicles yet. Tap + to add one.",
                modifier = Modifier.padding(padding).padding(24.dp),
            )
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(vehicles, key = { it.id }) { vehicle ->
                    val price = when (vehicle.fuelPriceMode) {
                        FuelPriceMode.PerTank ->
                            "${vehicle.currencySymbol}${vehicle.fuelPricePerTank.display2dp()}/tank"
                        FuelPriceMode.PerLitre ->
                            "${vehicle.currencySymbol}${vehicle.fuelPricePerLitre.display2dp()}/L"
                    }
                    ListItem(
                        headlineContent = { Text(vehicle.name) },
                        supportingContent = {
                            Text(
                                "${vehicle.consumptionNormal} / ${vehicle.consumptionEco} L/100km (normal/eco) · " +
                                    "${vehicle.tankCapacityLitres} L tank · $price"
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { pendingDelete = vehicle }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete ${vehicle.name}")
                            }
                        },
                        modifier = Modifier.clickable { onEdit(vehicle.id) },
                    )
                }
            }
        }
    }

    pendingDelete?.let { vehicle ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${vehicle.name}?") },
            text = { Text("Its trips will be deleted too. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(vehicle)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}
