package com.andrin.examcountdown.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

private data class GradeRow(
    val id: Int,
    val grade: String,
    val weight: String,
    val category: String
)

private val GradeRowsSaver = listSaver<SnapshotStateList<GradeRow>, String>(
    save = { rows -> rows.flatMap { listOf(it.id.toString(), it.grade, it.weight, it.category) } },
    restore = { saved -> saved.chunked(4).map { GradeRow(it[0].toInt(), it[1], it[2], it[3]) }.toMutableStateList() }
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GradeCalculatorScreen(modifier: Modifier = Modifier) {
    val isDark = isAppDarkTheme()
    val fieldColors = appTextFieldColors()
    var showCategories by rememberSaveable { mutableStateOf(false) }
    val rows = rememberSaveable(saver = GradeRowsSaver) {
        mutableStateListOf(
            GradeRow(id = 1, grade = "", weight = "1", category = "Prüfungen"),
            GradeRow(id = 2, grade = "", weight = "1", category = "Tests")
        )
    }
    var nextId by rememberSaveable { mutableIntStateOf(3) }
    var targetAverageText by rememberSaveable { mutableStateOf("4.0") }
    var nextWeightText by rememberSaveable { mutableStateOf("1") }
    var achievedPointsText by rememberSaveable { mutableStateOf("") }
    var maxPointsText by rememberSaveable { mutableStateOf("100") }
    var minGradeText by rememberSaveable { mutableStateOf("1.0") }
    var maxGradeText by rememberSaveable { mutableStateOf("6.0") }
    var targetGradeByPointsText by rememberSaveable { mutableStateOf("4.0") }

    val parsedRows = rows.mapNotNull { row ->
        val grade = parseSchoolGrade(row.grade)
        val weight = parsePositiveGradeNumber(row.weight)
        val category = row.category.trim().ifBlank { "Allgemein" }
        if (grade == null || weight == null || weight <= 0.0) {
            null
        } else {
            Triple(grade, weight, category)
        }
    }

    val invalidRows = rows.any { row ->
        row.grade.isNotBlank() && (parseSchoolGrade(row.grade) == null || parsePositiveGradeNumber(row.weight) == null)
    }
    val average = if (invalidRows) null else weightedGradeAverage(parsedRows.map { it.first to it.second })
    val categoryAverages = parsedRows
        .groupBy { it.third }
        .mapValues { (_, items) ->
            weightedGradeAverage(items.map { it.first to it.second })!!
        }
        .toSortedMap()

    val targetAverage = parseSchoolGrade(targetAverageText)
    val nextWeight = parsePositiveGradeNumber(nextWeightText)
    val requiredNextGrade = if (
        targetAverage != null &&
        nextWeight != null &&
        nextWeight > 0.0 &&
        average != null
    ) {
        // Divide before summing; no grade-times-weight product can overflow.
        (targetAverage + parsedRows.sumOf { (targetAverage - it.first) * (it.second / nextWeight) })
            .takeIf { it.isFinite() }
    } else {
        null
    }

    val achievedPoints = parseGradeNumber(achievedPointsText)
    val maxPoints = parseGradeNumber(maxPointsText)
    val minGrade = parseGradeNumber(minGradeText)
    val maxGrade = parseGradeNumber(maxGradeText)
    val targetGradeByPoints = parseGradeNumber(targetGradeByPointsText)

    val validScale = minGrade != null && maxGrade != null && maxGrade > minGrade && (maxGrade - minGrade).isFinite()
    val validPointsRange = maxPoints != null && maxPoints > 0.0
    val validAchievedPoints = achievedPoints != null && validPointsRange && achievedPoints in 0.0..maxPoints!!
    val validPointTarget = targetGradeByPoints != null && validScale && targetGradeByPoints in minGrade!!..maxGrade!!

    val gradeFromPoints = if (
        validAchievedPoints &&
        validPointsRange &&
        validScale
    ) {
        minGrade!! + (achievedPoints!! / maxPoints!!) * (maxGrade!! - minGrade)
    } else {
        null
    }

    val pointsPercent = if (validAchievedPoints) {
        (achievedPoints!! / maxPoints!!) * 100.0
    } else {
        null
    }

    val neededPointsForTarget = if (
        validPointTarget &&
        validScale &&
        validPointsRange
    ) {
        ((targetGradeByPoints!! - minGrade!!) / (maxGrade!! - minGrade)) * maxPoints!!
    } else {
        null
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StudyWorldHeader("Dein Lernfortschritt", "Schnitt, Zielnote & Punkte", illustrated = true, scene = StudyScene.GRADES)
        CalculatorCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Durchschnitt",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Schweizer Skala 1–6. Gewicht 2 zählt doppelt.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                rows.forEachIndexed { index, row ->
                    GradeRowEditor(
                        row = row,
                        canDelete = rows.size > 1,
                        showCategoryEditor = showCategories,
                        fieldColors = fieldColors,
                        onGradeChange = { newGrade -> rows[index] = row.copy(grade = newGrade) },
                        onWeightChange = { newWeight -> rows[index] = row.copy(weight = newWeight) },
                        onCategoryChange = { newCategory -> rows[index] = row.copy(category = newCategory) },
                        onDelete = { rows.removeAll { it.id == row.id } }
                    )
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = {
                            rows.add(GradeRow(id = nextId, grade = "", weight = "1", category = "Allgemein"))
                            nextId += 1
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isDark) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)
                            },
                            contentColor = if (isDark) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Text("Note hinzufügen", modifier = Modifier.padding(start = 6.dp), maxLines = 1)
                    }
                    TextButton(onClick = { showCategories = !showCategories }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(if (showCategories) "Fertig" else "Kategorien")
                    }
                }

                ResultPill(average = average, invalidRows = invalidRows)

                if (!invalidRows && categoryAverages.isNotEmpty()) {
                    Text(
                        text = "Schnitt je Kategorie",
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    categoryAverages.forEach { (category, value) ->
                        Surface(
                            color = if (isDark) {
                                MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "$category: ${formatNumber(value)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        CalculatorCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Zielnote",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                AdaptiveFieldPair(
                    first = { fieldModifier ->
                        AppTextField(
                            value = targetAverageText,
                            onValueChange = { targetAverageText = it },
                            modifier = fieldModifier,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            label = { Text("Zielschnitt") },
                            isError = targetAverageText.isNotBlank() && targetAverage == null,
                            errorMessage = "Gib einen Zielschnitt von 1 bis 6 ein.",
                            supportingText = if (targetAverageText.isNotBlank() && targetAverage == null) { { Text("Note von 1 bis 6") } } else null,
                            placeholder = { Text("z. B. 4.5") },
                            colors = fieldColors
                        )
                    },
                    second = { fieldModifier ->
                        AppTextField(
                            value = nextWeightText,
                            onValueChange = { nextWeightText = it },
                            modifier = fieldModifier,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            label = { Text("Gewicht") },
                            isError = nextWeightText.isNotBlank() && nextWeight == null,
                            errorMessage = "Das Gewicht muss eine Zahl größer als 0 sein.",
                            supportingText = if (nextWeightText.isNotBlank() && nextWeight == null) { { Text("Größer als 0") } } else null,
                            placeholder = { Text("z. B. 1") },
                            colors = fieldColors
                        )
                    }
                )

                CalculatorResult(
                    title = "Nächste Note",
                    value = requiredNextGrade?.let { formatNumber(it) } ?: "–",
                    detail = when {
                        invalidRows -> "Korrigiere zuerst die markierten Notenzeilen."
                        targetAverage == null || nextWeight == null -> "Gib einen gültigen Zielschnitt und ein Gewicht ein."
                        average == null -> "Trage zuerst mindestens eine Note ein."
                        requiredNextGrade == null -> "Diese Gewichtung ist zu gross für eine verlässliche Berechnung."
                        requiredNextGrade > 6.0 -> "Mit einer einzigen weiteren Note nicht erreichbar – auch eine 6 reicht nicht."
                        requiredNextGrade <= 1.0 -> "Ziel bereits abgesichert – selbst mit einer 1 in der nächsten Prüfung."
                        else -> "Diese Note brauchst du mindestens für deinen Zielschnitt."
                    },
                    warning = requiredNextGrade != null && requiredNextGrade > 6.0
                )
            }
        }

        CalculatorCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Punkte → Note",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                AdaptiveFieldPair(
                    first = { fieldModifier ->
                        AppTextField(
                            value = achievedPointsText,
                            onValueChange = { achievedPointsText = it },
                            modifier = fieldModifier,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            label = { Text("Erreicht") },
                            isError = achievedPointsText.isNotBlank() && !validAchievedPoints,
                            errorMessage = "Die erreichten Punkte müssen zwischen 0 und dem Maximum liegen.",
                            supportingText = if (achievedPointsText.isNotBlank() && !validAchievedPoints) { { Text("Zwischen 0 und Maximum") } } else null,
                            placeholder = { Text("z. B. 42") },
                            colors = fieldColors
                        )
                    },
                    second = { fieldModifier ->
                        AppTextField(
                            value = maxPointsText,
                            onValueChange = { maxPointsText = it },
                            modifier = fieldModifier,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            label = { Text("Maximum") },
                            isError = maxPointsText.isNotBlank() && !validPointsRange,
                            errorMessage = "Das Maximum muss eine Zahl größer als 0 sein.",
                            supportingText = if (maxPointsText.isNotBlank() && !validPointsRange) { { Text("Größer als 0") } } else null,
                            placeholder = { Text("z. B. 60") },
                            colors = fieldColors
                        )
                    }
                )

                AdaptiveFieldPair(
                    first = { fieldModifier ->
                        AppTextField(
                            value = minGradeText,
                            onValueChange = { minGradeText = it },
                            modifier = fieldModifier,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            label = { Text("Note min") },
                            isError = minGradeText.isNotBlank() && !validScale,
                            errorMessage = "Gib eine gültige Skala ein: Note max muss größer als Note min sein.",
                            placeholder = { Text("1.0") },
                            colors = fieldColors
                        )
                    },
                    second = { fieldModifier ->
                        AppTextField(
                            value = maxGradeText,
                            onValueChange = { maxGradeText = it },
                            modifier = fieldModifier,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            label = { Text("Note max") },
                            isError = maxGradeText.isNotBlank() && !validScale,
                            errorMessage = "Gib eine gültige Skala ein: Note max muss größer als Note min sein.",
                            placeholder = { Text("6.0") },
                            colors = fieldColors
                        )
                    }
                )

                CalculatorResult(
                    title = "Aktuelle Note aus Punkten",
                    value = gradeFromPoints?.let { formatNumber(it) } ?: "–",
                    detail = pointsPercent?.let { "${formatNumber(it)} % der maximalen Punkte · lineare Skala" }
                        ?: "Trage Punkte und eine gültige Notenskala ein."
                )

                AppTextField(
                    value = targetGradeByPointsText,
                    onValueChange = { targetGradeByPointsText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    label = { Text("Zielnote") },
                    isError = targetGradeByPointsText.isNotBlank() && !validPointTarget,
                    errorMessage = "Die Zielnote muss innerhalb der eingetragenen Notenskala liegen.",
                    supportingText = if (targetGradeByPointsText.isNotBlank() && !validPointTarget) { { Text("Innerhalb der Notenskala") } } else null,
                    placeholder = { Text("z. B. 5.0") },
                    colors = fieldColors
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

            }
        }
    }
}

