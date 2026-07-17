package com.molei.fuelcost.ui.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.molei.fuelcost.data.DistanceSource
import com.molei.fuelcost.domain.TripMode
import com.molei.fuelcost.ui.display2dp
import com.molei.fuelcost.ui.formatMoney
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTripScreen(
    onAddVehicle: () -> Unit,
    vm: NewTripViewModel = viewModel(factory = NewTripViewModel.Factory),
) {
    val vehicles by vm.vehicles.collectAsState()
    val form by vm.form.collectAsState()
    val selectedVehicle by vm.selectedVehicle.collectAsState()
    val calc by vm.calculation.collectAsState()
    val hasApiKey by vm.hasApiKey.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("New Trip") }) },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (vehicles.isEmpty()) {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No vehicles yet", style = MaterialTheme.typography.titleMedium)
                        Text("Add a vehicle profile (consumption, tank size, fuel price) to start calculating trip costs.")
                        Button(onClick = onAddVehicle) { Text("Add a vehicle") }
                    }
                }
                return@Column
            }

            // Vehicle dropdown, defaults to last-used
            var dropdownExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedVehicle?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Vehicle") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                ) {
                    vehicles.forEach { vehicle ->
                        DropdownMenuItem(
                            text = { Text(vehicle.name) },
                            onClick = {
                                vm.onVehicleSelected(vehicle.id)
                                dropdownExpanded = false
                            },
                        )
                    }
                }
            }

            // Mode toggle
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TripMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = form.mode == mode,
                        onClick = { vm.onModeChanged(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = TripMode.entries.size),
                    ) { Text(mode.name) }
                }
            }

            // Distance: manual entry...
            OutlinedTextField(
                value = form.distanceText,
                onValueChange = vm::onDistanceChanged,
                label = { Text("Distance") },
                suffix = { Text("km") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = {
                    if (form.distanceSource == DistanceSource.DirectionsAPI) {
                        Text("Road distance from Google Directions — edit to override.")
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // ...or address-to-address lookup
            Text("Or calculate from addresses", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = form.startLocation,
                onValueChange = vm::onStartChanged,
                label = { Text("Start address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.endLocation,
                onValueChange = vm::onEndChanged,
                label = { Text("End address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = vm::lookupDistance,
                    enabled = hasApiKey && !form.lookupInProgress,
                ) { Text("Calculate distance") }
                if (form.lookupInProgress) CircularProgressIndicator(Modifier.size(24.dp))
            }
            if (!hasApiKey) {
                Text(
                    "Address lookup needs a Google Maps API key — add one in Settings. " +
                        "Until then, enter the distance manually above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            form.lookupError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // Live result card — recomputes as the user types
            val symbol = selectedVehicle?.currencySymbol ?: "$"
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Result", style = MaterialTheme.typography.titleMedium)
                    ResultRow("Consumption rate", calc?.let { "${it.consumptionRate} L/100km (${form.mode})" } ?: "—")
                    ResultRow("Price per litre", calc?.let { formatMoney(symbol, it.pricePerLitre) } ?: "—")
                    ResultRow("Fuel used", calc?.let { "${it.fuelUsedLitres.display2dp()} L" } ?: "—")
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Trip cost", style = MaterialTheme.typography.titleMedium)
                        Text(
                            calc?.let { formatMoney(symbol, it.cost) } ?: "—",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Button(
                onClick = {
                    vm.saveTrip { scope.launch { snackbarHost.showSnackbar("Trip saved") } }
                },
                enabled = calc != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save trip") }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}
