package com.ateam.schedulizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.navigator.internal.BackHandler
import com.ateam.schedulizer.entities.Expense
import com.ateam.schedulizer.entities.Settings
import kotlinx.coroutines.launch
import kotlin.math.round

enum class Frequency {
    Weekly, Fortnightly, Monthly, Yearly
}

fun formatCost(amount: Float): String {
    val rounded = round(amount * 100f) / 100f
    val dollars = rounded.toLong()
    val cents = round((rounded - dollars) * 100f).toInt()
    val centsStr = if (cents < 10) "0$cents" else "$cents"
    return "$dollars.$centsStr"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatExpense(
    expense: Expense,
    onUpdate: (Expense) -> Unit,
    onDelete: () -> Unit
) {
    var nameT by remember(expense.id, expense.name) { mutableStateOf(expense.name) }
    var costT by remember(expense.id, expense.cost) {
        mutableStateOf(if (expense.cost == 0f) "" else formatCost(expense.cost))
    }
    var frequencyT by remember(expense.id, expense.frequency) { mutableStateOf(expense.frequency) }
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = nameT,
                    onValueChange = { newName ->
                        nameT = newName
                        onUpdate(expense.copy(name = newName))
                    },
                    label = { Text("Expense Name") },
                    placeholder = { Text("e.g. Rent, Groceries") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete expense",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = costT,
                    onValueChange = { newVal ->
                        var filtered = newVal.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } > 1) {
                            val firstDot = filtered.indexOf('.')
                            filtered = filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
                        }
                        costT = filtered
                        val newCost = costT.toFloatOrNull() ?: 0f
                        onUpdate(expense.copy(cost = newCost))
                    },
                    label = { Text("Cost") },
                    placeholder = { Text("0.00") },
                    prefix = { Text("$") },
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    modifier = Modifier.weight(1f),
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = frequencyT.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Occurrence") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        singleLine = true
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        Frequency.entries.forEach { payFre ->
                            DropdownMenuItem(
                                text = { Text(payFre.toString()) },
                                onClick = {
                                    expanded = false
                                    frequencyT = payFre
                                    onUpdate(expense.copy(frequency = payFre))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteExpenseDialog(
    expenseName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete expense") },
        text = { Text("Delete \"$expenseName\"? This can't be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, InternalVoyagerApi::class)
@Composable
@Preview
fun BudgetPlanner() {
    val coroutineScope = rememberCoroutineScope()
    val expenses by databaseInstance.expenses.collectAsState()
    val settings by databaseInstance.settings.collectAsState()
    val paidFrequency = settings?.paidFrequency ?: Frequency.Weekly

    var expanded by remember { mutableStateOf(false) }
    var calculated by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    LaunchedEffect(Unit) {
        databaseInstance.refresh()
    }

    expenseToDelete?.let { expense ->
        DeleteExpenseDialog(
            expenseName = expense.name,
            onConfirm = {
                coroutineScope.launch {
                    databaseInstance.deleteExpense(expense.id)
                }
                expenseToDelete = null
            },
            onDismiss = {
                expenseToDelete = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BackHandler(calculated, onBack = { calculated = false })

        Text(
            text = "Budget Planner",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (!calculated) {
            ExposedDropdownMenuBox(
                modifier = Modifier.fillMaxWidth(),
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = paidFrequency.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("How often are you paid?") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    Frequency.entries.forEach { payFre ->
                        DropdownMenuItem(
                            text = { Text(payFre.toString()) },
                            onClick = {
                                expanded = false
                                coroutineScope.launch {
                                    val currentSettings = settings ?: Settings()
                                    databaseInstance.saveSettings(currentSettings.copy(paidFrequency = payFre))
                                }
                            }
                        )
                    }
                }
            }

            Text(
                text = "Your Expenses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(
                    items = expenses,
                    key = { it.id }
                ) { expense ->
                    FormatExpense(
                        expense = expense,
                        onUpdate = { updatedExpense ->
                            coroutineScope.launch {
                                databaseInstance.upsertExpense(updatedExpense)
                            }
                        },
                        onDelete = {
                            expenseToDelete = expense
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            databaseInstance.upsertExpense(
                                Expense(
                                    name = "",
                                    cost = 0f,
                                    frequency = Frequency.Weekly
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Expense")
                }

                Button(
                    onClick = {
                        calculated = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Calculate Budget")
                }
            }
        } else {
            val timesBy = when (paidFrequency) {
                Frequency.Weekly -> 1f
                Frequency.Fortnightly -> 2f
                Frequency.Monthly -> 4.33f
                Frequency.Yearly -> 52f
            }
            val totalExpenses = expenses.sumOf { expense ->
                val expenseCost = expense.cost
                val rawCost = when (expense.frequency) {
                    Frequency.Weekly -> expenseCost * timesBy
                    Frequency.Fortnightly -> (expenseCost / 2f) * timesBy
                    Frequency.Monthly -> (expenseCost / 4.33f) * timesBy
                    Frequency.Yearly -> (expenseCost / 52f) * timesBy
                }
                (round(rawCost * 100f) / 100f).toDouble()
            }.toFloat()

            Text(
                text = "Budget Summary",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Total Projected Expenses ($paidFrequency)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$${formatCost(totalExpenses)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = "Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(expenses) { expense ->
                    val expenseCost = expense.cost
                    val rawCost = when (expense.frequency) {
                        Frequency.Weekly -> expenseCost * timesBy
                        Frequency.Fortnightly -> (expenseCost / 2f) * timesBy
                        Frequency.Monthly -> (expenseCost / 4.33f) * timesBy
                        Frequency.Yearly -> (expenseCost / 52f) * timesBy
                    }
                    val roundedCost = round(rawCost * 100f) / 100f

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text(
                                    text = expense.name.ifBlank { "Unnamed Expense" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${expense.frequency} ($${formatCost(expense.cost)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "$${formatCost(roundedCost)}",
                                modifier = Modifier.weight(0.8f),
                                textAlign = TextAlign.End,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                item {
                    Text(
                        text = "Disclaimer: some results may have small inaccuracies, always verify the results.",
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }

            OutlinedButton(
                onClick = { calculated = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Expenses")
            }
        }
    }
}
