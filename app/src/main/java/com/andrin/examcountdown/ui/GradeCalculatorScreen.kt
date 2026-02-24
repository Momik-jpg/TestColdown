package com.andrin.examcountdown.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.roundToInt

private data class GradeRow(
    val id: Int,
    val grade: String,
    val weight: String
)

@Composable
fun GradeCalculatorScreen(modifier: Modifier = Modifier) {
    val rows = remember {
        mutableStateListOf(
            GradeRow(id = 1, grade = "", weight = "1"),
            GradeRow(id = 2, grade = "", weight = "1")
        )
    }
    var nextId by remember { mutableIntStateOf(3) }
    var targetAverageText by remember { mutableStateOf("4.0") }
    var nextWeightText by remember { mutableStateOf("1") }
    var achievedPointsText by remember { mutableStateOf("") }
    var maxPointsText by remember { mutableStateOf("100") }
    var minGradeText by remember { mutableStateOf("1.0") }
    var maxGradeText by remember { mutableStateOf("6.0") }
    var targetGradeByPointsText by remember { mutableStateOf("4.0") }

    val parsedRows = rows.mapNotNull { row ->
        val grade = parseNumber(row.grade)
        val weight = parseNumber(row.weight)
        if (grade == null || weight == null || weight <= 0.0) null else grade to weight
    }

    val totalWeight = parsedRows.sumOf { it.second }
    val weightedSum = parsedRows.sumOf { it.first * it.second }
    val average = if (totalWeight > 0.0) weightedSum / totalWeight else null

    val targetAverage = parseNumber(targetAverageText)
    val nextWeight = parseNumber(nextWeightText)
    val requiredNextGrade = if (
        targetAverage != null &&
        nextWeight != null &&
        nextWeight > 0.0 &&
        totalWeight > 0.0
    ) {
        ((targetAverage * (totalWeight + nextWeight)) - weightedSum) / nextWeight
    } else {
        null
    }

    val achievedPoints = parseNumber(achievedPointsText)
    val maxPoints = parseNumber(maxPointsText)
    val minGrade = parseNumber(minGradeText)
    val maxGrade = parseNumber(maxGradeText)
    val targetGradeByPoints = parseNumber(targetGradeByPointsText)

    val validScale = minGrade != null && maxGrade != null && maxGrade > minGrade
    val validPointsRange = maxPoints != null && maxPoints > 0.0

    val gradeFromPoints = if (
        achievedPoints != null &&
        validPointsRange &&
        validScale
    ) {
        minGrade!! + (achievedPoints / maxPoints!!) * (maxGrade!! - minGrade)
    } else {
        null
    }

    val pointsPercent = if (achievedPoints != null && validPointsRange) {
        (achievedPoints / maxPoints!!) * 100.0
    } else {
        null
    }

    val neededPointsForTarget = if (
        targetGradeByPoints != null &&
        validScale &&
        validPointsRange
    ) {
        ((targetGradeByPoints - minGrade!!) / (maxGrade!! - minGrade)) * maxPoints!!
    } else {
        null
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Notenrechner",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Noten und Gewichte eintragen (z. B. Gewicht 2 für doppelte Wertung).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                rows.forEachIndexed { index, row ->
                    GradeRowEditor(
                        row = row,
                        canDelete = rows.size > 1,
                        onGradeChange = { newGrade -> rows[index] = row.copy(grade = newGrade) },
                        onWeightChange = { newWeight -> rows[index] = row.copy(weight = newWeight) },
                        onDelete = { rows.removeAll { it.id == row.id } }
                    )
                }

                OutlinedButton(
                    onClick = {
                        rows.add(GradeRow(id = nextId, grade = "", weight = "1"))
                        nextId += 1
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text(" Note hinzufügen", modifier = Modifier.padding(start = 6.dp))
                }

                ResultPill(average = average)
            }
        }

        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Zielnote-Rechner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = targetAverageText,
                    onValueChange = { targetAverageText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Gewünschter Schnitt") },
                    placeholder = { Text("z. B. 4.5") }
                )

                OutlinedTextField(
                    value = nextWeightText,
                    onValueChange = { nextWeightText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Gewicht nächste Note") },
                    placeholder = { Text("z. B. 1") }
                )

                val requiredText = requiredNextGrade?.let { formatNumber(it) } ?: "-"
                Text(
                    text = "Benötigte nächste Note: $requiredText",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                requiredNextGrade?.let { needed ->
                    val clamped = needed.coerceIn(1.0, 6.0)
                    if (needed != clamped) {
                        Text(
                            text = "Hinweis: Für das Ziel wäre ${formatNumber(needed)} nötig. Im Schweizer System ist der Bereich 1.0 bis 6.0.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Noten-Punkte-Rechner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = achievedPointsText,
                    onValueChange = { achievedPointsText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Erreichte Punkte") },
                    placeholder = { Text("z. B. 42") }
                )

                OutlinedTextField(
                    value = maxPointsText,
                    onValueChange = { maxPointsText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Maximale Punkte") },
                    placeholder = { Text("z. B. 60") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minGradeText,
                        onValueChange = { minGradeText = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Note min") },
                        placeholder = { Text("1.0") }
                    )

                    OutlinedTextField(
                        value = maxGradeText,
                        onValueChange = { maxGradeText = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Note max") },
                        placeholder = { Text("6.0") }
                    )
                }

                val gradeFromPointsText = gradeFromPoints?.let { formatNumber(it) } ?: "-"
                val pointsPercentText = pointsPercent?.let { "${formatNumber(it)} %" } ?: "-"

                Text(
                    text = "Aktuelle Note aus Punkten: $gradeFromPointsText",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Punkte in Prozent: $pointsPercentText",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = targetGradeByPointsText,
                    onValueChange = { targetGradeByPointsText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Zielnote") },
                    placeholder = { Text("z. B. 5.0") }
                )

                val neededPointsText = neededPointsForTarget?.let { formatNumber(it) } ?: "-"
                val maxPointsHint = maxPoints?.let { formatNumber(it) } ?: "-"
                Text(
                    text = "Benötigte Punkte für Zielnote: $neededPointsText / $maxPointsHint",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!validScale && (minGrade != null || maxGrade != null)) {
                    Text(
                        text = "Notenskala ungültig: 'Note max' muss größer als 'Note min' sein.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (achievedPoints != null && maxPoints != null && achievedPoints > maxPoints) {
                    Text(
                        text = "Hinweis: Erreichte Punkte sind größer als die maximalen Punkte.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun GradeRowEditor(
    row: GradeRow,
    canDelete: Boolean,
    onGradeChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = row.grade,
            onValueChange = onGradeChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text("Note") },
            placeholder = { Text("z. B. 5.25") }
        )

        OutlinedTextField(
            value = row.weight,
            onValueChange = onWeightChange,
            modifier = Modifier.weight(0.7f),
            singleLine = true,
            label = { Text("Gewicht") },
            placeholder = { Text("1") }
        )

        IconButton(onClick = onDelete, enabled = canDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = "Zeile löschen")
        }
    }
}

@Composable
private fun ResultPill(average: Double?) {
    val color = when {
        average == null -> MaterialTheme.colorScheme.onSurfaceVariant
        average >= 4.0 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }

    val text = when {
        average == null -> "Durchschnitt: -"
        else -> {
            val status = if (average >= 4.0) "Bestanden" else "Nicht bestanden"
            "Durchschnitt: ${formatNumber(average)} ($status)"
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = color
    )
}

private fun parseNumber(raw: String): Double? {
    return raw.trim()
        .replace(',', '.')
        .toDoubleOrNull()
}

private fun formatNumber(value: Double): String {
    val rounded = (value * 100.0).roundToInt() / 100.0
    return String.format(Locale.GERMANY, "%.2f", rounded)
}
