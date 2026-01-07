package com.by.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.by.android.data.model.ParameterDefinition
import com.by.android.data.model.ParameterType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildParametersDialog(
    jobName: String,
    parameters: List<ParameterDefinition>,
    onBuild: (Map<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    // Initialize parameter values with defaults
    val parameterValues = remember {
        mutableStateMapOf<String, String>().apply {
            parameters.forEach { param ->
                put(param.name, param.defaultValue)
            }
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Bar
                TopAppBar(
                    title = { Text("构建参数") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "关闭")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { onBuild(parameterValues.toMap()) }
                        ) {
                            Text("构建", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                )
                
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "需要填写以下参数来构建任务：$jobName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    parameters.forEach { param ->
                        ParameterInputField(
                            parameter = param,
                            value = parameterValues[param.name] ?: "",
                            onValueChange = { parameterValues[param.name] = it }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParameterInputField(
    parameter: ParameterDefinition,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = parameter.name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        when (parameter.parameterType) {
            ParameterType.CHOICE -> {
                ChoiceParameterInput(
                    choices = parameter.choices ?: emptyList(),
                    value = value,
                    onValueChange = onValueChange
                )
            }
            ParameterType.BOOLEAN -> {
                BooleanParameterInput(
                    value = value,
                    onValueChange = onValueChange
                )
            }
            ParameterType.TEXT -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    placeholder = { Text("输入文本") }
                )
            }
            ParameterType.PASSWORD -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    placeholder = { Text("输入密码") },
                    singleLine = true
                )
            }
            ParameterType.STRING -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入值") },
                    singleLine = true
                )
            }
        }
        
        if (!parameter.description.isNullOrEmpty()) {
            Text(
                text = parameter.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceParameterInput(
    choices: List<String>,
    value: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice) },
                    onClick = {
                        onValueChange(choice)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
private fun BooleanParameterInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = value.lowercase() == "true",
            onCheckedChange = { checked ->
                onValueChange(if (checked) "true" else "false")
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (value.lowercase() == "true") "是" else "否",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

