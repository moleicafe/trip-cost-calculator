package com.molei.fuelcost

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import com.molei.fuelcost.ui.theme.FuelCostTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FuelCostTheme {
                Text("Fuel Cost") // replaced by AppRoot in the UI task
            }
        }
    }
}