@Composable
private fun CalculatorCard(
    content: @Composable () -> Unit
) {
    val isDark = isAppDarkTheme()
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) {
                MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            },
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.18f else 0.42f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
private fun GradeRowEditor(
    row: GradeRow,
    canDelete: Boolean,
    showCategoryEditor: Boolean,
    fieldColors: androidx.compose.material3.TextFieldColors,
    onGradeChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val badGrade = row.grade.isNotBlank() && parseSchoolGrade(row.grade) == null
    val badWeight = (row.weight.isNotBlank() || row.grade.isNotBlank()) && parsePositiveGradeNumber(row.weight) == null
    val gradeField: @Composable (Modifier) -> Unit = { fieldModifier ->
        AppTextField(
            value = row.grade, onValueChange = onGradeChange, modifier = fieldModifier,
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = badGrade, errorMessage = "Gib eine Note von 1 bis 6 ein.",
            supportingText = if (badGrade) { { Text("Note von 1 bis 6") } } else null,
            label = { Text("Note") }, placeholder = { Text("z. B. 5.25") }, colors = fieldColors
        )
    }
    val weightField: @Composable (Modifier) -> Unit = { fieldModifier ->
        AppTextField(
            value = row.weight, onValueChange = onWeightChange, modifier = fieldModifier,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal,
                imeAction = if (showCategoryEditor) ImeAction.Next else ImeAction.Done),
            isError = badWeight, errorMessage = "Das Gewicht muss eine Zahl größer als 0 sein.",
            supportingText = if (badWeight) { { Text("Größer als 0") } } else null,
            label = { Text("Gewicht") }, placeholder = { Text("1") }, colors = fieldColors
        )
    }
    val deleteButton: @Composable () -> Unit = {
        FilledTonalIconButton(onClick = onDelete, enabled = canDelete, modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )) {
            Icon(Icons.Outlined.Delete, contentDescription = "Notenzeile ${row.id} löschen")
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth / LocalDensity.current.fontScale < 300.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Note ${row.id}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                            deleteButton()
                        }
                        AdaptiveFieldPair(gradeField, weightField)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        gradeField(Modifier.weight(1f))
                        weightField(Modifier.weight(0.8f))
                        deleteButton()
                    }
                }
            }
            if (showCategoryEditor) AppTextField(
                value = row.category, onValueChange = onCategoryChange, modifier = Modifier.fillMaxWidth(),
                singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                label = { Text("Kategorie") }, placeholder = { Text("z. B. Prüfungen, Tests, Mitarbeit") }, colors = fieldColors
            )
        }
    }
}

