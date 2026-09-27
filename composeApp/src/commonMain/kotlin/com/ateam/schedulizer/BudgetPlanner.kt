package com.ateam.schedulizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.round
// thing
class ExpenseBudget(val onDelete : () -> Unit) {
    var name = "Expense Name"
    var frequency = Frequency.Weekly
    var cost = "0"
    var id : Long? = null;
    var delete = false


    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun Build() {
        var nameT by remember { mutableStateOf(name) }
        var costT by remember { mutableStateOf(cost) }
        var expanded by remember { mutableStateOf(false) }
        var deleteT by remember { mutableStateOf(false)}

        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(12.dp)
        )
        {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1.1f),
                    value = nameT,
                    onValueChange = { nameT = it; name = nameT },
                    label = { Text("Name") })
                Spacer(modifier = Modifier.width(8.dp))
                ExposedDropdownMenuBox(
                    modifier = Modifier.weight(1f),
                    expanded = expanded,
                    onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = frequency.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Occurance") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )


                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }) {

                        Frequency.entries.forEach { payFre ->
                            DropdownMenuItem(
                                text = { Text(payFre.toString()) },
                                onClick = { expanded = false; frequency = payFre }
                            )
                        }

                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(0.6f),
                    value = "$$costT",
                    onValueChange = { newVal ->
                        costT = newVal.filter { it.isDigit() }; cost = costT
                    },
                    label = { Text("Cost") })
            }
            Row( modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(onClick = {
                    delete = true
                   deleteT = true
                    if (id != null) {
                        id?.let { removeExpense(it
                        )
                            onDelete()
                    }
                    }
                    else onDelete()


                }) {
                    Icon(Icons.Filled.Delete,"Delete")

                }
            }

        }
    }
}

enum class Frequency {
    Weekly, Fortnightly,Monthly, Yearly
}

@OptIn(ExperimentalMaterial3Api::class, InternalVoyagerApi::class)
@Composable
@Preview
fun BudgetPlanner() {
    var paidFrequency by remember { mutableStateOf(Frequency.Weekly) }
    var expanded by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(true) }
    val expenses = remember { mutableStateListOf(ExpenseBudget(onDelete = {refresh = true})) }
    var calculated by remember { mutableStateOf(false) }
    var cleared =false
    LaunchedEffect(Unit) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val expensesT = databaseInstance.getAllExpenses()
            expensesT.forEach {
                if (!cleared) {
                    expenses.clear()
                    cleared = true;
                }
                val expenseBudget = ExpenseBudget(onDelete = { refresh = true});
                expenseBudget.id = it.id
                expenseBudget.name = it.name
                expenseBudget.cost = it.cost.toString()
                expenseBudget.frequency = it.frequency
                expenses.add(expenseBudget)
            }
        }
    }
    Column (modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        var amountEarned by remember { mutableStateOf("0") }
        BackHandler(calculated, onBack = {calculated = false})

        if (!calculated) {

         ExposedDropdownMenuBox(modifier = Modifier.fillMaxWidth(0.5f).padding(bottom = 20.dp) ,expanded = expanded, onExpandedChange = { expanded = it }) {
             OutlinedTextField(
                 value = paidFrequency.toString(),
                 onValueChange = {},
                 readOnly = true,
                 label = { Text("How often are you paid?") },
                 trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                 modifier = Modifier
                     .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
             )
             ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {

                 Frequency.entries.forEach { payFre ->
                     DropdownMenuItem(
                         text = { Text(payFre.toString()) },
                         onClick = {  expanded = false; paidFrequency = payFre }
                     )
                 }

             }
         }
         Text("Expenses:")

         LazyColumn (modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally){
             items(items = expenses, key = { it.hashCode() }) { expense ->
                 expense.Build()
             }
             item {
                 OutlinedButton(onClick = {
                     expenses.add(ExpenseBudget(onDelete = {refresh = true}))
                 }) {
                     Text("Add Expense")
                 }
             }

             item {
                 OutlinedButton(onClick = {
                     calculated = true
                 }) {
                     Text("Calculate")
                 }
             }
         }
            if (refresh) {
                expenses.forEachIndexed { index, budget ->
                    if (budget.delete) expenses.removeAt(index)
                }
                refresh = false
            }
     }
      else {
          println(expenses.size)
            var totalExpenses = 0f
            saveExpenses(expenses)
            LazyColumn (
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ){
              item {
                  Text( text = "Disclaimer: some results may have small inaccuracies, always verify the results",
                      textAlign = TextAlign.Center,
                      fontSize = 11.sp,
                      modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                  )

              }
              items(expenses.size) { index ->
                  val expense = expenses[index]
                  val totalEarned = amountEarned.toFloat()
                  var cost = "0";
                  val timesBy = when (paidFrequency) {
                      Frequency.Weekly -> 1f
                      Frequency.Fortnightly -> 2f
                      Frequency.Monthly -> 4.33f
                      Frequency.Yearly -> 52f
                  }
                  cost = when (expense.frequency) {

                      Frequency.Weekly -> (expense.cost.toFloat() * timesBy).toString()
                      Frequency.Fortnightly -> (expense.cost.toFloat() / 2f * timesBy).toString()
                      Frequency.Monthly -> (expense.cost.toFloat() / 4.33f * timesBy).toString()
                      Frequency.Yearly -> (expense.cost.toFloat() / 52f * timesBy).toString()
                  }
                  cost = ((round((cost.toFloat() * 100f))/ 100f).toString())
                  totalExpenses += cost.toFloat()
                  Card(
                      modifier = Modifier
                          .fillMaxWidth()
                          .padding(horizontal = 20.dp),
                      colors = CardDefaults.cardColors(
                          containerColor = MaterialTheme.colorScheme.surfaceVariant
                      ),
                      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                      shape = RoundedCornerShape(12.dp)
                  ) {
                      Row(
                          modifier = Modifier
                              .fillMaxWidth()
                              .padding(horizontal = 16.dp, vertical = 14.dp),
                          verticalAlignment = Alignment.CenterVertically
                      ) {
                          Text(
                              text = expense.name,
                              modifier = Modifier.weight(1.2f),
                              textAlign = TextAlign.Start
                          )
                          Text(
                              text = "$$cost",
                              modifier = Modifier.weight(0.8f),
                              textAlign = TextAlign.End,
                              fontWeight = FontWeight.Medium
                          )
                      }
                  }
              }
                item {
                    Text(
                        "Total Cost: $$totalExpenses",
                        modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                    )
                }
          }
      }
    }
}

fun saveExpenses(expenses : List<ExpenseBudget>) {

    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {

     expenses.forEach {
         if (it.id != null) {
             val expense = Expense(id = it.id!!,name = it.name,it.cost.toInt(), frequency = it.frequency)
             databaseInstance.upsertExpense(expense)
         }

       else {
             val expense = Expense(cost = it.cost.toInt(), frequency = it.frequency,name = it.name)
             databaseInstance.upsertExpense(expense)
         }
     }
    }
}

fun removeExpense(id : Long) {
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        databaseInstance.deleteExpense(id)
    }
}