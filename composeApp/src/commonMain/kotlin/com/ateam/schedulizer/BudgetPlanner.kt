package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import schedulizer.composeapp.generated.resources.Res

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun BudgetPlanner() {
    val rows = mutableListOf(
        InputRow("Enter Wage",false, InputType.int),
        InputRow("Hours Worked", false,InputType.int),
        InputRow("Kiwisaver", true,InputType.percent),
        InputRow("Student Loan", true,InputType.int,InputType.percent),
        InputRow("Holiday Pay", true, InputType.percent),
        InputRow("Overtime", true,InputType.int,InputType.int),
    )
    Column  (Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally){
            rows.forEach { inputRow ->
                inputRow.BuildRow()
            }
        var expanded by remember { mutableStateOf(false) }
        var selectedVal by remember { mutableStateOf(WeekOptions.Weekly) }

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedVal.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Display pay for this..") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {

                WeekOptions.entries.forEach { weekOptions ->
                    DropdownMenuItem(
                        text = { Text(weekOptions.toString()) },
                        onClick = {  expanded = false; selectedVal = weekOptions }
                    )
                }

            }
        }

        OutlinedButton(modifier = Modifier.fillMaxWidth(0.8f), onClick = {}) {
            Text("Calculate")
        }
    }




}

enum class WeekOptions {
Weekly
}