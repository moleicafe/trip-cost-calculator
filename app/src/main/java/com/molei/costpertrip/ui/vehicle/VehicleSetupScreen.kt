package com.molei.costpertrip.ui.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.molei.costpertrip.domain.FuelPriceMode
import com.molei.costpertrip.domain.display2dp
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleSetupScreen(
    vehicleId: Long?,
    onDone: () -> Unit,
    vm: VehicleEditViewModel = viewModel(factory = VehicleEditViewModel.factory(vehicleId)),
) {
    val form by vm.form.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vehicleId == null) "Add Vehicle" else "Edit Vehicle") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (!form.loaded) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = form.name,
                onValueChange = { text -> vm.update { it.copy(name = text) } },
                label = { Text("Name") },
                placeholder = { Text("Honda Civic 1.6 VTi CVT") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.consumptionNormal,
                onValueChange = { text -> vm.update { it.copy(consumptionNormal = text) } },
                label = { Text("Consumption — normal") },
                suffix = { Text("L/100km") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = form.ecoAutoDerived,
                    onCheckedChange = { checked -> vm.update { it.copy(ecoAutoDerived = checked) } },
                )
                Text(
                    "Auto-calculate eco consumption from eco saving %",
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            if (form.ecoAutoDerived) {
                OutlinedTextField(
                    value = form.ecoSavingPercent,
                    onValueChange = { text -> vm.update { it.copy(ecoSavingPercent = text) } },
                    label = { Text("Eco saving") },
                    suffix = { Text("%") },
                    supportingText = { Text("Typical range 5–15%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.derivedEco?.let { it.trim3dp() } ?: "",
                    onValueChange = {},
                    enabled = false,
                    label = { Text("Consumption — eco (auto)") },
                    suffix = { Text("L/100km") },
                    supportingText = { Text("= normal × (1 − eco saving % / 100)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OutlinedTextField(
                    value = form.consumptionEco,
                    onValueChange = { text -> vm.update { it.copy(consumptionEco = text) } },
                    label = { Text("Consumption — eco") },
                    suffix = { Text("L/100km") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = form.tankCapacityLitres,
                onValueChange = { text -> vm.update { it.copy(tankCapacityLitres = text) } },
                label = { Text("Tank capacity") },
                suffix = { Text("L") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Fuel price", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                FuelPriceMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = form.fuelPriceMode == mode,
                        onClick = { vm.update { it.copy(fuelPriceMode = mode) } },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = FuelPriceMode.entries.size),
                    ) { Text(if (mode == FuelPriceMode.PerTank) "Per tank" else "Per litre") }
                }
            }

            if (form.fuelPriceMode == FuelPriceMode.PerTank) {
                OutlinedTextField(
                    value = form.fuelPricePerTank,
                    onValueChange = { text -> vm.update { it.copy(fuelPricePerTank = text) } },
                    label = { Text("Cost to fill full tank") },
                    prefix = { Text(form.currencySymbol) },
                    supportingText = {
                        form.derivedPricePerLitre?.let {
                            Text("= ${form.currencySymbol}${it.display2dp()} per litre")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OutlinedTextField(
                    value = form.fuelPricePerLitre,
                    onValueChange = { text -> vm.update { it.copy(fuelPricePerLitre = text) } },
                    label = { Text("Price per litre") },
                    prefix = { Text(form.currencySymbol) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = form.currencySymbol,
                onValueChange = { text -> vm.update { it.copy(currencySymbol = text) } },
                label = { Text("Currency symbol") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            form.saveError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { vm.save(onSaved = onDone) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save vehicle") }

            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun Double.trim3dp(): String =
    BigDecimal.valueOf(this).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