@Composable
private fun ResultPill(average: Double?, invalidRows: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val passed = average != null && average >= 4.0
    val background = when {
        invalidRows -> scheme.errorContainer
        average == null -> scheme.surfaceVariant
        passed -> scheme.primaryContainer
        else -> scheme.errorContainer
    }
    val foreground = when {
        invalidRows -> scheme.onErrorContainer
        average == null -> scheme.onSurfaceVariant
        passed -> scheme.onPrimaryContainer
        else -> scheme.onErrorContainer
    }
    Surface(Modifier.fillMaxWidth(), color = background, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Dein Durchschnitt", style = MaterialTheme.typography.labelLarge, color = foreground)
            Text(average?.let { formatNumber(it) } ?: "–",
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = foreground)
            Text(when {
                invalidRows -> "Korrigiere die markierten Notenzeilen, damit alle Noten in den Schnitt einfliessen."
                average == null -> "Füge eine gültige Note und ein Gewicht hinzu."
                passed -> "Bestanden · ab Note 4.0"
                else -> "Nicht bestanden · unter Note 4.0"
            }, style = MaterialTheme.typography.bodySmall, color = foreground)
        }
    }
}

@Composable
private fun CalculatorResult(title: String, value: String, detail: String, warning: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val foreground = if (warning) scheme.onErrorContainer else scheme.onSecondaryContainer
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        color = if (warning) scheme.errorContainer else scheme.secondaryContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = foreground)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = foreground)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = foreground)
        }
    }
}

private fun formatNumber(value: Double): String = String.format(Locale.GERMANY, "%.2f", value)
