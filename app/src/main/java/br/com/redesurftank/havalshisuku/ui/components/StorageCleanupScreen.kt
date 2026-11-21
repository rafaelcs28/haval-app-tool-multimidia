package br.com.redesurftank.havalshisuku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.redesurftank.havalshisuku.managers.StorageCleaner
import br.com.redesurftank.havalshisuku.ui.theme.ImpulseTextSizes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val RowShape = RoundedCornerShape(10.dp)
private val Border = Color(0xFF262A33)
private val DateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

private fun sizeText(bytes: Long): String {
    if (bytes < 0) return "—"
    val mb = bytes / 1_000_000.0
    return if (mb >= 1000) String.format(Locale("pt", "BR"), "%.1f GB", mb / 1000)
    else String.format(Locale("pt", "BR"), "%.0f MB", mb)
}

/**
 * Recursos → Limpar armazenamento: large files on the head unit, biggest first, so the user picks
 * what to delete (old APKs, dashcam clips, debug logs). See [StorageCleaner] for the safe roots.
 */
@Composable
fun StorageCleanupScreen(onBackToFeatures: () -> Unit) {
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<StorageCleaner.Entry>>(emptyList()) }
    val selected = remember { mutableStateListOf<String>() }
    var scanning by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var free by remember { mutableStateOf(-1L) }
    var total by remember { mutableStateOf(-1L) }

    fun rescan() {
        if (scanning) return
        scanning = true
        scope.launch {
            val found = withContext(Dispatchers.IO) { StorageCleaner.scan() }
            entries = found
            selected.retainAll(found.map { it.path }.toSet())
            free = withContext(Dispatchers.IO) { StorageCleaner.freeBytes() }
            total = withContext(Dispatchers.IO) { StorageCleaner.totalBytes() }
            scanning = false
        }
    }

    LaunchedEffect(Unit) { rescan() }

    val chosen = entries.filter { it.path in selected }
    val chosenBytes = chosen.sumOf { it.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBackToFeatures) { Text("‹ Recursos", color = ImpTokens.Accent) }
            Spacer(Modifier.width(8.dp))
            Text(
                "Limpar armazenamento",
                color = ImpTokens.TextPrimary,
                fontSize = ImpulseTextSizes.Title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = { rescan() }, enabled = !scanning && !deleting) {
                Text(if (scanning) "Analisando…" else "Analisar de novo", color = ImpTokens.TextPrimary)
            }
        }

        if (total > 0 && free >= 0) {
            val used = (total - free).coerceAtLeast(0)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${sizeText(free)} livres de ${sizeText(total)} · abaixo de ~500 MB o Android recusa atualizações",
                    color = if (free < 700_000_000L) ImpTokens.Attention else ImpTokens.TextSecondary,
                    fontSize = ImpulseTextSizes.BodyCompact
                )
                LinearProgressIndicator(
                    progress = { (used.toFloat() / total).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (free < 700_000_000L) ImpTokens.Attention else ImpTokens.Accent,
                    trackColor = ImpTokens.TrackOff
                )
            }
        }

        Text(
            "Arquivos a partir de 5 MB na memória da central e na pasta temporária do adb, do maior para o menor. " +
                "Marque o que quiser apagar. Instaladores (APK) de versões já instaladas e logs antigos podem ser apagados com segurança.",
            color = ImpTokens.TextMuted,
            fontSize = ImpulseTextSizes.BodyCompact
        )

        message?.let {
            Text(
                it,
                color = ImpTokens.TextSecondary,
                fontSize = ImpulseTextSizes.BodyCompact,
                modifier = Modifier.fillMaxWidth().clip(RowShape).background(ImpTokens.Container).padding(12.dp)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (chosen.isEmpty()) "${entries.size} arquivos grandes" else "${chosen.size} selecionados · ${sizeText(chosenBytes)}",
                color = ImpTokens.TextPrimary,
                fontSize = ImpulseTextSizes.BodyCompact,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = {
                selected.clear()
                selected.addAll(entries.filter { it.category == "Instalador (APK)" }.map { it.path })
            }) { Text("Marcar todos os APKs", color = ImpTokens.Accent) }
            TextButton(onClick = { selected.clear() }) { Text("Limpar seleção", color = ImpTokens.TextSecondary) }
            Button(
                onClick = { confirm = true },
                enabled = chosen.isNotEmpty() && !deleting,
                colors = impButtonColors(ImpTokens.Danger)
            ) { Text(if (deleting) "Apagando…" else "Apagar selecionados") }
        }

        if (scanning && entries.isEmpty()) {
            Text("Analisando o armazenamento…", color = ImpTokens.TextSecondary, fontSize = ImpulseTextSizes.BodyCompact)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(entries, key = { it.path }) { entry ->
                val isSelected = entry.path in selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RowShape)
                        .background(if (isSelected) ImpTokens.IndexActiveTint else Color.Transparent)
                        .border(1.dp, if (isSelected) ImpTokens.Accent.copy(alpha = 0.5f) else Border, RowShape)
                        .clickable {
                            if (isSelected) selected.remove(entry.path) else selected.add(entry.path)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { if (it) selected.add(entry.path) else selected.remove(entry.path) },
                        colors = CheckboxDefaults.colors(checkedColor = ImpTokens.Accent)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(entry.name, color = ImpTokens.TextPrimary, fontSize = ImpulseTextSizes.BodyCompact, fontWeight = FontWeight.Medium)
                        Text(
                            "${entry.category} · ${DateFormat.format(Date(entry.modifiedMs))} · ${entry.folder}",
                            color = ImpTokens.TextMuted,
                            fontSize = ImpulseTextSizes.BodyCompact
                        )
                    }
                    Box(Modifier.padding(start = 12.dp)) {
                        Text(sizeText(entry.size), color = ImpTokens.TextPrimary, fontSize = ImpulseTextSizes.Label, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Apagar ${chosen.size} arquivo${if (chosen.size == 1) "" else "s"}?") },
            text = {
                Text(
                    "Libera ${sizeText(chosenBytes)}. Os arquivos são apagados de vez, não vão para uma lixeira. " +
                        "Vídeos salvos que ainda não foram enviados ao celular serão perdidos."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirm = false
                        deleting = true
                        val toDelete = chosen
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { StorageCleaner.delete(toDelete) }
                            deleting = false
                            selected.clear()
                            message = "Apagados ${result.deleted} arquivos, ${sizeText(result.freedBytes)} liberados." +
                                if (result.failed.isEmpty()) "" else
                                    " Não foi possível apagar: ${result.failed.take(5).joinToString(", ")}" +
                                        if (result.failed.size > 5) "…" else "."
                            rescan()
                        }
                    },
                    colors = impButtonColors(ImpTokens.Danger)
                ) { Text("Apagar") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } }
        )
    }
}
