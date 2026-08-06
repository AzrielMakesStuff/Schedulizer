package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class InputType {
    percent,
    int
}

class InputRow(val title : String, val toggleable : Boolean, vararg tInputTypes : InputType)  {
    private val results = mutableListOf<String>();
    private var checked = true
    val inputTypes = tInputTypes;
    @Composable
    fun BuildRow() {
        Row (Modifier.height(50.dp), verticalAlignment = Alignment.CenterVertically){
            Row(modifier = Modifier.fillMaxHeight().weight(2f), verticalAlignment = Alignment.CenterVertically) {



          if (!toggleable) {
              OutlinedCard(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.8f),colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant,
              ),) {

                  Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(), contentAlignment = Alignment.Center) {
                      Text(
                          text = title,
                          textAlign = TextAlign.Center
                      )
                  }
              }
          }
            else {
              Row(
                  modifier = Modifier.fillMaxHeight().weight(2f),
                  verticalAlignment = Alignment.CenterVertically
              ) {
                  var iChecked by remember { mutableStateOf(false) }
                  Checkbox(
                      checked = iChecked,
                      onCheckedChange = { iChecked = it; checked = iChecked }
                  )
                  Text(text = title,)

              }
          }

          }
           Row (modifier = Modifier.weight(1.5f), verticalAlignment = Alignment.CenterVertically){
               for (listIndex in 0..<inputTypes.size) {
                   var input by remember { mutableStateOf("") }
                   TextField(modifier = Modifier.weight(1f),value = input, onValueChange = { input = it; results[listIndex] = input })
               }
           }
        }
    }

    fun getResult() : List<String> {
        return if (checked) {
            results;
        } else mutableListOf()
    }
}

